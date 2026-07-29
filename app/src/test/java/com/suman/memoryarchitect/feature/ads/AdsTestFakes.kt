package com.suman.memoryarchitect.feature.ads

import android.app.Activity
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.BillingProductUiState
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared by [BannerAdViewModelTest] and [InterstitialGateViewModelTest] - both exercise the exact
 * same "does this ad surface react to BillingManager.hasRemovedAds" question against the same
 * fake, rather than each file declaring its own near-identical copy (which Kotlin also wouldn't
 * allow silently - two `private class FakeBillingManager` in the same package/file set is a
 * redeclaration error, not just a style choice). */
internal class FakeBillingManager(hasRemovedAds: Boolean = false) : BillingManager {
    private val hasRemovedAdsFlow = MutableStateFlow(hasRemovedAds)
    override val hasRemovedAds = hasRemovedAdsFlow
    override val productStates = MutableStateFlow<Map<String, BillingProductUiState>>(emptyMap())
    override val purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    override val productDetailsLoadFailed = MutableStateFlow(false)

    fun setHasRemovedAds(value: Boolean) {
        hasRemovedAdsFlow.value = value
    }

    override fun retryLoadProductDetails() = throw UnsupportedOperationException("not exercised by these tests")
    override fun startConnection() = throw UnsupportedOperationException("not exercised by these tests")
    override fun launchPurchase(activity: Activity, productId: String) = throw UnsupportedOperationException("not exercised by these tests")
    override fun restorePurchases() = throw UnsupportedOperationException("not exercised by these tests")
}

internal class FakeAnalyticsLogger : AnalyticsLogger {
    val loggedEventNames = mutableListOf<String>()
    override fun logEvent(name: String, params: Map<String, Any?>) {
        loggedEventNames += name
    }
    override fun setUserProperty(name: String, value: String?) = Unit
}
