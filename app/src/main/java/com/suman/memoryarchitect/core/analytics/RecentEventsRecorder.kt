package com.suman.memoryarchitect.core.analytics

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecordedEvent(val name: String, val params: Map<String, Any?>, val loggedAt: Instant)

/**
 * In-memory ring buffer of the last [MAX_EVENTS] analytics events - backs the debug-only Analytics
 * Dashboard's "Last 50 Analytics Events" panel (see `feature/analyticsdashboard/`). Deliberately
 * process-lifetime only, not persisted: this is a live developer tool, not a data store. Wired
 * into [CompositeAnalyticsLogger] unconditionally (debug and release both record into it) since
 * holding 50 small entries in memory is negligible - only the *screen* that reads it is
 * debug-gated.
 */
@Singleton
class RecentEventsRecorder @Inject constructor() {
    private val _events = MutableStateFlow<List<RecordedEvent>>(emptyList())
    val events: StateFlow<List<RecordedEvent>> = _events.asStateFlow()

    // Firebase Analytics user properties are write-only from the client's own SDK - there's no
    // API to read back what was previously set (only the Firebase console/BigQuery can see that).
    // This is the debug dashboard's only way to show "current" properties: what this process has
    // itself set them to, which is the same thing on a live device but won't reflect properties
    // set in a previous app run.
    private val _userProperties = MutableStateFlow<Map<String, String?>>(emptyMap())
    val userProperties: StateFlow<Map<String, String?>> = _userProperties.asStateFlow()

    fun record(name: String, params: Map<String, Any?>) {
        _events.value = (listOf(RecordedEvent(name, params, Instant.now())) + _events.value).take(MAX_EVENTS)
    }

    fun recordUserProperty(name: String, value: String?) {
        _userProperties.value = _userProperties.value + (name to value)
    }

    private companion object {
        const val MAX_EVENTS = 50
    }
}
