package com.suman.memoryarchitect.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.suman.memoryarchitect.feature.ads.BannerAdViewModel

/**
 * One reusable adaptive banner, dropped into any non-gameplay screen's layout
 * (`ModeSelectScreen`/`ShopScreen`/`MissionsScreen`/`LuckySpinScreen`/`SettingsScreen`/
 * `AchievementsScreen` - see the placement audit in the monetization plan/report for why these six
 * and not `GameplayScreen`). Renders nothing at all - not even a collapsed placeholder that reflows
 * layout - whenever [BannerAdViewModel.shouldShowBanner] is false (Remove Ads purchased, or Remote
 * Config has banners off), so a Remove Ads purchaser never even constructs an [AdView].
 *
 * Sized via [AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize] - Google's own adaptive-
 * banner API, which is what actually handles phones/foldables/tablets/every DPI/every aspect ratio
 * without per-device code here. [LocalConfiguration.current.screenWidthDp] (not a raw
 * `DisplayMetrics` query) is the width fed into it, both because it's already the Compose-idiomatic
 * "current window width in dp" and because keying the whole [AndroidView] on it
 * ([key(screenWidthDp)]) forces a fresh, correctly-resized [AdView] across rotation or a foldable's
 * fold/unfold - an existing loaded [AdView] can't be resized in place after `loadAd()`.
 *
 * Lifecycle-correct: [AdView.pause]/[AdView.resume]/[AdView.destroy] are driven by
 * [LocalLifecycleOwner] via [DisposableEffect], not left to get garbage-collected - the same
 * "no leaked ad surface across navigation" requirement [InterstitialAdControllerImpl]'s
 * dead-Activity check satisfies for full-screen formats.
 */
@Composable
fun AdaptiveBannerAd(placement: String, modifier: Modifier = Modifier, viewModel: BannerAdViewModel = hiltViewModel()) {
    val shouldShow by viewModel.shouldShowBanner.collectAsStateWithLifecycle()
    if (!shouldShow) return
    // No activity (a rare host-less preview/test context) means nowhere safe to attach a banner -
    // same "nothing to show" outcome as shouldShow being false, never a crash.
    LocalActivity.current ?: return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    key(screenWidthDp) {
        val adViewHolder = remember { AdViewHolder() }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adViewHolder.adView?.resume()
                    Lifecycle.Event.ON_PAUSE -> adViewHolder.adView?.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adViewHolder.adView?.destroy()
                adViewHolder.adView = null
            }
        }

        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { factoryContext ->
                val loadStartedAtMs = System.currentTimeMillis()
                AdView(factoryContext).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp))
                    adUnitId = BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            viewModel.onBannerImpression(placement)
                            viewModel.onBannerLoadLatency(placement, System.currentTimeMillis() - loadStartedAtMs, success = true)
                        }
                        override fun onAdClicked() = viewModel.onBannerClicked(placement)
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            viewModel.onBannerLoadFailed(placement)
                            viewModel.onBannerLoadLatency(placement, System.currentTimeMillis() - loadStartedAtMs, success = false)
                        }
                    }
                    onPaidEventListener = OnPaidEventListener { adValue ->
                        viewModel.onBannerRevenue(placement, adValue.valueMicros, adValue.currencyCode)
                    }
                    adViewHolder.adView = this
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}

private class AdViewHolder {
    var adView: AdView? = null
}

/** Google's official Android test adaptive banner ad unit ID - always fills with a test creative,
 * never a real ad, safe to ship in any build. Swap for a real ad unit ID (from your own AdMob
 * console) before release - same placeholder convention [RewardedAdControllerImpl]/
 * [InterstitialAdControllerImpl] already use. */
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
