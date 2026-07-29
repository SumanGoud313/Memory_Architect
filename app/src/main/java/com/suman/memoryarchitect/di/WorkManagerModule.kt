package com.suman.memoryarchitect.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** [WorkManager.getInstance] only works once WorkManager has actually been initialized - since
 * [com.suman.memoryarchitect.MemoryArchitectApp] disables the default `androidx.startup`
 * auto-initializer in favor of on-demand init via `Configuration.Provider` (see its doc), this is
 * the one place that first call happens, safely after Hilt itself has finished constructing the
 * Application (and therefore the injected `HiltWorkerFactory` [Configuration.Provider] needs). */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
