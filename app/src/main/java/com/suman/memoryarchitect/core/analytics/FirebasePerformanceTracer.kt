package com.suman.memoryarchitect.core.analytics

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.perf.performance
import javax.inject.Inject
import javax.inject.Singleton

/** Real Firebase Performance Monitoring-backed [PerformanceTracer]. Same no-op-when-unconfigured
 * shape as the other Firebase-backed classes in this package. Trace start/stop are lightweight,
 * synchronous SDK calls (unlike event logging, there's no network I/O to push to a background
 * thread), so this doesn't need [FirebaseAnalyticsLogger]'s coroutine dispatch. */
@Singleton
class FirebasePerformanceTracer @Inject constructor() : PerformanceTracer {
    override fun startTrace(name: String): PerformanceTrace {
        if (!FirebaseAvailability.isConfigured) return NoopPerformanceTrace
        return try {
            val trace = Firebase.performance.newTrace(name)
            trace.start()
            FirebaseTraceHandle(trace)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to start trace $name", t)
            NoopPerformanceTrace
        }
    }

    private class FirebaseTraceHandle(private val trace: com.google.firebase.perf.metrics.Trace) : PerformanceTrace {
        override fun putMetric(name: String, value: Long) {
            try {
                trace.putMetric(name, value)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to put metric $name", t)
            }
        }

        override fun stop() {
            try {
                trace.stop()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop trace", t)
            }
        }
    }

    private object NoopPerformanceTrace : PerformanceTrace {
        override fun putMetric(name: String, value: Long) = Unit
        override fun stop() = Unit
    }

    private companion object {
        const val TAG = "FirebasePerformance"
    }
}
