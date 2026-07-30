package com.suman.memoryarchitect.core.ads

import android.app.Activity
import android.content.Context
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logAdLoadLatency
import com.suman.memoryarchitect.core.analytics.logAdRevenue
import com.suman.memoryarchitect.core.analytics.logInterstitialClicked
import com.suman.memoryarchitect.core.analytics.logInterstitialDismissed
import com.suman.memoryarchitect.core.analytics.logInterstitialLoadFailed
import com.suman.memoryarchitect.core.analytics.logInterstitialLoaded
import com.suman.memoryarchitect.core.analytics.logInterstitialRequested
import com.suman.memoryarchitect.core.analytics.logInterstitialShowFailed
import com.suman.memoryarchitect.core.analytics.logInterstitialShown
import com.suman.memoryarchitect.core.connectivity.NetworkMonitor
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin, testable wrapper around the Google Mobile Ads interstitial-ad flow - mirrors
 * [RewardedAdControllerImpl] line for line (same [NetworkMonitor] short-circuit, same lazy
 * [MobileAds.initialize] shared behind one [Mutex]... actually initialized independently here since
 * this class has no reference to [RewardedAdControllerImpl]'s own flag; [MobileAds.initialize] is
 * itself idempotent/safe to call from more than one place, so this isn't a real duplicate-init risk,
 * just two call sites racing to be first). Same init/load timeouts, same dead-Activity guard before
 * showing. [InterstitialPacingGate] is what decides *whether* this should be called at all - this
 * class only knows how to load/show one when asked.
 */
@Singleton
class InterstitialAdControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val analytics: AnalyticsLogger,
    private val adConsentGate: AdConsentGate,
) : InterstitialAdController {

    private val initMutex = Mutex()
    private var isInitialized = false

    override suspend fun loadAndShow(activity: Activity, placement: String): InterstitialAdResult {
        if (!networkMonitor.isOnline.value) {
            return InterstitialAdResult.Failed(InterstitialAdFailureReason.NO_INTERNET)
        }
        // UMP policy: never request an ad before consent is gathered (where required) - see
        // AdConsentManager's own doc.
        if (!adConsentGate.canRequestAds) {
            return InterstitialAdResult.Failed(InterstitialAdFailureReason.AD_UNAVAILABLE)
        }
        ensureInitialized()

        analytics.logInterstitialRequested(placement)
        val ad = loadAd(placement) ?: return InterstitialAdResult.Failed(InterstitialAdFailureReason.AD_UNAVAILABLE)
        // Same reasoning as RewardedAdControllerImpl.loadAndShow - the load round-trip can outlast
        // the hosting screen (a fast back-tap, an app background) before the ad is ready to show.
        if (activity.isFinishing || activity.isDestroyed) {
            return InterstitialAdResult.Failed(InterstitialAdFailureReason.SHOW_FAILED)
        }
        return showAd(activity, ad, placement)
    }

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

    private suspend fun loadAd(placement: String): InterstitialAd? = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
        val loadStartedAtMs = System.currentTimeMillis()
        suspendCancellableCoroutine { continuation ->
            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        analytics.logInterstitialLoaded(placement)
                        analytics.logAdLoadLatency("interstitial", placement, System.currentTimeMillis() - loadStartedAtMs, success = true)
                        interstitialAd.onPaidEventListener = OnPaidEventListener { adValue ->
                            analytics.logAdRevenue(placement, adValue.valueMicros, adValue.currencyCode, adFormat = "interstitial")
                        }
                        if (continuation.isActive) continuation.resume(interstitialAd)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        analytics.logInterstitialLoadFailed(placement)
                        analytics.logAdLoadLatency("interstitial", placement, System.currentTimeMillis() - loadStartedAtMs, success = false)
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    private suspend fun showAd(activity: Activity, ad: InterstitialAd, placement: String): InterstitialAdResult =
        suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    analytics.logInterstitialDismissed(placement)
                    if (continuation.isActive) continuation.resume(InterstitialAdResult.Shown)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    analytics.logInterstitialShowFailed(placement)
                    if (continuation.isActive) continuation.resume(InterstitialAdResult.Failed(InterstitialAdFailureReason.SHOW_FAILED))
                }

                override fun onAdClicked() {
                    analytics.logInterstitialClicked(placement)
                }
            }
            analytics.logInterstitialShown(placement)
            ad.show(activity)
        }

    private companion object {
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6355592583655922/8909184395"
        const val INIT_TIMEOUT_MS = 10_000L
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
