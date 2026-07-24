package com.suman.memoryarchitect.feature.gameplay

import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.PlacedObject
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult

sealed interface GameplayUiState {
    data object Loading : GameplayUiState
    data class Error(val error: AppError) : GameplayUiState

    /** Level 1 (Classic) has been generated but the real Memorize timer hasn't started yet — the
     * first-time tutorial is showing first, so it can narrate against this same [level] without
     * burning any of the player's actual memorize time while it plays out. Every other session
     * (every other mode, level, or a Classic level 1 replay after it's already been passed) skips
     * this state entirely and goes straight to [InProgress]. */
    data class TutorialPending(val level: LevelSpec) : GameplayUiState

    /** A one-shot "here's a new mechanic" beat the very first time a Classic level requires
     * rotation (level 30) or placement order (level 55) - the same "explain before the clock
     * starts" idea as [TutorialPending], scoped to a single new fact instead of a full replay of
     * the whole loop. See [GameplayViewModel.pendingMechanicIntro]. */
    data class MechanicIntroPending(val level: LevelSpec, val mechanic: NewMechanic) : GameplayUiState

    data class InProgress(
        val phase: GamePhase,
        val level: LevelSpec,
        val remainingMs: Long?,
        val placements: Map<String, PlacedObject> = emptyMap(),
        val trayObjectIds: List<String> = emptyList(),
        /** Objects in the order they currently sit on the board — a re-placement after
         * picking an object back up moves it to the end, reflecting the player's most recent
         * placement action. Only meaningful (and only scored) when [LevelSpec.orderModeEnabled]. */
        val placementOrder: List<String> = emptyList(),
        /** See [PrivacyShieldPhase] - [GameplayViewModel.onPaused]/[onResumed]/[onPrivacyShieldContinue]
         * are the only places this changes. */
        val privacyShield: PrivacyShieldPhase = PrivacyShieldPhase.NONE,
    ) : GameplayUiState

    /**
     * [submission] is null for Practice mode (never submitted) or while [isSubmitting] is
     * still true for scored modes. [levelOutcome] is non-null only for [com.suman.memoryarchitect.domain.model.GameMode.CLASSIC],
     * driving the pass/fail button layout on the results screen. [stars] is computed for every
     * mode (used for tiered feedback copy); only Classic persists it via [levelOutcome]. [passed]
     * is the same accuracy-threshold verdict [levelOutcome] carries for Classic, exposed here for
     * every mode - the results screen's victory/failure music cue (see
     * [com.suman.memoryarchitect.ui.screens.gameplay.GameplayResultsPanel]) needs a real pass/fail
     * signal for Practice/Daily/Weekly too, not just the star-tier flavor text.
     */
    data class Finished(
        val result: ScoreResult,
        val stars: Int,
        val passed: Boolean,
        val isSubmitting: Boolean = false,
        val submission: ScoreSubmissionResult? = null,
        val levelOutcome: LevelCompletionOutcome? = null,
    ) : GameplayUiState
}

/** A campaign mechanic that debuts partway through, not at level 1 - see
 * [com.suman.memoryarchitect.domain.progression.LevelCampaignEngine.isRotationDebutLevel]/
 * [com.suman.memoryarchitect.domain.progression.LevelCampaignEngine.isOrderModeDebutLevel]. */
enum class NewMechanic { ROTATION, ORDER_MODE }

/**
 * Drives the Anti-Cheat Privacy Shield (see [com.suman.memoryarchitect.ui.screens.gameplay.PrivacyShieldOverlay])
 * - the full-screen branded cover that replaces the gameplay scene the instant the app loses real
 * focus, and stays up (gating timer/music/input) until the player explicitly confirms they're back.
 * Independent of [WindowManager.LayoutParams.FLAG_SECURE] (set for the whole time Gameplay is on
 * screen - see [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScreen]), which is what
 * actually keeps the real scene out of Recent Apps/screenshots/screen recording; this is the
 * in-app experience layered on top of that guarantee.
 */
enum class PrivacyShieldPhase {
    /** Normal gameplay - nothing is obscured, frozen, or blocked for this reason. */
    NONE,
    /** The app just lost focus (backgrounded, Recent Apps, lock screen, another app/dialog taking
     * focus, or a fresh launch restoring a session after a background process kill) - timer, music,
     * and input are frozen, and the shield fully covers the scene. */
    HIDDEN_AWAY,
    /** The app is foregrounded again but the player hasn't confirmed they're back yet - still
     * fully frozen and covered, waiting on an explicit Continue tap. */
    READY_TO_CONTINUE,
}
