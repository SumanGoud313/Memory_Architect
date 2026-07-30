package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.ads.AdConsentGate
import com.suman.memoryarchitect.core.ads.AdConsentManager
import com.suman.memoryarchitect.core.ads.InterstitialAdController
import com.suman.memoryarchitect.core.ads.InterstitialAdControllerImpl
import com.suman.memoryarchitect.core.ads.InterstitialPacingPreferences
import com.suman.memoryarchitect.core.ads.RewardedAdController
import com.suman.memoryarchitect.core.ads.RewardedAdControllerImpl
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Rewarded ([RewardedAdController]), Banner ([com.suman.memoryarchitect.core.ads.BannerAdView],
 * a plain `@Composable` with no controller/interface of its own - a `AdView` is lightweight enough
 * to construct directly per placement, unlike the full-screen formats below), and Interstitial
 * ([InterstitialAdController]) ads all exist in this app. **App Open ads are deliberately not
 * implemented** - this is a memory/concentration game, and the priority on cold launch is getting
 * the player to the Home Screen immediately, not intercepting them with a full-screen ad before
 * they've even seen it once; an app-open interstitial-style ad is also too easy to get wrong
 * (accidental taps on cold launch, a jarring first impression) for the retention upside it offers
 * here. If business requirements ever change: add a new `AppOpenAdController` mirroring
 * [InterstitialAdControllerImpl]'s exact shape, gate it in [com.suman.memoryarchitect.MemoryArchitectApp]'s
 * `ActivityLifecycleCallbacks`, and gate *that* behind its own Remote Config toggle (an
 * `app_open_ads_enabled` key, same `emergencyAdsDisabled()`-folding pattern
 * [com.suman.memoryarchitect.core.ads.bannerAdsEnabled]/[com.suman.memoryarchitect.core.ads.interstitialAdsEnabled]
 * already use) defaulting to `false` - so enabling it later is a console change, not a release, and
 * it starts disabled for every existing install rather than silently turning on. Never show one on
 * the very first app launch even once enabled - check
 * [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore.sessionCount] the same way
 * [InterstitialPacingRules.minSessionCountBeforeAny] already does for interstitials. Every ad
 * surface must check [com.suman.memoryarchitect.core.billing.BillingManager.hasRemovedAds] before
 * showing - Rewarded ads are the one deliberate exception, they must never be gated by it (see that
 * property's doc). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindRewardedAdController(
        impl: RewardedAdControllerImpl,
    ): RewardedAdController

    @Binds
    @Singleton
    abstract fun bindInterstitialAdController(
        impl: InterstitialAdControllerImpl,
    ): InterstitialAdController

    /** See [InterstitialPacingPreferences]'s own doc - this binding is the only place production
     * code learns which concrete type satisfies it; tests substitute a plain fake instead. */
    @Binds
    @Singleton
    abstract fun bindInterstitialPacingPreferences(
        impl: UserPreferencesDataStore,
    ): InterstitialPacingPreferences

    /** See [AdConsentGate]'s own doc - this binding is the only place production code learns which
     * concrete type satisfies it; tests substitute a plain fake instead. */
    @Binds
    @Singleton
    abstract fun bindAdConsentGate(
        impl: AdConsentManager,
    ): AdConsentGate
}
