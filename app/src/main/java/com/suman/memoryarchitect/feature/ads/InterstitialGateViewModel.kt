package com.suman.memoryarchitect.feature.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.ads.InterstitialAdController
import com.suman.memoryarchitect.core.ads.InterstitialAdResult
import com.suman.memoryarchitect.core.ads.InterstitialPacingGate
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logInterstitialSkipped
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single call site that decides whether to show an interstitial before completing a
 * navigation action, and then always completes it regardless of what happened - see
 * [maybeShowInterstitial]'s own doc for why this can never block or delay navigation on an ad
 * problem. Backs [com.suman.memoryarchitect.ui.navigation.MemoryArchitectNavHost]'s `Route.Gameplay`
 * `onBack` wiring, the one real trigger point this app's interstitial pacing is built around (see
 * [InterstitialPacingGate]'s own doc).
 */
@HiltViewModel
class InterstitialGateViewModel @Inject constructor(
    private val interstitialAdController: InterstitialAdController,
    private val interstitialPacingGate: InterstitialPacingGate,
    private val billingManager: BillingManager,
    private val getStatistics: GetStatisticsUseCase,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    /** [onComplete] always runs exactly once - immediately if [activity] is null, the player has
     * purchased Remove Ads (checked first, before even touching [InterstitialPacingGate] - a
     * purchaser's navigation should never wait on a pacing/stats lookup for an ad that was never
     * going to show), or [InterstitialPacingGate] says this isn't a good moment (routine pacing, not
     * a failure); and again (after natural dismissal) only on a genuinely shown interstitial. A
     * load/show failure from [InterstitialAdController] itself is treated exactly like "not
     * eligible" here - the controller has already logged the specific failure reason, this call site
     * just needs to proceed with navigation either way, per "never block gameplay because an ad
     * fails." */
    fun maybeShowInterstitial(activity: Activity?, placement: String = PLACEMENT_LEVEL_SELECT_RETURN, onComplete: () -> Unit) {
        if (activity == null || billingManager.hasRemovedAds.value) {
            onComplete()
            return
        }
        viewModelScope.launch {
            val gamesPlayed = getStatistics().gamesPlayed
            val skipReason = interstitialPacingGate.skipReasonOrNull(gamesPlayed)
            if (skipReason != null) {
                analytics.logInterstitialSkipped(placement, skipReason.analyticsName)
                onComplete()
                return@launch
            }
            if (interstitialAdController.loadAndShow(activity, placement) is InterstitialAdResult.Shown) {
                interstitialPacingGate.recordInterstitialShown(gamesPlayed)
            }
            onComplete()
        }
    }

    private companion object {
        const val PLACEMENT_LEVEL_SELECT_RETURN = "level_select_return"
    }
}
