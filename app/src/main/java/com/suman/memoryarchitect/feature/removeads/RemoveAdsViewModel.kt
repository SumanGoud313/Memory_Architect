package com.suman.memoryarchitect.feature.removeads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Thin pass-through to the unified [BillingManager] - all the actual purchase/restore/entitlement
 * logic already lives there; this ViewModel exists only to narrow that manager's catalog-wide,
 * multi-product state down to the one product this screen cares about
 * ([PremiumShopCatalog.REMOVE_ADS_PRODUCT_ID]), so the screen can use plain `collectAsStateWithLifecycle()`
 * on single-value flows rather than filtering a map/productId-keyed state itself. */
@HiltViewModel
class RemoveAdsViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    private val productId = PremiumShopCatalog.REMOVE_ADS_PRODUCT_ID

    val hasRemovedAds: StateFlow<Boolean> = billingManager.hasRemovedAds

    val formattedPrice: StateFlow<String?> = billingManager.productStates
        .map { states -> states[productId]?.formattedPrice }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [billingManager.purchaseState] is shared across every product in the catalog - an event
     * belonging to a different product (e.g. a Premium Collection bought from the Shop tab in the
     * same session) is remapped to [PurchaseUiState.Idle] here rather than leaking through and
     * showing this screen a status message about a purchase it has nothing to do with. */
    val purchaseState: StateFlow<PurchaseUiState> = billingManager.purchaseState
        .map { state -> if (belongsToThisProduct(state)) state else PurchaseUiState.Idle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseUiState.Idle)

    val priceLoadFailed: StateFlow<Boolean> = billingManager.productDetailsLoadFailed

    fun buyNow(activity: Activity) = billingManager.launchPurchase(activity, productId)

    fun restorePurchases() = billingManager.restorePurchases()

    fun retryPriceLoad() = billingManager.retryLoadProductDetails()

    private fun belongsToThisProduct(state: PurchaseUiState): Boolean = when (state) {
        is PurchaseUiState.Loading -> state.productId == productId
        is PurchaseUiState.Success -> state.productId == productId
        is PurchaseUiState.AlreadyOwned -> state.productId == productId
        is PurchaseUiState.Pending -> state.productId == productId
        is PurchaseUiState.Failed -> state.productId == productId
        // Idle/RestoreNotFound carry no product id - a restore attempt spans the whole catalog, so
        // "nothing found" is a meaningful result for this screen too, not something to filter out.
        PurchaseUiState.Idle, PurchaseUiState.RestoreNotFound -> true
    }
}
