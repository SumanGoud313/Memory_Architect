package com.suman.memoryarchitect.core.analytics

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real Firebase Crashlytics-backed [CrashReporter]. Same "no-op, never throw, never block" shape
 * as [FirebaseAnalyticsLogger] - see that class for the full reasoning ([FirebaseAvailability],
 * background dispatch, try/catch).
 */
@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val crashlytics: FirebaseCrashlytics? by lazy {
        if (FirebaseAvailability.isConfigured) Firebase.crashlytics else null
    }

    override fun recordException(throwable: Throwable) {
        if (!FirebaseAvailability.isConfigured) return
        scope.launch {
            try {
                crashlytics?.recordException(throwable)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to record exception", t)
            }
        }
    }

    override fun log(message: String) {
        if (!FirebaseAvailability.isConfigured) return
        scope.launch {
            try {
                crashlytics?.log(message)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to log breadcrumb", t)
            }
        }
    }

    override fun setCustomKey(key: String, value: String) {
        if (!FirebaseAvailability.isConfigured) return
        scope.launch {
            try {
                crashlytics?.setCustomKey(key, value)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to set custom key $key", t)
            }
        }
    }

    private companion object {
        const val TAG = "FirebaseCrashlytics"
    }
}
