package com.suman.memoryarchitect.core.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logCollectionPurchased
import com.suman.memoryarchitect.core.analytics.logPurchaseCancelled
import com.suman.memoryarchitect.core.analytics.logPurchaseFailed
import com.suman.memoryarchitect.core.analytics.logPurchasePending
import com.suman.memoryarchitect.core.analytics.logPurchaseRestored
import com.suman.memoryarchitect.core.analytics.logPurchaseStarted
import com.suman.memoryarchitect.core.analytics.logPurchaseSuccess
import com.suman.memoryarchitect.core.analytics.logRemoveAdsPurchased
import com.suman.memoryarchitect.core.analytics.logRestoreCompleted
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.model.BillingCatalogProduct
import com.suman.memoryarchitect.domain.model.BillingEntitlementKind
import com.suman.memoryarchitect.domain.model.BillingProductType
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * See [BillingManager] for the contract - this is the one and only implementation, covering
 * `remove_ads_lifetime` and every `domain.progression.PremiumShopCatalog` product through one
 * `SharedBillingClient` subscription, one purchase/grant pipeline. Replaces the two managers
 * (`BillingManagerImpl`/`PremiumShopManagerImpl`) this app used to have.
 *
 * Every grant branches twice, on two orthogonal fields of the purchased [BillingCatalogProduct]:
 * [BillingProductType] decides the Play Billing *mechanics* (acknowledge vs consume, `INAPP` vs
 * `SUBS` queries); [BillingEntitlementKind] decides what actually gets granted ([grantEntitlement]).
 * Neither axis needs the other touched to extend - a future coin pack is one catalog entry plus one
 * new `when` arm in each, never a rewrite of this class.
 *
 * No Cloud Function is called anywhere in this class - see [PurchaseSignatureVerifier] for the
 * Spark-plan-compatible local verification that replaces the old `verifyRemoveAdsPurchase`/
 * `verifyPremiumPurchase` server round-trip, and [CosmeticCollectionGrantor] for the direct client
 * Firestore write (guarded by a `firestore.rules`-enforced create-only replay token) that replaces
 * what a Cloud Function used to do transactionally - shared with `core.debug.DebugTestGrantor`'s own
 * debug-only grant path, so that logic exists in exactly one place.
 */
@Singleton
class BillingManagerImpl @Inject constructor(
    private val sharedBillingClient: SharedBillingClient,
    private val playerIdentityManager: PlayerIdentityManager,
    private val preferences: UserPreferencesDataStore,
    private val cosmeticCollectionGrantor: CosmeticCollectionGrantor,
    private val dispatchers: DispatcherProvider,
    private val analytics: AnalyticsLogger,
) : BillingManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var startConnectionCalled = false
    private var inFlightProductId: String? = null

    private val _hasRemovedAds = MutableStateFlow(false)
    override val hasRemovedAds: StateFlow<Boolean> = _hasRemovedAds.asStateFlow()

    /** Every non-Remove-Ads product this account currently owns, per the last successful Play
     * Billing sync - the actual source [productStates]' `owned` field reads for a
     * [BillingEntitlementKind.COSMETIC_COLLECTION] product, mirrored to [UserPreferencesDataStore.ownedProductIds]
     * purely as a fast local cache, never trusted ahead of a fresh sync. */
    private val _ownedProductIds = MutableStateFlow<Set<String>>(emptySet())

    private val _productStates = MutableStateFlow<Map<String, BillingProductUiState>>(emptyMap())
    override val productStates: StateFlow<Map<String, BillingProductUiState>> = _productStates.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    override val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    private val _productDetailsLoadFailed = MutableStateFlow(false)
    override val productDetailsLoadFailed: StateFlow<Boolean> = _productDetailsLoadFailed.asStateFlow()

    /** The full SDK objects Play returned, keyed by product id - needed to launch a purchase and to
     * read a resolved offer's raw price for revenue analytics. [productStates] is only the small,
     * UI-facing subset. */
    private var resolvedProductDetails: Map<String, ProductDetails> = emptyMap()

    private val billingClient: BillingClient get() = sharedBillingClient.billingClient
    private val catalog: List<BillingCatalogProduct> get() = PremiumShopCatalog.products

    /** The `BillingClient.ProductType` string [BillingProductType] maps to - `SUBS` for
     * [BillingProductType.SUBSCRIPTION], `INAPP` for everything else (Play Billing itself doesn't
     * distinguish consumable from non-consumable at the query/purchase level - that split only
     * matters once a purchase resolves, see [applyPurchases]'s consume-vs-acknowledge branch).
     * Lives here rather than on the enum itself so `domain.model` stays free of Android/SDK
     * imports, matching every other file in that package. */
    private fun BillingProductType.toPlayBillingProductType(): String = when (this) {
        BillingProductType.SUBSCRIPTION -> BillingClient.ProductType.SUBS
        BillingProductType.NON_CONSUMABLE, BillingProductType.CONSUMABLE -> BillingClient.ProductType.INAPP
    }

    override fun startConnection() {
        if (startConnectionCalled) return
        startConnectionCalled = true
        scope.launch {
            // Instant local seed so a caller reading these before the network round-trip below
            // completes (e.g. a purchase screen's very first frame) never shows "Buy Now" to
            // someone who already owns this - see UserPreferencesDataStore's own doc.
            _hasRemovedAds.value = preferences.hasRemovedAds.first()
            _ownedProductIds.value = preferences.ownedProductIds.first()
            refreshProductStates()
        }
        sharedBillingClient.startConnection()
        scope.launch {
            sharedBillingClient.isReady.filter { it }.collect {
                loadProductDetails()
                queryAndApplyExistingPurchases(reportSuccess = false)
            }
        }
        scope.launch {
            sharedBillingClient.purchasesUpdated.collect { update ->
                handlePurchasesUpdated(update.billingResult, update.purchases)
            }
        }
    }

    override fun retryLoadProductDetails() {
        scope.launch { loadProductDetails() }
    }

    /** One query covers every product regardless of [BillingProductType] - `QueryProductDetailsParams.Product`
     * carries its own product type per entry, unlike [queryAndApplyExistingPurchases], which needs a
     * separate call per type. Only a genuine query failure (thrown exception, or Play never
     * returning a result at all) sets [_productDetailsLoadFailed] - a *partial* catalog (some
     * products resolve, others don't, e.g. one isn't published in this Play Console track yet) is a
     * configuration gap retrying can't fix, so it's surfaced per-card instead (that product simply
     * stays absent from [productStates]) rather than blocking every other product behind a retry
     * button that wouldn't help them either. */
    private suspend fun loadProductDetails() {
        val products = catalog.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.billingProductId)
                .setProductType(product.type.toPlayBillingProductType())
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        val result = runCatching { billingClient.queryProductDetails(params) }
            .onFailure { Log.w(TAG, "queryProductDetails failed", it) }
            .getOrNull()
        if (result == null) {
            _productDetailsLoadFailed.value = true
            return
        }
        resolvedProductDetails = result.productDetailsList.orEmpty().associateBy { it.productId }
        refreshProductStates()
        _productDetailsLoadFailed.value = false
    }

    private fun refreshProductStates() {
        _productStates.value = catalog.associate { product ->
            val price = resolvedProductDetails[product.billingProductId]?.let(::formattedPriceOf)
            val owned = when (product.entitlement) {
                BillingEntitlementKind.REMOVE_ADS -> _hasRemovedAds.value
                BillingEntitlementKind.COSMETIC_COLLECTION,
                BillingEntitlementKind.SUBSCRIPTION_TIER,
                -> product.billingProductId in _ownedProductIds.value
                // A consumable is always re-purchasable by definition (it gets consumeAsync'd right
                // after granting, see applyPurchases) - it must never show as permanently "owned"
                // after the first purchase, which would incorrectly hide the Buy button forever.
                BillingEntitlementKind.CONSUMABLE_GRANT -> false
            }
            product.billingProductId to BillingProductUiState(formattedPrice = price, owned = owned)
        }
    }

    private fun formattedPriceOf(details: ProductDetails): String? =
        details.oneTimePurchaseOfferDetails?.formattedPrice
            ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

    /** Play Billing scopes a purchases query to exactly one product type per call (unlike product
     * *details* queries above) - two calls, `INAPP` then `SUBS`, cover the whole catalog regardless
     * of how many subscription products exist (zero today; this still runs so that path is
     * exercised-ready, not dead code waiting for the first subscription SKU). */
    private suspend fun queryAndApplyExistingPurchases(reportSuccess: Boolean) {
        val inapp = runCatching { billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) }
            .onFailure { Log.w(TAG, "queryPurchasesAsync(INAPP) failed", it) }
            .getOrNull()
        val subs = runCatching { billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) }
            .onFailure { Log.w(TAG, "queryPurchasesAsync(SUBS) failed", it) }
            .getOrNull()
        val purchases = (inapp?.purchasesList.orEmpty() + subs?.purchasesList.orEmpty())
            .filter { purchase -> purchase.products.any { PremiumShopCatalog.productOrNull(it) != null } }

        if (purchases.isNotEmpty()) {
            applyPurchases(purchases, reportSuccess = reportSuccess)
        } else if (reportSuccess) {
            _purchaseState.value = PurchaseUiState.RestoreNotFound
        }
        if (reportSuccess) analytics.logRestoreCompleted(purchases.size)
    }

    override fun launchPurchase(activity: Activity, productId: String) {
        analytics.logPurchaseStarted(productId)
        val product = PremiumShopCatalog.productOrNull(productId)
        if (product == null) {
            _purchaseState.value = PurchaseUiState.Failed(productId, BillingFailureReason.NOT_READY)
            analytics.logPurchaseFailed(productId, "unknown_product")
            return
        }
        val details = resolvedProductDetails[productId]
        if (details == null) {
            _purchaseState.value = PurchaseUiState.Failed(productId, BillingFailureReason.NOT_READY)
            analytics.logPurchaseFailed(productId, "not_ready")
            return
        }
        scope.launch {
            val uid = playerIdentityManager.awaitUid()
            if (uid == null) {
                _purchaseState.value = PurchaseUiState.Failed(productId, BillingFailureReason.NOT_READY)
                analytics.logPurchaseFailed(productId, "not_signed_in")
                return@launch
            }
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
            if (product.type == BillingProductType.SUBSCRIPTION) {
                // Seam only - no subscription SKU is enabled today, so this offer token never
                // resolves to null in practice yet, but the resolution path already exists.
                details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let(productDetailsParams::setOfferToken)
            }
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams.build()))
                // Binds this purchase to the signed-in uid from the moment it's created -
                // CosmeticCollectionGrantor's Firestore transaction only ever writes under this same
                // uid, so a purchase token stolen from another account has nowhere valid to grant
                // into.
                .setObfuscatedAccountId(sha256(uid))
                .build()
            inFlightProductId = productId
            _purchaseState.value = PurchaseUiState.Loading(productId)
            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                inFlightProductId = null
                _purchaseState.value = PurchaseUiState.Failed(productId, launchResult.responseCode.toFailureReason())
                analytics.logPurchaseFailed(productId, launchResult.responseCode.toString())
            }
        }
    }

    override fun restorePurchases() {
        scope.launch { queryAndApplyExistingPurchases(reportSuccess = true) }
    }

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>) {
        val productId = inFlightProductId
        inFlightProductId = null
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val relevant = purchases.filter { purchase -> purchase.products.any { PremiumShopCatalog.productOrNull(it) != null } }
                if (relevant.isEmpty()) {
                    _purchaseState.value = PurchaseUiState.Idle
                    return
                }
                scope.launch { applyPurchases(relevant, reportSuccess = true) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Shouldn't normally be reachable (launchPurchase's own catalog already reflects
                // ownership), but Play is the actual source of truth, not this app's cached flag -
                // if Play says already-owned, re-sync from Play rather than trust local state.
                if (productId != null) _purchaseState.value = PurchaseUiState.AlreadyOwned(productId)
                scope.launch { queryAndApplyExistingPurchases(reportSuccess = false) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                if (productId != null) analytics.logPurchaseCancelled(productId)
                _purchaseState.value = PurchaseUiState.Idle
            }
            else -> {
                if (productId != null) {
                    _purchaseState.value = PurchaseUiState.Failed(productId, billingResult.responseCode.toFailureReason())
                    analytics.logPurchaseFailed(productId, billingResult.responseCode.toString())
                }
            }
        }
    }

    /** Shared by the initial restore query, [handlePurchasesUpdated]'s OK branch, and
     * [restorePurchases] - the one place a raw [Purchase] list becomes an actual grant, so that only
     * ever happens once regardless of which of those three triggered it. Per Google Play Billing
     * policy, a non-consumable purchase left unacknowledged for 3 days is automatically refunded and
     * revoked - every code path that can observe a `PURCHASED` [Purchase] funnels through here
     * specifically so that can never happen. */
    private suspend fun applyPurchases(purchases: List<Purchase>, reportSuccess: Boolean) {
        for (purchase in purchases) {
            val productId = purchase.products.firstOrNull { PremiumShopCatalog.productOrNull(it) != null } ?: continue
            val product = PremiumShopCatalog.requireProduct(productId)

            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                _purchaseState.value = PurchaseUiState.Pending(productId)
                if (reportSuccess) analytics.logPurchasePending(productId)
                continue
            }
            if (!isGrantedEntitlement(purchase.products, purchase.purchaseState, productId)) continue

            when (PurchaseSignatureVerifier.verify(purchase)) {
                PurchaseSignatureVerifier.Result.Invalid -> {
                    Log.w(TAG, "Purchase signature verification failed for $productId - not granting")
                    if (reportSuccess) {
                        _purchaseState.value = PurchaseUiState.Failed(productId, BillingFailureReason.GENERIC)
                        analytics.logPurchaseFailed(productId, "signature_invalid")
                    }
                    continue
                }
                PurchaseSignatureVerifier.Result.NotConfigured -> {
                    if (!BuildConfig.DEBUG) {
                        Log.w(TAG, "Purchase signature key not configured - refusing to grant $productId in a release build")
                        if (reportSuccess) _purchaseState.value = PurchaseUiState.Failed(productId, BillingFailureReason.GENERIC)
                        continue
                    }
                    // Debug builds proceed unverified - see PurchaseSignatureVerifier's own doc.
                }
                PurchaseSignatureVerifier.Result.Valid -> Unit
            }

            val grantedCosmeticIds = grantEntitlement(product, purchase)

            when (product.type) {
                BillingProductType.CONSUMABLE -> {
                    val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    runCatching { billingClient.consumePurchase(consumeParams) }
                        .onFailure { Log.w(TAG, "Failed to consume $productId purchase", it) }
                }
                BillingProductType.NON_CONSUMABLE, BillingProductType.SUBSCRIPTION -> {
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        runCatching { billingClient.acknowledgePurchase(ackParams) }
                            .onFailure { Log.w(TAG, "Failed to acknowledge $productId purchase", it) }
                    }
                }
            }

            // A consumable is deliberately never added here - see refreshProductStates' own doc for
            // why "owned" must never latch permanently true for something meant to be re-buyable.
            if (product.type != BillingProductType.CONSUMABLE) {
                _ownedProductIds.value = _ownedProductIds.value + productId
                preferences.setOwnedProductIds(_ownedProductIds.value)
            }
            refreshProductStates()

            if (reportSuccess) {
                _purchaseState.value = PurchaseUiState.Success(productId, grantedCosmeticIds)
                analytics.logPurchaseSuccess(productId)
                val offer = resolvedProductDetails[productId]?.oneTimePurchaseOfferDetails
                when (product.entitlement) {
                    BillingEntitlementKind.REMOVE_ADS -> analytics.logRemoveAdsPurchased(offer?.priceAmountMicros ?: 0L, offer?.priceCurrencyCode ?: "")
                    BillingEntitlementKind.COSMETIC_COLLECTION -> analytics.logCollectionPurchased(productId, offer?.priceAmountMicros ?: 0L, offer?.priceCurrencyCode ?: "")
                    BillingEntitlementKind.CONSUMABLE_GRANT, BillingEntitlementKind.SUBSCRIPTION_TIER -> Unit
                }
            } else {
                analytics.logPurchaseRestored(productId)
            }
        }
    }

    /** Branches on [BillingCatalogProduct.entitlement] - the "what does this grant" axis, orthogonal
     * to [BillingProductType]'s "how do I pay for this" axis the caller already branched on.
     * Returns the cosmetic ids granted (empty for every entitlement kind except
     * [BillingEntitlementKind.COSMETIC_COLLECTION]). */
    private suspend fun grantEntitlement(product: BillingCatalogProduct, purchase: Purchase): List<CosmeticId> = when (product.entitlement) {
        BillingEntitlementKind.REMOVE_ADS -> {
            _hasRemovedAds.value = true
            preferences.setHasRemovedAds(true)
            emptyList()
        }
        BillingEntitlementKind.COSMETIC_COLLECTION -> {
            val uid = playerIdentityManager.awaitUid()
            cosmeticCollectionGrantor.grant(uid, product, replayGuardToken = purchase.purchaseToken)
            product.grantedCosmeticIds
        }
        BillingEntitlementKind.CONSUMABLE_GRANT, BillingEntitlementKind.SUBSCRIPTION_TIER -> {
            // Not wired to a real grant yet - no consumable/subscription SKU exists in the catalog
            // today. A future one should credit a quantity (coins, a token count) or set an active-
            // tier flag here, following whichever pattern (Room + Firestore mirror)
            // [CosmeticCollectionGrantor] already establishes for cosmetic collections.
            Log.w(TAG, "No grant wired for ${product.entitlement} yet (${product.billingProductId})")
            emptyList()
        }
    }

    /** Never logs [value] itself (a purchase token/signature) - only ever its hash, per this app's
     * "never expose purchase information in logs" requirement. */
    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun Int.toFailureReason(): BillingFailureReason = when (this) {
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingFailureReason.ITEM_UNAVAILABLE
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        -> BillingFailureReason.BILLING_UNAVAILABLE
        else -> BillingFailureReason.GENERIC
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
