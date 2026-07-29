package com.suman.memoryarchitect.feature.analyticsdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AppLifecycleTracker
import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.analytics.RecentEventsRecorder
import com.suman.memoryarchitect.core.analytics.RecordedEvent
import com.google.firebase.Firebase
import com.google.firebase.installations.installations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AnalyticsDashboardUiState(
    val firebaseInstallationId: String = "Loading…",
    val firebaseConfigured: Boolean = FirebaseAvailability.isConfigured,
    val performancePluginActive: Boolean = FirebaseAvailability.isPerformancePluginActive,
    val recentEvents: List<RecordedEvent> = emptyList(),
    val userProperties: Map<String, String?> = emptyMap(),
    val currentLevelNumber: Int? = null,
    val currentMode: String? = null,
    /** `null` if the app hasn't recorded a foreground transition yet this process - the
     * Composable computes a live-ticking duration from this rather than the ViewModel, since a
     * plain [StateFlow] has no natural way to "tick" on its own without polling. */
    val sessionStartedAtMs: Long? = null,
)

/**
 * Backs the debug-only Analytics Dashboard (see `feature/analyticsdashboard/` and
 * `SettingsScreen`'s debug-gated entry point). Never referenced from release-build UI - see
 * `SettingsScreen` for the `BuildConfig.DEBUG` gate - so this ViewModel existing in the release
 * binary at all costs nothing at runtime; it's just unreachable code, not a security concern
 * (nothing here is sensitive - it only surfaces what this same device already sent to Firebase).
 */
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    recentEvents: RecentEventsRecorder,
    appLifecycleTracker: AppLifecycleTracker,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : ViewModel() {

    private val firebaseInstallationId = MutableStateFlow("Loading…")

    val uiState: StateFlow<AnalyticsDashboardUiState> = combine(
        recentEvents.events,
        recentEvents.userProperties,
        firebaseInstallationId,
    ) { events, properties, installationId ->
        val lastLevelEvent = events.firstOrNull { it.name == "level_started" || it.name == "memorize_started" }
        AnalyticsDashboardUiState(
            firebaseInstallationId = installationId,
            firebaseConfigured = firebaseAvailabilityProvider.isConfigured,
            performancePluginActive = FirebaseAvailability.isPerformancePluginActive,
            recentEvents = events,
            userProperties = properties,
            currentLevelNumber = lastLevelEvent?.params?.get("level_number") as? Int,
            currentMode = lastLevelEvent?.params?.get("mode") as? String,
            sessionStartedAtMs = appLifecycleTracker.foregroundedAtMs,
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), AnalyticsDashboardUiState())

    init {
        viewModelScope.launch {
            firebaseInstallationId.value = if (firebaseAvailabilityProvider.isConfigured) {
                runCatching { Firebase.installations.id.await() }.getOrDefault("Unavailable")
            } else {
                "Not configured - see FIREBASE_SETUP.md"
            }
        }
    }
}
