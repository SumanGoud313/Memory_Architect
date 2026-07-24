package com.suman.memoryarchitect.core.analytics

/**
 * Non-fatal crash/error reporting abstraction - the Crashlytics counterpart to [AnalyticsLogger].
 * Kept separate from [AnalyticsLogger] rather than folded in: crash reports and analytics events
 * are different Firebase products with different semantics (Crashlytics needs the full stack
 * trace and benefits from breadcrumbs leading up to it, not a flat event/params pair), and a
 * future non-Firebase backend for one will rarely also be the right backend for the other.
 *
 * Fatal (uncaught) exceptions are captured automatically by the Crashlytics SDK once configured -
 * nothing in this app needs to call anything for those. [recordException] is for exceptions this
 * app already caught and recovered from (see `ErrorMapper`) - genuinely unexpected failures worth
 * surfacing, not routine, expected conditions like "device is offline."
 */
interface CrashReporter {
    fun recordException(throwable: Throwable)

    /** A breadcrumb - short free-form context logged alongside whatever crash/exception report
     * follows it, not a standalone event. */
    fun log(message: String)

    fun setCustomKey(key: String, value: String)
}
