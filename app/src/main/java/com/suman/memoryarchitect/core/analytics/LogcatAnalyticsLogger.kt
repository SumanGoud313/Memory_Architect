package com.suman.memoryarchitect.core.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logcat-backed [AnalyticsLogger] — always included alongside [FirebaseAnalyticsLogger] in debug
 * builds (see `AnalyticsModule`) so events are visible during development regardless of whether a
 * real Firebase project is configured yet.
 */
@Singleton
class LogcatAnalyticsLogger @Inject constructor() : AnalyticsLogger {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        val paramsText = if (params.isEmpty()) "" else " " + params.entries.joinToString(", ") { (key, value) -> "$key=$value" }
        Log.d(TAG, "event: $name$paramsText")
    }

    override fun setUserProperty(name: String, value: String?) {
        Log.d(TAG, "user_property: $name=$value")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
