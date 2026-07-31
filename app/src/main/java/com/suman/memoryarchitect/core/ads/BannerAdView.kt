package com.suman.memoryarchitect.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * One reusable adaptive banner, dropped into every non-gameplay screen's layout (Mode Select,
 * Missions, Lucky Spin, Inventory, Leaderboard, Settings, Shop, Collections, Profile,
 * Achievements - never `GameplayScreen`, see [bannerAdUnitIdFor] for each placement's real ad
 * unit). Renders nothing at all - not even a collapsed placeholder that reflows layout - whenever
 * [BannerAdViewModel.shouldShowBanner] is false (Remove Ads purchased, or Remote Config has banners
 * off), so a Remove Ads purchaser never even constructs an [AdView].
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
 *
 * [onHeightChanged] reports this banner's real, currently-rendered height (`0.dp` whenever nothing
 * is actually shown - not shown, no activity, or not yet loaded) so a caller with bottom-anchored
 * content of its own (a `Snackbar`, a scrollable list's last row, a fixed action button) can reserve
 * exactly that much space rather than risk the banner overlapping and hiding it - see
 * `InventoryScreen.kt`'s own call site for why this matters (a Mystery Chest's "you got N coins"
 * Snackbar was rendering directly underneath the banner, invisible, before this existed).
 *
 * [Modifier.navigationBarsPadding] is applied internally, once, rather than in every caller - every
 * call site except `ModeSelectScreen`'s own bottom-anchored copy placed this banner flush against
 * the very bottom of an edge-to-edge window with no clearance at all, so on any 2/3-button
 * navigation-bar device (not just gesture nav, where the inset is usually negligible) the ad's
 * bottom portion rendered partly or fully behind the nav bar. [onHeightChanged] still reports only
 * the ad's own rendered height, not this padding - it's measured before the padding is applied, so
 * a caller reserving space *above* this banner (Inventory's Snackbar, a scrollable list's bottom
 * padding) is unaffected; the padding only adds clearance *below* the ad, between it and the real
 * screen edge.
 */
@Composable
fun AdaptiveBannerAd(
    placement: String,
    modifier: Modifier = Modifier,
    onHeightChanged: (Dp) -> Unit = {},
    viewModel: BannerAdViewModel = hiltViewModel(),
) {
    val shouldShow by viewModel.shouldShowBanner.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    // No activity (a rare host-less preview/test context) means nowhere safe to attach a banner -
    // same "nothing to show" outcome as shouldShow being false, never a crash.
    val visible = shouldShow && activity != null
    LaunchedEffect(visible) { if (!visible) onHeightChanged(0.dp) }
    if (!visible) return

    val context = LocalContext.current
    val density = LocalDensity.current
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
                onHeightChanged(0.dp)
            }
        }

        AndroidView(
            // navigationBarsPadding() before onSizeChanged (not after) so the reported height
            // stays just the ad's own rendered size - the padding it adds becomes an outer wrapper
            // around the measurement point below, invisible to it, rather than inflating it.
            modifier = modifier.fillMaxWidth().navigationBarsPadding().onSizeChanged { size ->
                onHeightChanged(with(density) { size.height.toDp() })
            },
            factory = { factoryContext ->
                val loadStartedAtMs = System.currentTimeMillis()
                AdView(factoryContext).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp))
                    adUnitId = bannerAdUnitIdFor(placement)
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

/** One real AdMob ad unit per screen/screen-group, matching [placement]'s exact string from each
 * [AdaptiveBannerAd] call site - "rewards" in the product's own naming is the Inventory screen (see
 * `LuckySpinScreen.kt`'s "Rewards/Inventory screen" doc), sharing the Lucky Spin unit. An
 * unrecognized placement (should never happen - every real call site is listed here) falls back to
 * the mode-select unit rather than crashing. */
private fun bannerAdUnitIdFor(placement: String): String = when (placement) {
    "missions" -> "ca-app-pub-6355592583655922/7292282174"
    "mode_select" -> "ca-app-pub-6355592583655922/3559655377"
    "lucky_spin", "inventory" -> "ca-app-pub-6355592583655922/8772884805"
    "leaderboard", "settings" -> "ca-app-pub-6355592583655922/7459803136"
    "shop", "collections" -> "ca-app-pub-6355592583655922/9335991582"
    "profile", "achievements" -> "ca-app-pub-6355592583655922/2207476455"
    else -> "ca-app-pub-6355592583655922/3559655377"
}
