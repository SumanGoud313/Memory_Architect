package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.BillingManagerImpl
import com.suman.memoryarchitect.core.billing.PremiumShopManager
import com.suman.memoryarchitect.core.billing.PremiumShopManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingManager(impl: BillingManagerImpl): BillingManager

    @Binds
    @Singleton
    abstract fun bindPremiumShopManager(impl: PremiumShopManagerImpl): PremiumShopManager
}
