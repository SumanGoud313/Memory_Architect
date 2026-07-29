package com.suman.memoryarchitect.core.feedback

import com.suman.memoryarchitect.core.feedback.audio.MusicTrack

/** Which phase of a timer this urgency cue belongs to - shared between Memorize and Rebuild
 * countdowns per the brief ("Timer Audio" applies to both phases identically). */
enum class TimerUrgency { CALM, LAST_TEN_SECONDS, LAST_FIVE_SECONDS, FINAL_SECOND }

/** The overall "how did that go" read of a finished round, already computed by the results UI's
 * existing tier logic - see [com.suman.memoryarchitect.core.feedback.FeedbackManager.onResultsRevealed].
 * Deliberately graduated rather than a binary win/lose, matching this app's existing "never shows
 * red/punishing styling" results design (even a low-star run stays upbeat) - [ENCOURAGE] is a
 * warm nudge to retry, never a failure buzzer. */
enum class ResultMood { VICTORY, GREAT, ENCOURAGE }

/**
 * The single facade every ViewModel and (via a thin Compose bridge - never directly) every
 * Composable calls for feedback. Combines [com.suman.memoryarchitect.core.feedback.audio.GameAudioManager],
 * [com.suman.memoryarchitect.core.feedback.audio.MusicManager], and
 * [com.suman.memoryarchitect.core.feedback.haptics.HapticsManager] behind one semantic,
 * event-shaped API so no call site ever needs to know which of the three actually fires for a
 * given moment, or read [AudioSettings] itself - this is the only class that does.
 */
interface FeedbackManager {

    // --- UI chrome -----------------------------------------------------------------------
    fun onUiTap()
    fun onUiConfirm()
    fun onUiBack()
    fun onDialogOpen()
    fun onDialogClose()
    fun onScreenOpen(track: MusicTrack)

    /** Changes the background music bed without the screen-open chime/analytics [onScreenOpen]
     * implies - for in-place mood changes that aren't a navigation event (e.g. Memorize ->
     * Reconstruct within the same Gameplay screen). */
    fun setMusicTrack(track: MusicTrack)

    /** Pauses whatever's currently playing (main bed and countdown overlay alike) in place, for
     * a rewarded ad or the app backgrounding - see [com.suman.memoryarchitect.core.feedback.audio.MusicManager.pause].
     * Always paired with [resumeMusic] once the interruption ends. */
    fun pauseMusic()

    /** Resumes exactly where [pauseMusic] left off - the same position, the same track, the same
     * countdown-overlay state if one was active. A no-op while [setMusicResumeSuppressed] is
     * active, however - see its doc. */
    fun resumeMusic()

    /** Suppresses every [resumeMusic] call while [suppressed] is true, without touching whatever's
     * currently paused/playing - used by Gameplay's Anti-Cheat Privacy Shield
     * ([com.suman.memoryarchitect.feature.gameplay.PrivacyShieldPhase]) so the generic, whole-app
     * background/foreground music observer (see [com.suman.memoryarchitect.ui.navigation.MusicPauseOnBackground])
     * firing the instant the Activity is foregrounded again can never silently resume the gameplay
     * bed before the player has explicitly confirmed they're back via the shield's Continue action.
     * Both observers call the same [resumeMusic]/[pauseMusic] pair, so this is the one chokepoint
     * that makes "resume only after Continue" hold regardless of which observer's `ON_RESUME`
     * callback happens to run first. */
    fun setMusicResumeSuppressed(suppressed: Boolean)

    // --- Gameplay --------------------------------------------------------------------------
    fun onObjectPickup()
    fun onObjectRotate()
    fun onObjectPlace()
    fun onComboStep(step: Int)

    /** [remainingMs] drives the urgency tier internally - callers just report the live countdown
     * on every tick, this decides when a tick is audible/felt at all (only within the last 10s,
     * see [FeedbackManagerImpl]) so it's safe to call from a 100ms timer loop unconditionally.
     * [isReconstructPhase] gates the countdown-music overlay specifically to the Reconstruct
     * "beat the clock" phase - Memorize's own countdown reuses this same tick cadence for
     * urgency sfx, but most levels' Memorize duration is already close to 10 seconds total, so
     * treating it as "final stretch" too would swap in the countdown bed almost immediately.
     *
     * Also drives the crossfade-to-final-stretch half of the independent looping "tick-tock"
     * countdown bed ([com.suman.memoryarchitect.core.feedback.audio.TimerAudioManager]) - the
     * *starting* half is [onTimerPhaseStarted], a separate explicit call, not inferred from this
     * stream (see that method's doc for why inference doesn't work here). This method crossfades
     * to the faster loop once [remainingMs] crosses a fixed threshold that differs by phase
     * ([isReconstructPhase]) - see [FeedbackManagerImpl] for the exact thresholds. */
    fun onTimerTick(remainingMs: Long, isReconstructPhase: Boolean)

    /** Starts the looping tick-tock countdown bed from the beginning - call exactly once at every
     * genuine moment a timed Memorize or Reconstruct phase begins (a fresh attempt, a Rewatch
     * replay's own Memorize and its later resumed Reconstruct, or a process-death restore), never
     * inferred from [onTimerTick]'s [remainingMs] stream: two consecutive phases of the *same*
     * type (e.g. a level quit and retried mid-Memorize, then a new Memorize begins) are
     * indistinguishable from one continuous phase using only the tick stream, so this needs a
     * real, explicit signal from whichever function actually starts the phase's
     * [com.suman.memoryarchitect.feature.gameplay.engine.CountdownTimer]. Safe to call even when
     * nothing should audibly change - the underlying state machine only reacts if this is
     * genuinely a transition. */
    fun onTimerPhaseStarted()

    /** Immediately stops the looping timer tick-tock bed and resets its state machine to
     * [com.suman.memoryarchitect.core.feedback.audio.TimerAudioState.NONE] - call at every point
     * gameplay's own [remainingMs]-driven timer stops for a reason other than a pause (round
     * submitted, quit, or a fresh attempt tearing down the previous one). A no-op if nothing is
     * currently playing. */
    fun stopTimerAudio()

    // --- Results ---------------------------------------------------------------------------
    /** [mood] drives the sfx/haptic flavor only (the existing 5-tier "how did that go" read).
     * [passed] is the real accuracy-threshold verdict, independent of star tier, and is what
     * actually picks [com.suman.memoryarchitect.core.feedback.audio.MusicTrack.VICTORY] vs
     * [com.suman.memoryarchitect.core.feedback.audio.MusicTrack.FAILURE] - a tier like GREAT still
     * gets its own sfx/haptic feel but always resolves to the real victory bed underneath it,
     * since 2 stars is still a genuine pass. */
    fun onResultsRevealed(mood: ResultMood, passed: Boolean)
    fun onCoinsAwarded()
    fun onXpAwarded()
    fun onStarAwarded()
    fun onAchievementUnlocked()
    fun onLevelUnlocked()
    fun onDailyRewardClaimed()
    fun onWeeklyRewardClaimed()

    /** The instant a Lucky Spin's wheel starts turning - see
     * [com.suman.memoryarchitect.ui.screens.shop.LuckySpinWheel]'s doc for the 5-second spin this
     * accompanies. */
    fun onLuckySpinStarted()

    /** The instant a Lucky Spin's wheel settles and its reward reveals - [HapticPattern.VICTORY]
     * tier, the same STRONG haptic reserved for a Perfect/Memory Master results reveal, since a
     * spin's reward (especially a cosmetic, or the 500-coin jackpot) is meant to feel like a
     * comparably major moment. */
    fun onLuckySpinRevealed()

    // --- Denials / warnings ------------------------------------------------------------------
    fun onWarning()
    fun onError()
}
