package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.BillingManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The one and only real-money billing manager binding in this app - see [BillingManager]'s own doc
 * for why a second one must never be added. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingManager(impl: BillingManagerImpl): BillingManager
}
