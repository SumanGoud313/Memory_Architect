package com.suman.memoryarchitect.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.core.cosmetics.RootCosmeticsViewModel
import com.suman.memoryarchitect.core.feedback.audio.MusicTrack
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics
import com.suman.memoryarchitect.ui.screens.analyticsdashboard.AnalyticsDashboardScreen
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.feature.modeselect.ModeSelectViewModel
import com.suman.memoryarchitect.ui.screens.gameplay.GameplayScreen
import com.suman.memoryarchitect.ui.screens.home.HomeScreen
import com.suman.memoryarchitect.ui.screens.leaderboard.LeaderboardScreen
import com.suman.memoryarchitect.ui.screens.levelselect.LevelSelectScreen
import com.suman.memoryarchitect.ui.screens.missions.MissionsScreen
import com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen
import com.suman.memoryarchitect.ui.screens.profile.AchievementsScreen
import com.suman.memoryarchitect.ui.screens.profile.MemoryJourneyScreen
import com.suman.memoryarchitect.ui.screens.profile.ProfileScreen
import com.suman.memoryarchitect.ui.screens.profile.RewardsScreen
import com.suman.memoryarchitect.ui.screens.removeads.RemoveAdsScreen
import com.suman.memoryarchitect.ui.screens.settings.SettingsScreen
import com.suman.memoryarchitect.ui.screens.shop.CosmeticsHubScreen
import com.suman.memoryarchitect.ui.screens.shop.InventoryScreen
import com.suman.memoryarchitect.ui.screens.shop.LuckySpinScreen
import com.suman.memoryarchitect.ui.screens.statistics.StatisticsScreen

/** Compose Navigation graph — replaces `res/navigation/nav_graph.xml`. This composable is
 * permanently mounted directly under [com.suman.memoryarchitect.ui.ConnectivityGate] (itself
 * mounted directly in `MainActivity.setContent`), never inside any one `composable<Route> { }`
 * destination - so [LocalLifecycleOwner] here resolves to the real hosting Activity, not a
 * per-destination `NavBackStackEntry`. That's what makes [MusicPauseOnBackground] below a true
 * whole-app background/foreground signal (fires once per real app minimize/restore, regardless of
 * which screen is showing) rather than something that would also fire on ordinary in-app
 * navigation. The same "mounted for the whole app session" property is why
 * [com.suman.memoryarchitect.core.cosmetics.RootCosmeticsViewModel] is hosted here too - it feeds
 * [LocalEquippedCosmetics], reachable by every destination below without per-screen wiring. */
@Composable
fun MemoryArchitectNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    MusicPauseOnBackground()
    val rootCosmeticsViewModel: RootCosmeticsViewModel = hiltViewModel()
    val equippedCosmetics by rootCosmeticsViewModel.equipped.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalEquippedCosmetics provides equippedCosmetics) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier,
        enterTransition = NavAnimations.slideEnter,
        exitTransition = NavAnimations.slideExit,
        popEnterTransition = NavAnimations.slidePopEnter,
        popExitTransition = NavAnimations.slidePopExit,
    ) {
        composable<Route.Home> {
            TrackScreenView("home", MusicTrack.HOME)
            HomeScreen(
                onPlay = { navController.navigate(Route.ModeSelect) },
                onProfile = { navController.navigate(Route.Profile) },
                onSettings = { navController.navigate(Route.Settings) },
            )
        }
        composable<Route.ModeSelect>(
            // Same scale+fade "zoom in" treatment as entering Gameplay — tapping the Home
            // hero circle should feel like growing into the next screen, not a flat slide.
            enterTransition = NavAnimations.gameplayEnter,
            exitTransition = NavAnimations.gameplayExit,
        ) {
            TrackScreenView("mode_select", MusicTrack.HOME)
            val modeSelectViewModel: ModeSelectViewModel = hiltViewModel()
            ModeSelectScreen(
                onClassicSelected = {
                    modeSelectViewModel.onModeSelected(GameMode.CLASSIC)
                    navController.navigate(Route.LevelSelect)
                },
                onOtherModeSelected = { mode, tier ->
                    modeSelectViewModel.onModeSelected(mode)
                    navController.navigate(Route.Gameplay(mode = mode.name, difficulty = tier.name))
                },
                onOpenRemoveAds = { navController.navigate(Route.RemoveAds) },
                onOpenCosmetics = { navController.navigate(Route.CosmeticsHub) },
                onOpenLuckySpin = { navController.navigate(Route.LuckySpin) },
                onOpenMissions = { navController.navigate(Route.Missions) },
                onOpenInventory = { navController.navigate(Route.Inventory) },
            )
        }
        composable<Route.LevelSelect> {
            TrackScreenView("level_select", MusicTrack.HOME)
            LevelSelectScreen(
                onLevelSelected = { levelNumber ->
                    navController.navigate(Route.Gameplay(mode = GameMode.CLASSIC.name, levelNumber = levelNumber))
                },
            )
        }
        composable<Route.Profile>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("profile", MusicTrack.HOME)
            ProfileScreen(
                onOpenStatistics = { navController.navigate(Route.Statistics) },
                onOpenLeaderboard = { navController.navigate(Route.Leaderboard) },
                onOpenAchievements = { navController.navigate(Route.Achievements) },
                onOpenRewards = { navController.navigate(Route.Rewards) },
                onOpenMemoryJourney = { navController.navigate(Route.MemoryJourney) },
            )
        }
        composable<Route.Statistics>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("statistics", MusicTrack.HOME)
            StatisticsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Leaderboard>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("leaderboard", MusicTrack.HOME)
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Achievements>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("achievements", MusicTrack.HOME)
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Rewards>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("rewards", MusicTrack.HOME)
            RewardsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.MemoryJourney>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("memory_journey", MusicTrack.HOME)
            MemoryJourneyScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Settings>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            // Every non-gameplay screen shares MusicTrack.HOME on purpose - see MusicTrack's doc.
            TrackScreenView("settings", MusicTrack.HOME)
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAnalyticsDashboard = { navController.navigate(Route.AnalyticsDashboard) },
            )
        }
        composable<Route.RemoveAds>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("remove_ads", MusicTrack.HOME)
            RemoveAdsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.CosmeticsHub>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("cosmetics_hub", MusicTrack.HOME)
            CosmeticsHubScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.LuckySpin>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("lucky_spin", MusicTrack.HOME)
            LuckySpinScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Missions>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("missions", MusicTrack.HOME)
            MissionsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Inventory>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
            TrackScreenView("inventory", MusicTrack.HOME)
            InventoryScreen(onBack = { navController.popBackStack() })
        }
        if (BuildConfig.DEBUG) {
            composable<Route.AnalyticsDashboard>(enterTransition = NavAnimations.fadeEnter, exitTransition = NavAnimations.fadeExit) {
                TrackScreenView("analytics_dashboard", MusicTrack.UNCHANGED)
                AnalyticsDashboardScreen(onBack = { navController.popBackStack() })
            }
        }
        composable<Route.Gameplay>(
            enterTransition = NavAnimations.gameplayEnter,
            exitTransition = NavAnimations.gameplayExit,
        ) {
            // UNCHANGED here too - GameplayViewModel picks the right per-mode Memorize/Reconstruct
            // track itself (see GameModeMusic.kt) the moment each phase actually starts, which
            // happens a beat after this composable enters; setting a track here first would just
            // be immediately overridden.
            TrackScreenView("gameplay", MusicTrack.UNCHANGED)
            GameplayScreen(
                onBack = { navController.popBackStack() },
                onReplayLevel = { mode, levelNumber ->
                    navController.navigate(Route.Gameplay(mode = mode.name, levelNumber = levelNumber)) {
                        popUpTo(Route.Gameplay::class) { inclusive = true }
                    }
                },
            )
        }
    }
    }
}

/** See [ScreenViewTrackerViewModel] - the actual analytics call lives there, never here; this
 * just triggers it once per genuine visit to this destination, alongside the matching screen-open
 * feedback cue (soft chime + this screen's ambient music bed, see [com.suman.memoryarchitect.core.feedback.FeedbackManager.onScreenOpen]).
 *
 * Gated on this destination's own `NavBackStackEntry` reaching `ON_RESUME` - deliberately not a
 * plain `LaunchedEffect(Unit)`, which would fire the instant this composable enters composition.
 * With predictive back (or mid-flight while an enter transition is still animating), a destination
 * can be composed - and this composable would run - before the player has actually committed to
 * navigating here: a back-gesture preview shows the destination underneath without confirming the
 * navigation, and lets go/cancels back to the original screen. A back-stack entry only reaches
 * `RESUMED` once it's genuinely the settled, active top of the stack, so this can't fire for a
 * preview that gets cancelled - the exact "music changes before the screen actually has" bug this
 * fixes. */
@Composable
private fun TrackScreenView(screenName: String, musicTrack: MusicTrack) {
    val tracker: ScreenViewTrackerViewModel = hiltViewModel()
    val feedback = rememberFeedback()
    val currentTracker by rememberUpdatedState(tracker)
    val currentFeedback by rememberUpdatedState(feedback)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screenName, musicTrack) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentTracker.trackScreen(screenName)
                currentFeedback.onScreenOpen(musicTrack)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Whole-app background/foreground music pause - independent of [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]'s
 * own `onPaused`/`onResumed` (which freezes the gameplay *timer*, only relevant on the Gameplay
 * screen), this pauses/resumes whatever [MusicTrack] is currently playing regardless of which
 * screen is showing - Home, a menu, or Gameplay alike, since [com.suman.memoryarchitect.core.feedback.FeedbackManager]
 * is one shared singleton either way. `ON_PAUSE` rather than `ON_STOP` for the same "freeze the
 * instant focus is lost" reasoning as the gameplay timer - phone locked, an incoming call, or the
 * app minimized all reach `ON_PAUSE` immediately. */
@Composable
private fun MusicPauseOnBackground() {
    val feedback = rememberFeedback()
    val currentFeedback by rememberUpdatedState(feedback)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> currentFeedback.pauseMusic()
                Lifecycle.Event.ON_RESUME -> currentFeedback.resumeMusic()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
