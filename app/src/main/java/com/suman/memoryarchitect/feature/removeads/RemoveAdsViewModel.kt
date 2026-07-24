package com.suman.memoryarchitect.feature.removeads

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.core.billing.RemoveAdsProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin pass-through to [BillingManager] - all the actual purchase/restore/entitlement logic
 * already lives there (it's an app-wide `@Singleton`, not ViewModel-scoped, since Play Billing's
 * connection and entitlement state need to outlive any one screen); this ViewModel exists only so
 * the screen can use the standard `hiltViewModel()` + `collectAsStateWithLifecycle()` pattern
 * every other screen in this app already uses, rather than reading a singleton's flows directly
 * from a Composable. */
@HiltViewModel
class RemoveAdsViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    val hasRemovedAds: StateFlow<Boolean> = billingManager.hasRemovedAds
    val product: StateFlow<RemoveAdsProduct?> = billingManager.productDetails
    val purchaseState: StateFlow<PurchaseUiState> = billingManager.purchaseState

    fun buyNow(activity: Activity) {
        billingManager.launchPurchase(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }
}
