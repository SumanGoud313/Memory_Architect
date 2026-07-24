package com.suman.memoryarchitect.core.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticEntity
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumShopManagerImpl @Inject constructor(
    private val sharedBillingClient: SharedBillingClient,
    private val playerIdentityManager: PlayerIdentityManager,
    private val ownedCosmeticDao: OwnedCosmeticDao,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) : PremiumShopManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var startConnectionCalled = false
    private var inFlightProductId: String? = null

    private val _productPrices = MutableStateFlow<Map<String, PremiumProductPrice>>(emptyMap())
    override val productPrices: StateFlow<Map<String, PremiumProductPrice>> = _productPrices.asStateFlow()

    private val _purchaseState = MutableStateFlow<PremiumPurchaseUiState>(PremiumPurchaseUiState.Idle)
    override val purchaseState: StateFlow<PremiumPurchaseUiState> = _purchaseState.asStateFlow()

    /** The full SDK objects Play returned, keyed by product id - needed to launch a purchase.
     * [productPrices] is only the small subset the UI displays, same split [BillingManagerImpl]
     * already uses for `remove_ads_lifetime`. */
    private var resolvedProductDetails: Map<String, ProductDetails> = emptyMap()

    private val billingClient: BillingClient get() = sharedBillingClient.billingClient
    private val productIds: List<String> get() = PremiumShopCatalog.cosmeticBundleProductIds

    override fun startConnection() {
        if (startConnectionCalled) return
        startConnectionCalled = true
        sharedBillingClient.startConnection()
        scope.launch {
            sharedBillingClient.isReady.filter { it }.collect {
                loadProductDetails()
                queryAndApplyExistingPurchases()
            }
        }
        scope.launch {
            sharedBillingClient.purchasesUpdated.collect { update ->
                val relevant = update.purchases.filter { purchase -> purchase.products.any { it in productIds } }
                when {
                    relevant.isNotEmpty() -> handlePurchasesUpdated(update.billingResult, relevant)
                    inFlightProductId != null -> handlePurchasesUpdated(update.billingResult, emptyList())
                    else -> Unit // Not ours - BillingManager's remove_ads_lifetime flow, most likely.
                }
            }
        }
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productIds.map { id -> QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.INAPP).build() })
            .build()
        val result = runCatching { billingClient.queryProductDetails(params) }
            .onFailure { Log.w(TAG, "queryProductDetails failed", it) }
            .getOrNull() ?: return
        val detailsList = result.productDetailsList.orEmpty()
        resolvedProductDetails = detailsList.associateBy { it.productId }
        _productPrices.value = detailsList.mapNotNull { details ->
            val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: return@mapNotNull null
            details.productId to PremiumProductPrice(price)
        }.toMap()
    }

    private suspend fun queryAndApplyExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val result = runCatching { billingClient.queryPurchasesAsync(params) }
            .onFailure { Log.w(TAG, "queryPurchasesAsync failed", it) }
            .getOrNull() ?: return
        val relevant = result.purchasesList.filter { purchase -> purchase.products.any { it in productIds } }
        if (relevant.isNotEmpty()) applyPurchases(relevant, reportSuccess = false)
    }

    override fun launchPurchase(activity: Activity, productId: String) {
        val details = resolvedProductDetails[productId]
        if (details == null) {
            _purchaseState.value = PremiumPurchaseUiState.Failed(productId, BillingFailureReason.NOT_READY)
            return
        }
        scope.launch {
            val uid = playerIdentityManager.awaitUid()
            if (uid == null) {
                _purchaseState.value = PremiumPurchaseUiState.Failed(productId, BillingFailureReason.NOT_READY)
                return@launch
            }
            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                // Binds this purchase to the signed-in uid from the moment it's created -
                // verifyPremiumPurchase compares this same hash server-side, so a purchase token
                // stolen from another account is rejected on its very first use, not only replay.
                .setObfuscatedAccountId(sha256(uid))
                .build()
            inFlightProductId = productId
            _purchaseState.value = PremiumPurchaseUiState.Loading(productId)
            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                inFlightProductId = null
                _purchaseState.value = PremiumPurchaseUiState.Failed(productId, launchResult.responseCode.toFailureReason())
            }
        }
    }

    override fun restorePurchases() {
        scope.launch {
            queryAndApplyExistingPurchases()
            if (_purchaseState.value is PremiumPurchaseUiState.Loading) {
                _purchaseState.value = PremiumPurchaseUiState.RestoreNotFound
            }
        }
    }

    private fun handlePurchasesUpdated(billingResult: com.android.billingclient.api.BillingResult, purchases: List<Purchase>?) {
        val productId = inFlightProductId
        inFlightProductId = null
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _purchaseState.value = PremiumPurchaseUiState.Idle
                    return
                }
                scope.launch { applyPurchases(purchases, reportSuccess = true) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                if (productId != null) _purchaseState.value = PremiumPurchaseUiState.AlreadyOwned(productId)
                scope.launch { queryAndApplyExistingPurchases() }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PremiumPurchaseUiState.Idle
            }
            else -> {
                if (productId != null) _purchaseState.value = PremiumPurchaseUiState.Failed(productId, billingResult.responseCode.toFailureReason())
            }
        }
    }

    /** Unlike [BillingManagerImpl.applyPurchases], never grants anything from the raw [Purchase]
     * alone - each one is server-verified via `verifyPremiumPurchase` first (see the class doc).
     * Still acknowledges locally afterward (required within 3 days regardless, per Play policy),
     * and mirrors the grant into [ownedCosmeticDao] directly rather than through
     * [com.suman.memoryarchitect.domain.repository.ShopRepository] - the same "a core class writes
     * straight to the Room cache after a server-confirmed grant" pattern
     * [com.suman.memoryarchitect.core.debug.DebugTestGrantor] already establishes. */
    private suspend fun applyPurchases(purchases: List<Purchase>, reportSuccess: Boolean) {
        for (purchase in purchases) {
            val productId = purchase.products.firstOrNull { it in productIds } ?: continue
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                _purchaseState.value = PremiumPurchaseUiState.Pending(productId)
                continue
            }
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue

            val grantedSkus = runCatching { verifyPurchaseServerSide(productId, purchase.purchaseToken) }
                .onFailure { Log.w(TAG, "verifyPremiumPurchase failed for $productId", it) }
                .getOrNull()
            if (grantedSkus == null) {
                if (reportSuccess) _purchaseState.value = PremiumPurchaseUiState.Failed(productId, BillingFailureReason.GENERIC)
                continue
            }

            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                runCatching { billingClient.acknowledgePurchase(ackParams) }
                    .onFailure { Log.w(TAG, "Failed to acknowledge $productId purchase", it) }
            }

            val grantedIds = grantedSkus.mapNotNull { sku -> runCatching { CosmeticId.valueOf(sku) }.getOrNull() }
            persistOwnedLocally(grantedSkus)
            if (reportSuccess) _purchaseState.value = PremiumPurchaseUiState.Success(productId, grantedIds)
        }
    }

    /** Calls the `verifyPremiumPurchase` Cloud Function (see its doc in `functions/src/index.ts`)
     * and returns the skus it granted, or `null` on any failure (network, not-yet-configured Play
     * Developer API access, a rejected/invalid purchase) - every failure mode is treated the same
     * by the caller: no local grant happens unless the server explicitly confirmed one. */
    private suspend fun verifyPurchaseServerSide(productId: String, purchaseToken: String): List<String>? {
        Firebase.functions.getHttpsCallable("verifyPremiumPurchase")
            .call(mapOf("productId" to productId, "purchaseToken" to purchaseToken))
            .await()
        return PremiumShopCatalog.productOrNull(productId)?.grantedCosmeticIds?.map { it.name }
    }

    private suspend fun persistOwnedLocally(skus: List<String>) {
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        skus.forEach { sku -> ownedCosmeticDao.upsert(OwnedCosmeticEntity(sku, todayEpochDay, "PREMIUM_PURCHASE", System.currentTimeMillis())) }
    }

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
        const val TAG = "PremiumShopManager"
    }
}
