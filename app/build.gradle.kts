import java.io.IOException
import java.net.Socket

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase (Analytics/Crashlytics/Performance) needs a real app/google-services.json from a
// Firebase console project to do anything useful, and the google-services plugin hard-fails the
// build if that file is missing. Applying these plugins conditionally means: no file yet -> the
// project builds and runs exactly as before, Firebase simply stays inert; drop in a real
// google-services.json -> these light up automatically on the next build, no other code changes
// needed. See FIREBASE_SETUP.md for how to get that file.
val hasFirebaseConfig = file("google-services.json").exists()

// The Performance Monitoring Gradle plugin (com.google.firebase.firebase-perf) does not yet
// support Android Gradle Plugin 9.x as of this writing - it depends on the removed
// com.android.build.api.transform.Transform API and fails to apply at all
// (https://github.com/firebase/firebase-android-sdk/issues/7293, open upstream). This is not
// something a version bump here fixes; there is no released version of the plugin that supports
// AGP 9 yet. Left disabled until Firebase ships one - re-enabling later is this one flag.
//
// This does NOT remove Performance Monitoring entirely: the firebase-perf SDK dependency below
// still works without its Gradle plugin for manually-started custom traces (see
// core/analytics/FirebasePerformanceTracer.kt) - only the plugin's automatic bytecode-woven
// screen-rendering/network-request instrumentation is unavailable until this is re-enabled.
val enableFirebasePerfPlugin = false

if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    if (enableFirebasePerfPlugin) {
        apply(plugin = "com.google.firebase.firebase-perf")
    }
}

android {
    namespace = "com.suman.memoryarchitect"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.suman.memoryarchitect"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Lets runtime code (see core/analytics/FirebaseAvailability.kt) know at compile time
        // whether a real Firebase project is wired up, instead of probing FirebaseApp state or
        // catching init exceptions - both build variants (debug/release) build cleanly either way.
        buildConfigField("boolean", "FIREBASE_CONFIGURED", hasFirebaseConfig.toString())
        buildConfigField("boolean", "FIREBASE_PERF_PLUGIN_APPLIED", enableFirebasePerfPlugin.toString())

        // The OAuth Web Client ID Firebase generates once Authentication -> Sign-in method ->
        // Google is enabled in the console (see LEADERBOARD_SETUP.md's "Google Sign-In upgrade"
        // section) - NOT an Android client ID, the Web one, since that's what Credential Manager's
        // GetGoogleIdOption needs to mint an ID token Firebase can verify. A blank value makes
        // AccountViewModel treat Google Sign-In as "not yet configured" and hide the button
        // entirely rather than launching a request that would only fail - Web Client IDs are not
        // secret (unlike client *secrets*), so committing a real one here is fine.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"955650453441-90dregg2t6430cb5jgt21kebh56bk28u.apps.googleusercontent.com\"")
    }

    buildTypes {
        debug {
            // Dev-only mock backend (mock-backend/), run locally on this machine's LAN IP
            // so a physical test device on the same Wi-Fi can reach it. Emulators should
            // use http://10.0.2.2:4000/ instead (the emulator's host-loopback alias).
            // This IP is only as good as the dev machine's current DHCP lease - if it ever
            // changes (new network, router reboot reassigning it), re-check with
            // `Get-NetIPAddress -AddressFamily IPv4` and update this. The whole session's
            // "physical device can't reach the mock backend" saga turned out to be exactly
            // this: the address here (192.168.1.11) didn't match the machine's actual Wi-Fi
            // IP (192.168.29.120) at all - not a firewall problem, wrong address entirely.
            buildConfigField("String", "BASE_URL", "\"http://192.168.29.120:4000/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://api.memoryarchitect.example.com/\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    sourceSets {
        // Lets AppDatabaseMigrationTest's MigrationTestHelper load the exported per-version schema
        // JSON (ksp's "room.schemaLocation" output below) as a test asset, the standard Room
        // migration-testing wiring - without this, MigrationTestHelper.createDatabase() can't find
        // the old-version schema it seeds a pre-migration database from.
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Dev convenience: the debug build talks to mock-backend/ (see BASE_URL above). Make sure
// it's actually running before we build/install a debug APK, instead of failing later at
// runtime with "Couldn't reach the server." Never touches release builds.
val mockBackendDir = rootProject.file("mock-backend")
val mockBackendPort = 4000

val startMockBackend by tasks.registering {
    group = "memory architect"
    description = "Starts mock-backend/ (dev-only Node server) on port 4000 if it isn't already running."
    notCompatibleWithConfigurationCache("launches an external process and probes a live port, so its outcome can't be cached")

    doLast {
        val port = mockBackendPort
        fun portIsOpen(): Boolean = try {
            Socket("127.0.0.1", port).close(); true
        } catch (e: IOException) {
            false
        }

        if (portIsOpen()) {
            logger.lifecycle("Mock backend already running on port $port — skipping.")
            return@doLast
        }

        if (!mockBackendDir.resolve("index.js").exists()) {
            logger.warn("mock-backend/index.js not found — skipping mock backend startup.")
            return@doLast
        }

        logger.lifecycle("Starting mock backend (mock-backend/) on port $port...")
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val command = if (isWindows) {
                listOf("cmd", "/c", "start", "\"mock-backend\"", "/b", "node", "index.js")
            } else {
                listOf("node", "index.js")
            }
            ProcessBuilder(command)
                .directory(mockBackendDir)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(mockBackendDir.resolve("server.log")))
                .redirectErrorStream(true)
                .start()
        } catch (e: IOException) {
            logger.warn("Couldn't launch mock backend (is Node.js installed and on PATH?): ${e.message}")
            return@doLast
        }

        // Give it a moment to come up and confirm, but never fail the build over it.
        var ready = false
        repeat(20) {
            if (!ready) {
                Thread.sleep(250)
                ready = portIsOpen()
            }
        }
        if (ready) {
            logger.lifecycle("Mock backend is up on port $port.")
        } else {
            logger.warn("Mock backend didn't respond on port $port within 5s — check mock-backend/server.log")
        }
    }
}

tasks.whenTaskAdded {
    if (name == "assembleDebug" || name == "installDebug") {
        dependsOn(startMockBackend)
    }
}

// Security gate: core.billing.PurchaseSignatureVerifier.BASE64_PUBLIC_KEY ships blank in source
// control (a per-developer-account Play Console value nobody else can supply) - this task fails a
// release build outright while it stays blank, so local purchase verification can never silently
// ship disabled. Debug builds are unaffected (see that class's own NotConfigured handling).
val checkBillingPublicKeyConfigured by tasks.registering {
    group = "memory architect"
    description = "Fails the build if PurchaseSignatureVerifier's BASE64_PUBLIC_KEY is still blank."
    notCompatibleWithConfigurationCache("reads a source file's live text at execution time, not a cacheable input/output relationship")

    doLast {
        val verifierFile = file("src/main/java/com/suman/memoryarchitect/core/billing/PurchaseSignatureVerifier.kt")
        check(verifierFile.exists()) { "PurchaseSignatureVerifier.kt not found at the expected path - has it moved? Update this task's path if so." }
        val text = verifierFile.readText()
        val keyValue = Regex("""BASE64_PUBLIC_KEY\s*=\s*"([^"]*)"""").find(text)?.groupValues?.get(1)
            ?: error("Couldn't find BASE64_PUBLIC_KEY in PurchaseSignatureVerifier.kt - has its declaration changed shape?")
        check(keyValue.isNotBlank()) {
            "PurchaseSignatureVerifier.BASE64_PUBLIC_KEY is still blank - a release build must not ship " +
                "without local purchase-signature verification configured. Paste the Base64-encoded RSA " +
                "public key from Play Console > [app] > Monetize > Monetization setup > Licensing into that " +
                "constant before building a release."
        }
    }
}

tasks.whenTaskAdded {
    if (name == "assembleRelease" || name == "bundleRelease") {
        dependsOn(checkBillingPublicKeyConfigured)
    }
}

dependencies {
    // AndroidX / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.material)
    implementation(libs.androidx.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.process)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Local notifications (core/notifications/) - streak/daily-challenge reminders, scheduled via
    // WorkManager so they survive process death and don't need the app in the foreground.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Local cache
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Ads
    implementation(libs.play.services.ads)

    // Billing (core/billing/ - lifetime "Remove Ads" one-time purchase)
    implementation(libs.billing.ktx)

    // Audio (asset-based music/SFX playback - core/feedback/audio/)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)

    // Firebase (Analytics, Crashlytics, Performance Monitoring, Remote Config). The SDKs compile
    // and link fine even with no google-services.json present - only the Gradle plugins above
    // require it. Runtime code must still guard every call behind FirebaseAvailability (see
    // core/analytics/) since FirebaseApp itself never initializes without that file.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)
    // Anonymous Auth (competitive-progression player identity) + Firestore (leaderboards) - see
    // core/auth/PlayerIdentityManager.kt and core/leaderboard/. Both guarded by
    // FirebaseAvailability.isConfigured exactly like every other Firebase-backed class in this
    // app, so they're inert (never crash) if google-services.json is ever absent, and functionally
    // no-op (leaderboard reads/writes fail soft to Outcome.Error) until Firestore Database +
    // Anonymous sign-in are enabled in the Firebase console for this project - see LEADERBOARD_SETUP.md.
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    // Custom avatar upload (Settings' Account section) - see storage.rules at the project root and
    // data/repository/AvatarUploadRepository.kt. Same FirebaseAvailability-guarded, fails-soft
    // posture as Auth/Firestore above.
    implementation(libs.firebase.storage)

    // Google Sign-In upgrade path (core/auth/PlayerIdentityManager.linkWithGoogle) - the modern
    // Credential Manager API, not the deprecated GoogleSignInClient. See
    // feature/settings/AccountViewModel.kt for where the ID token this obtains gets consumed.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Play Integrity API (core/security/DeviceIntegrityChecker.kt) - soft/advisory device
    // attestation signal only; see that class's doc for why it's never a hard gate in this app.
    implementation(libs.play.integrity)

    // Remote image loading - the linked Google account's own profile photo (Account section) and
    // a custom-uploaded avatar (ui/screens/profile/AccountSection.kt) are both just a URL until
    // something actually fetches/decodes/caches them.
    implementation(libs.coil.compose)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // Room migration tests (core/database/ - see AppDatabaseMigrationTest) - needs a real device/
    // emulator's SQLite, same as every other Room-backed androidTest here already does.
    androidTestImplementation(libs.androidx.room.testing)
    // Compose UI tests (ui/screens/gameplay/ - see HintButtonTest) - first use of this in the
    // project; needs a real device/emulator's Compose runtime, same as the Room migration tests
    // above.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}