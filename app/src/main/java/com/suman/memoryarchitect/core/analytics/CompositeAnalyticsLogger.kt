package com.suman.memoryarchitect.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fans a single [logEvent]/[setUserProperty] call out to every bound backend (see
 * `AnalyticsModule` for which ones - Firebase always, Logcat in debug builds only). This is the
 * only [AnalyticsLogger] bound into the app - every ViewModel/repository injects this one type and
 * never knows how many real backends are behind it.
 */
@Singleton
class CompositeAnalyticsLogger @Inject constructor(
    private val loggers: Set<@JvmSuppressWildcards AnalyticsLogger>,
) : AnalyticsLogger {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        loggers.forEach { it.logEvent(name, params) }
    }

    override fun setUserProperty(name: String, value: String?) {
        loggers.forEach { it.setUserProperty(name, value) }
    }
}
