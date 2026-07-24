package com.suman.memoryarchitect

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.AppLifecycleTracker
import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.logMemoryWarning
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.PremiumShopManager
import com.suman.memoryarchitect.core.security.DeviceIntegrityChecker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MemoryArchitectApp : Application(), ComponentCallbacks2 {

    @Inject lateinit var appLifecycleTracker: AppLifecycleTracker

    @Inject lateinit var crashReporter: CrashReporter

    @Inject lateinit var analytics: AnalyticsLogger

    @Inject lateinit var playerIdentityManager: PlayerIdentityManager

    @Inject lateinit var deviceIntegrityChecker: DeviceIntegrityChecker

    @Inject lateinit var billingManager: BillingManager

    @Inject lateinit var premiumShopManager: PremiumShopManager

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
        // Connects to Play Billing and re-verifies the remove_ads_lifetime entitlement against the
        // signed-in Google Play account - this, not any explicit "Restore" tap, is what actually
        // satisfies "restore automatically on reinstall/new device" (see BillingManager's doc).
        billingManager.startConnection()
        // Same "connect + re-verify existing purchases" role as billingManager.startConnection()
        // above, for the 7 premium cosmetic bundles instead of remove_ads_lifetime - shares the
        // same underlying BillingClient (see SharedBillingClient's doc), so this is a second
        // logical connection, not a second real one.
        premiumShopManager.startConnection()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        analytics.logMemoryWarning(trimMemoryLevelName(level))
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun onLowMemory() = Unit

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
