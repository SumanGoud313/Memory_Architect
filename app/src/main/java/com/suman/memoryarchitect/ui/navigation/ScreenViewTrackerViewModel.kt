package com.suman.memoryarchitect.ui.navigation

import androidx.lifecycle.ViewModel
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logScreenView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Compose Navigation has no Fragment/Activity boundary per screen for Firebase's automatic
 * screen_view tracking to attach to - this whole app is one Activity - so each
 * `composable<Route.X> { }` block in [MemoryArchitectNavHost] calls [trackScreen] once (via
 * `LaunchedEffect(Unit)`) to log it manually. This tiny ViewModel exists solely so that call goes
 * through a ViewModel rather than a Composable calling [AnalyticsLogger] directly, per this
 * project's "Composables never call analytics directly" rule - it holds no other state.
 */
@HiltViewModel
class ScreenViewTrackerViewModel @Inject constructor(
    private val analytics: AnalyticsLogger,
) : ViewModel() {
    private var lastTracked: String? = null

    /** Guards against re-logging the same screen on a recomposition-triggered re-run of the same
     * `LaunchedEffect(Unit)` (shouldn't normally happen given the `Unit` key, but a ViewModel
     * surviving configuration changes while its host recomposes is exactly the scenario worth
     * being defensive about for something whose only job is "log this exactly once per visit"). */
    fun trackScreen(screenName: String) {
        if (lastTracked == screenName) return
        lastTracked = screenName
        analytics.logScreenView(screenName)
    }
}
