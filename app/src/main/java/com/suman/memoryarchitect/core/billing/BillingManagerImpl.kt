package com.suman.memoryarchitect.core.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import android.util.Log
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * See [BillingManager] for the contract. Shares [SharedBillingClient]'s one [BillingClient]
 * instance for the whole app process (see that class's doc for why) rather than constructing its
 * own - this manager only ever queries/purchases/acknowledges `remove_ads_lifetime`, filtering
 * [SharedBillingClient.purchasesUpdated] for that one product id.
 */
@Singleton
class BillingManagerImpl @Inject constructor(
    private val sharedBillingClient: SharedBillingClient,
    private val preferences: UserPreferencesDataStore,
    private val dispatchers: DispatcherProvider,
) : BillingManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var startConnectionCalled = false

    /** [SharedBillingClient.purchasesUpdated] is one signal shared with `PremiumShopManager` - an
     * update that doesn't mention `remove_ads_lifetime` might be about a *different* purchase
     * flow entirely (someone buying a themed collection while this manager has nothing in
     * flight), which must never be allowed to reset [_purchaseState] to [PurchaseUiState.Idle].
     * Only react to a "nothing relevant" update when a flow *this* manager launched is still
     * outstanding. */
    private var purchaseInFlight = false

    private val _hasRemovedAds = MutableStateFlow(false)
    override val hasRemovedAds: StateFlow<Boolean> = _hasRemovedAds.asStateFlow()

    private val _productDetails = MutableStateFlow<RemoveAdsProduct?>(null)
    override val productDetails: StateFlow<RemoveAdsProduct?> = _productDetails.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    override val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    /** The full SDK object Play returned for `remove_ads_lifetime`, needed to actually launch a
     * purchase - [productDetails] is only the small subset of it the UI ever needs to display. */
    private var resolvedProductDetails: ProductDetails? = null

    private val billingClient: BillingClient get() = sharedBillingClient.billingClient

    override fun startConnection() {
        if (startConnectionCalled) return
        startConnectionCalled = true
        scope.launch {
            // Instant local seed so a caller reading hasRemovedAds before the network round-trip
            // below completes (e.g. this screen's very first frame) never shows "Buy Now" to
            // someone who already owns this - see UserPreferencesDataStore.hasRemovedAds's doc.
            _hasRemovedAds.value = preferences.hasRemovedAds.first()
        }
        sharedBillingClient.startConnection()
        scope.launch {
            sharedBillingClient.isReady.filter { it }.collect {
                loadProductDetails()
                queryAndApplyExistingPurchases()
            }
        }
        scope.launch {
            sharedBillingClient.purchasesUpdated.collect { update ->
                val relevant = update.purchases.filter { REMOVE_ADS_PRODUCT_ID in it.products }
                when {
                    relevant.isNotEmpty() -> handlePurchasesUpdated(update.billingResult, relevant)
                    purchaseInFlight -> handlePurchasesUpdated(update.billingResult, emptyList())
                    // Not our product and no flow of ours is outstanding - some other manager's
                    // update, not ours to react to.
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = runCatching { billingClient.queryProductDetails(params) }
            .onFailure { Log.w(TAG, "queryProductDetails failed", it) }
            .getOrNull() ?: return
        val details = result.productDetailsList?.firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID } ?: return
        resolvedProductDetails = details
        val formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice ?: return
        _productDetails.value = RemoveAdsProduct(formattedPrice)
    }

    private suspend fun queryAndApplyExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val result = runCatching { billingClient.queryPurchasesAsync(params) }
            .onFailure { Log.w(TAG, "queryPurchasesAsync failed", it) }
            .getOrNull() ?: return
        applyPurchases(result.purchasesList)
    }

    override fun launchPurchase(activity: Activity) {
        if (_hasRemovedAds.value) {
            _purchaseState.value = PurchaseUiState.AlreadyOwned
            return
        }
        val details = resolvedProductDetails
        if (details == null) {
            _purchaseState.value = PurchaseUiState.Failed(BillingFailureReason.NOT_READY)
            return
        }
        // No offer token needed here - that's a subscription base-plan/offer-selection concept.
        // A plain one-time non-consumable product's single default offer needs nothing beyond
        // the ProductDetails itself.
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        purchaseInFlight = true
        _purchaseState.value = PurchaseUiState.Loading
        val launchResult = billingClient.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseInFlight = false
            _purchaseState.value = PurchaseUiState.Failed(launchResult.responseCode.toFailureReason())
        }
        // Any other outcome (OK, or the flow actually opening) resolves later via
        // handlePurchasesUpdated - never resolved here.
    }

    override fun restorePurchases() {
        _purchaseState.value = PurchaseUiState.Loading
        scope.launch {
            queryAndApplyExistingPurchases()
            // applyPurchases only overwrites purchaseState on an actual grant (Success) or a
            // Pending purchase found along the way - if neither happened, this explicit restore
            // attempt found nothing to restore, which is worth telling the player rather than
            // leaving them on a silent Loading spinner.
            if (_purchaseState.value == PurchaseUiState.Loading) {
                _purchaseState.value = if (_hasRemovedAds.value) PurchaseUiState.AlreadyOwned else PurchaseUiState.RestoreNotFound
            }
        }
    }

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        purchaseInFlight = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _purchaseState.value = PurchaseUiState.Idle
                    return
                }
                scope.launch { applyPurchases(purchases, reportSuccess = true) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Shouldn't normally be reachable (launchPurchase already checks hasRemovedAds
                // first), but Play is the actual source of truth, not this app's cached flag - if
                // Play says already-owned, re-sync from Play rather than trust the local state.
                scope.launch {
                    queryAndApplyExistingPurchases()
                    _purchaseState.value = PurchaseUiState.AlreadyOwned
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseUiState.Idle
            }
            else -> {
                _purchaseState.value = PurchaseUiState.Failed(billingResult.responseCode.toFailureReason())
            }
        }
    }

    /** Shared by the initial restore query, [handlePurchasesUpdated]'s OK branch, and
     * [restorePurchases] - the one place a raw [Purchase] list becomes this app's actual
     * [hasRemovedAds] state and gets acknowledged, so that only ever happens once regardless of
     * which of those three triggered it. Per Google Play Billing policy, a non-consumable purchase
     * left unacknowledged for 3 days is automatically refunded and revoked - every code path that
     * can observe a `PURCHASED` [Purchase] funnels through here specifically so that can never
     * happen. */
    private suspend fun applyPurchases(purchases: List<Purchase>, reportSuccess: Boolean = false) {
        var granted = false
        for (purchase in purchases) {
            if (!isGrantedEntitlement(purchase.products, purchase.purchaseState, REMOVE_ADS_PRODUCT_ID)) {
                if (purchase.purchaseState == Purchase.PurchaseState.PENDING && REMOVE_ADS_PRODUCT_ID in purchase.products) {
                    _purchaseState.value = PurchaseUiState.Pending
                }
                continue
            }
            granted = true
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                runCatching { billingClient.acknowledgePurchase(ackParams) }
                    .onFailure { Log.w(TAG, "Failed to acknowledge $REMOVE_ADS_PRODUCT_ID purchase", it) }
            }
        }
        if (granted) {
            _hasRemovedAds.value = true
            preferences.setHasRemovedAds(true)
            if (reportSuccess) _purchaseState.value = PurchaseUiState.Success
        }
    }

    private fun Int.toFailureReason(): BillingFailureReason = when (this) {
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingFailureReason.ITEM_UNAVAILABLE
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        -> BillingFailureReason.BILLING_UNAVAILABLE
        else -> BillingFailureReason.GENERIC
    }

    private companion object {
        const val TAG = "BillingManager"
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads_lifetime"
    }
}
