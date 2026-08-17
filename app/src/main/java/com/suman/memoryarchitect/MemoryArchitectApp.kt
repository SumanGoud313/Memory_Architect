package com.suman.memoryarchitect

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration as AndroidConfiguration
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration as WorkConfiguration
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.AppLifecycleTracker
import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.logMemoryWarning
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.notifications.NotificationScheduler
import com.suman.memoryarchitect.core.security.DeviceIntegrityChecker
import com.suman.memoryarchitect.core.sync.PendingMissionClaimSyncScheduler
import com.suman.memoryarchitect.core.sync.PendingScoreSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [WorkConfiguration.Provider] disables WorkManager's default `androidx.startup` auto-init (see
 * the manifest's `tools:node="remove"` on `WorkManagerInitializer`) in favor of building it lazily
 * with the injected [HiltWorkerFactory] - required for [com.suman.memoryarchitect.core.notifications.DailyReminderWorker]'s
 * `@HiltWorker` constructor injection to work at all; the default factory can't construct a worker
 * with non-trivial dependencies.
 */
@HiltAndroidApp
class MemoryArchitectApp : Application(), ComponentCallbacks2, WorkConfiguration.Provider {

    @Inject lateinit var appLifecycleTracker: AppLifecycleTracker

    @Inject lateinit var crashReporter: CrashReporter

    @Inject lateinit var analytics: AnalyticsLogger

    @Inject lateinit var playerIdentityManager: PlayerIdentityManager

    @Inject lateinit var deviceIntegrityChecker: DeviceIntegrityChecker

    @Inject lateinit var billingManager: BillingManager

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var notificationScheduler: NotificationScheduler

    @Inject lateinit var pendingScoreSyncScheduler: PendingScoreSyncScheduler

    @Inject lateinit var pendingMissionClaimSyncScheduler: PendingMissionClaimSyncScheduler

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleTracker)
        crashReporter.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashReporter.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        // Silent, no login screen - see PlayerIdentityManager's doc. Kicked off once here so a
        // stable uid is usually already available by the time the player ever opens a leaderboard
        // screen, rather than paying the round-trip on first visit.
        playerIdentityManager.ensureSignedIn()
        // Best-effort, advisory-only - see DeviceIntegrityChecker's doc for why a failure here
        // (untestable in this environment, expected until Play Console linking exists) is never
        // surfaced to the player or treated as an error.
        ProcessLifecycleOwner.get().lifecycleScope.launch { deviceIntegrityChecker.checkOpportunistically() }
        // Connects to Play Billing and re-verifies every catalog product's entitlement (Remove Ads
        // and every Premium Collection alike - one unified manager now covers both, see
        // BillingManager's own doc) against the signed-in Google Play account - this, not any
        // explicit "Restore" tap, is what actually satisfies "restore automatically on reinstall/new
        // device."
        billingManager.startConnection()
        // Idempotent (KEEP policy) - safe to call on every cold start, never re-delays an
        // already-queued reminder chain. See NotificationScheduler's doc.
        notificationScheduler.schedule()
        // Catches anything still queued from a previous session that never got flushed (e.g. the
        // app was killed before connectivity returned) - a no-op if the queue is already empty.
        // See PendingScoreSyncScheduler's doc.
        pendingScoreSyncScheduler.scheduleRetry()
        // Same reasoning as pendingScoreSyncScheduler above, for queued mission claims instead of
        // score submissions - see PendingMissionClaimSyncScheduler's doc.
        pendingMissionClaimSyncScheduler.scheduleRetry()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // TRIM_MEMORY_UI_HIDDEN fires every single time this app's UI simply stops being visible -
        // switching apps, locking the screen, a rewarded ad covering the screen - completely
        // independent of whether the device is actually short on memory. Logging it as
        // "memory_warning" was the real bug behind the high daily event volume: it fires dozens of
        // times a day per player from ordinary navigation alone, none of it real memory pressure.
        // Every other level here - RUNNING_MODERATE/LOW/CRITICAL while this app is foregrounded, and
        // BACKGROUND/MODERATE/COMPLETE while it's cached - is the system genuinely asking this
        // process to free memory, so those still log exactly as before.
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) return
        analytics.logMemoryWarning(trimMemoryLevelName(level))
    }

    // Both call super (fixing a real gap, not stylistic) - Application's own base implementation
    // fans these out to every ComponentCallbacks2 any library registered (Coil's ImageLoader is
    // exactly this kind of registrant - it trims/invalidates its bitmap memory cache on a config
    // change or a low-memory signal). Skipping super here silently cut every library-level
    // registrant off from both signals - a real risk for stale cached bitmaps after a runtime
    // density/font-scale change, or the app failing to release memory it otherwise could under
    // memory pressure on a lower-RAM device.
    override fun onConfigurationChanged(newConfig: AndroidConfiguration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    private fun trimMemoryLevelName(level: Int): String = when (level) {
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
        ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
        ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
        ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
        else -> "UNKNOWN_$level"
    }
}
