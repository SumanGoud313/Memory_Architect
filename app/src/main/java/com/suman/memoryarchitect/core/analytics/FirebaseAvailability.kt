package com.suman.memoryarchitect.core.analytics

import com.suman.memoryarchitect.BuildConfig

/**
 * Single source of truth for "is a real Firebase project actually wired up." [BuildConfig
 * .FIREBASE_CONFIGURED] is set at build time from whether `app/google-services.json` exists (see
 * app/build.gradle.kts) - without a real file there, `FirebaseApp.initializeApp()` never succeeds
 * (no project id/API key to initialize with), so every Firebase-backed class in this package
 * checks this before touching `Firebase.*` at all, rather than letting an
 * `IllegalStateException: Default FirebaseApp is not initialized` surface at some unpredictable
 * first-use call site. See FIREBASE_SETUP.md for how to obtain that file.
 */
object FirebaseAvailability {
    val isConfigured: Boolean = BuildConfig.FIREBASE_CONFIGURED

    /** True only when the Performance Monitoring Gradle plugin's automatic bytecode-woven
     * instrumentation (screen rendering, network requests) is active - independent of
     * [isConfigured], since that plugin doesn't yet support AGP 9 (see FIREBASE_SETUP.md) and is
     * disabled separately in app/build.gradle.kts regardless of whether Firebase itself is
     * configured. Custom traces (see [PerformanceTracer]) work whenever [isConfigured] is true,
     * with or without this. */
    val isPerformancePluginActive: Boolean = BuildConfig.FIREBASE_PERF_PLUGIN_APPLIED
}
