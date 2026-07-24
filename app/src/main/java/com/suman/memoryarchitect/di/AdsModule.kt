package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.ads.RewardedAdController
import com.suman.memoryarchitect.core.ads.RewardedAdControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Only Rewarded ads exist in this app today (Hint/Redo/Rewatch, gated behind [RewardedAdController]
 * below) - there is no Banner, Interstitial, or App Open ad implementation to audit or gate. If any
 * of those are added later, check [com.suman.memoryarchitect.core.billing.BillingManager.hasRemovedAds]
 * before showing one; Rewarded ads must never be gated by it - see that property's doc. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindRewardedAdController(
        impl: RewardedAdControllerImpl,
    ): RewardedAdController
}
