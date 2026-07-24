// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    // Declared here (never applied at this level) so :app can conditionally `apply(plugin = ...)`
    // them only when a real app/google-services.json is present - see :app's build.gradle.kts.
    // Without this, a project without Firebase configured yet would fail to build at all.
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
    alias(libs.plugins.firebase.perf.plugin) apply false
}