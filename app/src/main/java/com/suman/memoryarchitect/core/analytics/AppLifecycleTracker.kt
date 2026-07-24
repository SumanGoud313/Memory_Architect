package com.suman.memoryarchitect.core.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * App-wide (not per-Activity/per-screen) foreground/background tracking, registered onto
 * [androidx.lifecycle.ProcessLifecycleOwner] from [com.suman.memoryarchitect.MemoryArchitectApp]
 * - `onStart`/`onStop` here fire once per app-level foreground/background transition, unlike a
 * single Activity's own lifecycle callbacks which fire on every configuration change too. This is
 * also where [UserPreferencesDataStore]'s session-count/lifetime-play-time counters (see that
 * class - device-local, analytics-only, never gameplay truth) get updated and turned into the
 * corresponding Firebase user properties.
 */
@Singleton
class AppLifecycleTracker @Inject constructor(
    private val analytics: AnalyticsLogger,
    private val preferences: UserPreferencesDataStore,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** When the app most recently entered the foreground - exposed for the debug Analytics
     * Dashboard's "Session Duration" readout. `null` before the first [onStart]. */
    var foregroundedAtMs: Long? = null
        private set

    override fun onStart(owner: LifecycleOwner) {
        val now = System.currentTimeMillis()
        foregroundedAtMs = now
        analytics.logAppForegrounded()
        scope.launch {
            val sessionCount = preferences.incrementSessionCount()
            analytics.setTotalSessionsProperty(sessionCount)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        analytics.logAppBackgrounded()
        val startedAt = foregroundedAtMs ?: return
        val durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
        scope.launch {
            val lifetimeMs = preferences.addPlayTime(durationMs)
            analytics.setLifetimePlayTimeMinutesProperty(lifetimeMs / 60_000L)
        }
    }
}
