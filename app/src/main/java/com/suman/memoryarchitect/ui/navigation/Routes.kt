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

    /** Also hosts the "Unlocks" tab (the former standalone Rewards screen) - see
     * `AchievementsScreen.kt`'s doc. */
    @Serializable
    data object Achievements : Route

    /** The full Memory Journey tier showcase - reached via Profile's own big Memory Journey
     * button. See `MemoryJourneyBar.kt`'s doc for the slim Profile-embedded version. */
    @Serializable
    data object MemoryJourney : Route

    @Serializable
    data object RemoveAds : Route

    /** The cosmetics hub - Shop/Collections as tabs within one screen (Shop selected by default
     * unless [startOnCollectionsTab] is set - see `CosmeticsHubScreen.kt`), reached via the round
     * "Cosmetics" corner button on [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen]
     * or, with [startOnCollectionsTab] = true, from [LuckySpin]'s "go equip it" nudge after
     * winning a cosmetic. Shop/Collections no longer have their own routes. Lucky Spin is its own
     * route again (below) - it moved out of this tab shell onto Mode Select directly. */
    @Serializable
    data class CosmeticsHub(val startOnCollectionsTab: Boolean = false) : Route

    /** Lucky Spin - its own icon on [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen],
     * between Cosmetics and Remove Ads. Not part of [CosmeticsHub]'s tab shell. */
    @Serializable
    data object LuckySpin : Route

    /** Today's/this week's/this month's active missions - its own icon on
     * [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen], alongside Cosmetics/
     * Lucky Spin/Remove Ads/Inventory. */
    @Serializable
    data object Missions : Route

    /** The permanent home for every earned consumable (Hint/Redo/Rewatch tokens, Lucky Spin
     * tickets, XP boosts, discount coupons, mystery chests) - its own icon alongside Missions. */
    @Serializable
    data object Inventory : Route

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
