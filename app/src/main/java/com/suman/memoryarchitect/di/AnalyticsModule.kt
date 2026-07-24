package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.CompositeAnalyticsLogger
import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.FirebaseAnalyticsLogger
import com.suman.memoryarchitect.core.analytics.FirebaseCrashReporter
import com.suman.memoryarchitect.core.analytics.FirebasePerformanceTracer
import com.suman.memoryarchitect.core.analytics.LogcatAnalyticsLogger
import com.suman.memoryarchitect.core.analytics.PerformanceTracer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    // Every ViewModel/repository injects this one type - it fans out to whichever concrete
    // backends are bound below, so a call site never knows or cares how many there are.
    @Binds
    @Singleton
    abstract fun bindAnalyticsLogger(impl: CompositeAnalyticsLogger): AnalyticsLogger

    @Binds
    @IntoSet
    abstract fun bindFirebaseAnalyticsLogger(impl: FirebaseAnalyticsLogger): AnalyticsLogger

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter

    @Binds
    @Singleton
    abstract fun bindPerformanceTracer(impl: FirebasePerformanceTracer): PerformanceTracer

    companion object {
        // Logcat visibility only in debug builds - BuildConfig.DEBUG is a compile-time constant
        // per build variant, so this resolves once at compile time, not a runtime branch.
        @Provides
        @ElementsIntoSet
        fun provideLogcatAnalyticsLogger(impl: LogcatAnalyticsLogger): Set<@JvmSuppressWildcards AnalyticsLogger> =
            if (BuildConfig.DEBUG) setOf(impl) else emptySet()
    }
}
