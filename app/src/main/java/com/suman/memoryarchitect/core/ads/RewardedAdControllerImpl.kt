package com.suman.memoryarchitect.core.ads

import android.app.Activity
import android.content.Context
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logAdClosed
import com.suman.memoryarchitect.core.analytics.logAdLoadFailed
import com.suman.memoryarchitect.core.analytics.logAdLoadLatency
import com.suman.memoryarchitect.core.analytics.logAdLoaded
import com.suman.memoryarchitect.core.analytics.logAdRequested
import com.suman.memoryarchitect.core.analytics.logAdRevenue
import com.suman.memoryarchitect.core.analytics.logAdShowFailed
import com.suman.memoryarchitect.core.analytics.logAdShown
import com.suman.memoryarchitect.core.connectivity.NetworkMonitor
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin, testable wrapper around the Google Mobile Ads rewarded-ad flow. [NetworkMonitor] gives a
 * fast, explicit "no internet" outcome without ever bothering the ad SDK - the SDK's own network
 * errors are surfaced too (as [RewardedAdFailureReason.AD_UNAVAILABLE]), but only after actually
 * attempting a load. The Mobile Ads SDK is initialized lazily on first use rather than at app
 * startup, so this package stays fully self-contained (nothing to wire into the Application
 * class).
 */
@Singleton
class RewardedAdControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val analytics: AnalyticsLogger,
    private val interstitialPacingGate: InterstitialPacingGate,
    private val adConsentGate: AdConsentGate,
) : RewardedAdController {

    private val initMutex = Mutex()
    private var isInitialized = false

    override suspend fun loadAndShow(activity: Activity, feature: String): RewardedAdResult {
        if (!networkMonitor.isOnline.value) {
            return RewardedAdResult.Failed(RewardedAdFailureReason.NO_INTERNET)
        }
        // UMP policy: never request an ad before consent is gathered (where required) - see
        // AdConsentManager's own doc. Same "no ad available" outcome as any other unavailable-ad
        // reason, since to a player these are indistinguishable anyway.
        if (!adConsentGate.canRequestAds) {
            return RewardedAdResult.Failed(RewardedAdFailureReason.AD_UNAVAILABLE)
        }
        ensureInitialized()

        analytics.logAdRequested(feature)
        val ad = loadAd(feature) ?: return RewardedAdResult.Failed(RewardedAdFailureReason.AD_UNAVAILABLE)
        // The load round-trip above can take long enough for the player to background the app or
        // finish the hosting screen (e.g. a fast back-tap) before the ad is actually ready to
        // show - showing on a dead/finishing Activity is a real crash risk with the Ads SDK, so
        // this is checked fresh here rather than trusting that whoever called loadAndShow is
        // still around by the time it returns.
        if (activity.isFinishing || activity.isDestroyed) {
            return RewardedAdResult.Failed(RewardedAdFailureReason.SHOW_FAILED)
        }
        val result = showAd(activity, ad, feature)
        // Rewarded/Cancelled both mean the ad was genuinely shown and dismissed (only Failed means
        // no full-screen experience ever happened) - see InterstitialPacingGate.recordRewardedAdShown's
        // own doc for why this stamps the interstitial cooldown regardless of which rewarded
        // placement (Hint/Redo/Rewatch/Lucky Spin/...) triggered it.
        if (result !is RewardedAdResult.Failed) {
            interstitialPacingGate.recordRewardedAdShown()
        }
        return result
    }

    /** Bounded so a hung SDK callback (dead network mid-handshake, first-run init that never
     * completes) degrades to a normal [RewardedAdFailureReason] instead of leaving every future
     * call blocked forever on [initMutex] - only the network-bound load/init steps get a timeout;
     * once an ad is actually showing full-screen, [showAd] lets it run to whatever natural
     * dismissal the SDK/user produces rather than risk cancelling a continuation the player is
     * mid-way through earning a reward on. */
    private suspend fun ensureInitialized() {
        if (isInitialized) return
        initMutex.withLock {
            if (isInitialized) return@withLock
            withTimeoutOrNull(INIT_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    MobileAds.initialize(context) {
                        isInitialized = true
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
    }

    private suspend fun loadAd(feature: String): RewardedAd? = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
        val loadStartedAtMs = System.currentTimeMillis()
        suspendCancellableCoroutine { continuation ->
            RewardedAd.load(
                context,
                rewardedAdUnitIdFor(feature),
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        analytics.logAdLoaded(feature)
                        analytics.logAdLoadLatency("rewarded", feature, System.currentTimeMillis() - loadStartedAtMs, success = true)
                        rewardedAd.onPaidEventListener = OnPaidEventListener { adValue ->
                            analytics.logAdRevenue(feature, adValue.valueMicros, adValue.currencyCode)
                        }
                        if (continuation.isActive) continuation.resume(rewardedAd)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        analytics.logAdLoadFailed(feature)
                        analytics.logAdLoadLatency("rewarded", feature, System.currentTimeMillis() - loadStartedAtMs, success = false)
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    private suspend fun showAd(activity: Activity, ad: RewardedAd, feature: String): RewardedAdResult = suspendCancellableCoroutine { continuation ->
        // Reward and dismissal are two independent callbacks that can fire in either order in
        // principle, so track "earned" as local state and only resolve once the ad has actually
        // finished (onAdDismissedFullScreenContent), not the instant the reward itself fires.
        var earnedReward = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                analytics.logAdClosed(feature, earnedReward)
                val result = if (earnedReward) RewardedAdResult.Rewarded else RewardedAdResult.Cancelled
                if (continuation.isActive) continuation.resume(result)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                analytics.logAdShowFailed(feature)
                if (continuation.isActive) continuation.resume(RewardedAdResult.Failed(RewardedAdFailureReason.SHOW_FAILED))
            }
        }
        analytics.logAdShown(feature)
        ad.show(activity) { earnedReward = true }
    }

    /** One real AdMob ad unit per rewarded placement - [feature] is the exact string every
     * [RewardedAdFlow] call site already tags its requests with (see that class/`GameplayViewModel`/
     * `LuckySpinViewModel`'s own construction sites), so this never needs its own separate enum. An
     * unrecognized feature (should never happen - every real call site is listed here) falls back
     * to the hint unit rather than crashing. */
    private fun rewardedAdUnitIdFor(feature: String): String = when (feature) {
        "hint" -> "ca-app-pub-6355592583655922/5586504172"
        "rewatch" -> "ca-app-pub-6355592583655922/2015638836"
        "redo" -> "ca-app-pub-6355592583655922/4642068104"
        "lucky_spin" -> "ca-app-pub-6355592583655922/5943541482"
        "lucky_spin_mystery_chest" -> "ca-app-pub-6355592583655922/5899877673"
        else -> "ca-app-pub-6355592583655922/5586504172"
    }

    private companion object {
        const val INIT_TIMEOUT_MS = 10_000L
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
