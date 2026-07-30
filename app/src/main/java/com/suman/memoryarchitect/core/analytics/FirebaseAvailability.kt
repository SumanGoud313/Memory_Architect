package com.suman.memoryarchitect.core.analytics

import com.suman.memoryarchitect.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "is a real Firebase project actually wired up." [BuildConfig
 * .FIREBASE_CONFIGURED] is set at build time from whether `app/google-services.json` exists (see
 * app/build.gradle.kts) - without a real file there, `FirebaseApp.initializeApp()` never succeeds
 * (no project id/API key to initialize with), so every Firebase-backed class in this package
 * checks this before touching `Firebase.*` at all, rather than letting an
 * `IllegalStateException: Default FirebaseApp is not initialized` surface at some unpredictable
 * first-use call site. See FIREBASE_SETUP.md for how to obtain that file.
 *
 * Every repository/service that gates behavior on this injects [FirebaseAvailabilityProvider]
 * instead of reading [isConfigured] here directly (see that interface's doc for why) - this
 * `object` itself is only ever read by [RealFirebaseAvailabilityProvider].
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

/**
 * Injectable seam around [FirebaseAvailability.isConfigured] - every repository/service that
 * gates behavior on Firebase's availability (dual-source repositories' `activeRemoteSource()`
 * routing, [com.suman.memoryarchitect.core.auth.PlayerIdentityManager]'s sign-in gate, the
 * Firebase-backed `core/analytics` classes' no-op checks, etc.) takes this as a constructor
 * dependency instead of reading [FirebaseAvailability.isConfigured] directly.
 *
 * [FirebaseAvailability] itself stays a plain `object` backed by a build-time [BuildConfig]
 * constant - fine for production, where it only ever runs against the real, current build
 * variant - but that same shape makes a *JVM unit test* unable to force either branch on demand:
 * whether this machine happens to have a real `app/google-services.json` checked out locally (see
 * FIREBASE_SETUP.md) would otherwise silently decide which branch a test exercises, rather than
 * the test itself. Routing every call site through this interface lets a test inject a fake that
 * deterministically forces either path, independent of the local machine's/CI runner's Firebase
 * setup - see `RemoteConfigRepositoryImplTest`'s `FakeFirebaseAvailabilityProvider` for the pattern.
 *
 * Production wiring (`di/AnalyticsModule.kt`) binds this to [RealFirebaseAvailabilityProvider],
 * which just delegates to [FirebaseAvailability.isConfigured] - so this is purely a testability
 * seam, never a behavior change.
 */
interface FirebaseAvailabilityProvider {
    val isConfigured: Boolean
}

@Singleton
class RealFirebaseAvailabilityProvider @Inject constructor() : FirebaseAvailabilityProvider {
    override val isConfigured: Boolean get() = FirebaseAvailability.isConfigured
}
