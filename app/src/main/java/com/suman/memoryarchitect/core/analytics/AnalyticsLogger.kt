package com.suman.memoryarchitect.core.analytics

/**
 * The reusable analytics abstraction every ViewModel/repository logs through — never Firebase (or
 * any other backend) directly, and never from a Composable. Swapping or adding a concrete backend
 * (Firebase Analytics, Amplitude, a first-party endpoint) is a change to the bound implementation
 * only (see `AnalyticsModule`), not to any call site. Call sites never build [logEvent]'s `name`/
 * `params` by hand either — see the typed logging functions in `AnalyticsEvents.kt`, which exist
 * specifically so an event can't be logged with a typo'd name or an inconsistent param shape from
 * two different call sites.
 *
 * Implementations must never block the calling thread — gameplay must never wait on analytics.
 */
interface AnalyticsLogger {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    /** User properties are a separate Firebase concept from events - persistent attributes of the
     * *player* ("highest_level" = 42), not something that happened at a point in time. [value]
     * `null` clears a previously-set property. Firebase caps property values at 36 characters and
     * property names at 24 - callers should keep both short (see `AnalyticsEvents.kt`'s typed
     * setters, which already respect this). */
    fun setUserProperty(name: String, value: String?)
}
