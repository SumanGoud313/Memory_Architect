package com.suman.memoryarchitect.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation-Compose routes — replaces `res/navigation/nav_graph.xml`. */
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object ModeSelect : Route

    @Serializable
    data object LevelSelect : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Statistics : Route

    @Serializable
    data object Leaderboard : Route

    @Serializable
    data object Achievements : Route

    @Serializable
    data object Rewards : Route

    @Serializable
    data object RemoveAds : Route

    @Serializable
    data object Shop : Route

    @Serializable
    data object Collections : Route

    @Serializable
    data object LuckySpin : Route

    /** The cosmetics hub - Showcase + Shop/Collections/Lucky Spin entry points, reached via the
     * round "Cosmetics" corner button on [RemoveAds] (moved here from Profile). */
    @Serializable
    data object CosmeticsHub : Route

    /** Debug-build-only - see `SettingsScreen`'s `BuildConfig.DEBUG`-gated entry point, the only
     * place this is ever navigated to from. */
    @Serializable
    data object AnalyticsDashboard : Route

    @Serializable
    data class Gameplay(
        val mode: String,
        val difficulty: String = "MEDIUM",
        val levelNumber: Int = 1,
    ) : Route
}
