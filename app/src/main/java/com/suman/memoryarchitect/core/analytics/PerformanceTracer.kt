package com.suman.memoryarchitect.core.analytics

/**
 * Custom Performance Monitoring traces for the handful of operations Firebase's automatic
 * instrumentation doesn't already cover (app startup, screen rendering, and network requests are
 * captured automatically once the Performance Monitoring Gradle plugin is applied - see
 * FIREBASE_SETUP.md - no custom code needed for those). This covers what's left: on-device level
 * generation/scene load, which never touches the network for Practice/Classic and so is otherwise
 * invisible to Performance Monitoring.
 */
interface PerformanceTracer {
    /** Starts a named trace; call [PerformanceTrace.stop] exactly once when the measured work
     * finishes. See [trace] for the common "wrap a suspend block" case. */
    fun startTrace(name: String): PerformanceTrace
}

interface PerformanceTrace {
    fun putMetric(name: String, value: Long)
    fun stop()
}

/** Measures [block], stopping the trace whether it succeeds or throws - the common case, so call
 * sites rarely need [PerformanceTracer.startTrace]/[PerformanceTrace.stop] directly. */
suspend fun <T> PerformanceTracer.trace(name: String, block: suspend () -> T): T {
    val handle = startTrace(name)
    try {
        return block()
    } finally {
        handle.stop()
    }
}
