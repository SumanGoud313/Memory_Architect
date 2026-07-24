package com.suman.memoryarchitect.core.network

import android.os.Build
import java.net.URI

/**
 * Debug-only convenience. `BuildConfig.BASE_URL` is checked in as the developer machine's current
 * real LAN IP (see `app/build.gradle.kts`'s own comment on that field) so a physical test device on
 * the same Wi-Fi can reach the local mock backend directly. Android emulators cannot reach that
 * address at all under their default NAT networking - only the host's *loopback* is reachable, via
 * the fixed alias `10.0.2.2` - so a build config value tuned for a physical device always times out
 * from an AVD (`SocketTimeoutException: failed to connect to /<lan-ip>:4000 ... after 3000ms`,
 * surfacing in the app as "Couldn't reach the server") regardless of whether the mock backend
 * itself is healthy.
 *
 * Rather than requiring hand-editing `BASE_URL` every time development switches between a physical
 * device and an emulator, [resolve] detects "is this process running inside an emulator" at
 * runtime (there's no official API for this - [isRunningOnEmulator] uses the standard, widely-used
 * `Build.*` heuristic) and swaps the host to `10.0.2.2`, preserving the configured value's own
 * scheme/port/path exactly. A physical device never trips the detection, so its behavior is
 * unchanged.
 */
object EmulatorBaseUrlResolver {

    fun resolve(configuredBaseUrl: String): String {
        if (!isRunningOnEmulator()) return configuredBaseUrl
        return resolveForEmulator(configuredBaseUrl)
    }

    /** The pure, host-rewriting half of [resolve] - split out so it's unit-testable without
     * touching `android.os.Build` (unpredictable in a plain JVM unit test; this project's test
     * setup has no Robolectric). Falls back to the original value unchanged if it isn't a parseable
     * URL, rather than throwing - a malformed BASE_URL should fail exactly the same way whether or
     * not this rewrite exists, never differently because of it. */
    fun resolveForEmulator(configuredBaseUrl: String): String =
        runCatching { configuredBaseUrl.withHost(EMULATOR_HOST_LOOPBACK_ALIAS) }.getOrDefault(configuredBaseUrl)

    private fun isRunningOnEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT == "google_sdk" ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")

    private fun String.withHost(newHost: String): String {
        val uri = URI(this)
        return URI(uri.scheme, uri.userInfo, newHost, uri.port, uri.path, uri.query, uri.fragment).toString()
    }

    private const val EMULATOR_HOST_LOOPBACK_ALIAS = "10.0.2.2"
}
