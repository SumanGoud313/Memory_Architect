package com.suman.memoryarchitect.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real Firebase Analytics-backed [AnalyticsLogger]. Every call is a no-op when
 * [FirebaseAvailabilityProvider.isConfigured] is false (no `google-services.json` yet - see
 * FIREBASE_SETUP.md) rather than throwing, so the rest of the app never needs to know or care
 * whether a real Firebase project is wired up.
 *
 * Dispatches onto its own background scope so a slow or hung Firebase call can never block the
 * caller (typically a ViewModel on the main thread mid-gameplay) - gameplay must never wait on
 * analytics. Wrapped in try/catch for the same reason: a malformed event should degrade to "not
 * logged," never a crash.
 */
@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : AnalyticsLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        if (firebaseAvailabilityProvider.isConfigured) Firebase.analytics else null
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        if (!firebaseAvailabilityProvider.isConfigured) return
        scope.launch {
            try {
                firebaseAnalytics?.logEvent(name, params.toBundle())
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to log event $name", t)
            }
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        if (!firebaseAvailabilityProvider.isConfigured) return
        scope.launch {
            try {
                firebaseAnalytics?.setUserProperty(name, value)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to set user property $name", t)
            }
        }
    }

    private fun Map<String, Any?>.toBundle(): Bundle {
        val bundle = Bundle()
        forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value.take(FIREBASE_STRING_PARAM_MAX_LENGTH))
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString().take(FIREBASE_STRING_PARAM_MAX_LENGTH))
            }
        }
        return bundle
    }

    private companion object {
        const val TAG = "FirebaseAnalytics"
        // Firebase Analytics truncates string param values past 100 characters server-side;
        // trimming client-side avoids silently losing the tail of a longer value unexpectedly.
        const val FIREBASE_STRING_PARAM_MAX_LENGTH = 100
    }
}
