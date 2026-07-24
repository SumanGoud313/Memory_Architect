package com.suman.memoryarchitect.core.feedback.audio

/** The looping "tick-tock" countdown bed's current state - shared by Memorize and Rebuild alike,
 * since both just report the same live-remaining-time signal (see [TimerAudioManager]). */
enum class TimerAudioState { NONE, NORMAL, FINAL }

/** What [TimerAudioManagerImpl] should actually do in response to a requested transition, given
 * the state at the moment of the request - [NoOp] for every request that doesn't represent a real
 * change (e.g. requesting NORMAL while already NORMAL), which is what makes it safe to call
 * [TimerAudioStateMachine.requestNormal]/etc. unconditionally on every 100ms tick without ever
 * restarting, reloading, or recreating a player that's already doing the right thing. */
enum class TimerAudioAction { NO_OP, PLAY_NORMAL_FRESH, CROSSFADE_TO_FINAL, PLAY_FINAL_FRESH, STOP }

/**
 * Pure decision logic for the timer tick-tock audio - zero Android/SoundPool dependency by design,
 * so it's unit-testable without an emulator (everything else touching real playback,
 * [TimerAudioManagerImpl], can't be - see that class's doc). [TimerAudioManagerImpl] owns exactly
 * one instance of this and only ever acts on the [TimerAudioAction] each method returns; it never
 * re-derives "is this actually a change" logic itself, so this is the single place that invariant
 * lives and can be verified in isolation.
 *
 * [requestFinal] distinguishes a genuine NORMAL->FINAL transition (crossfade the two together)
 * from a cold start straight into FINAL (nothing to fade from, e.g. a restored/resumed phase whose
 * corrected remaining time already happens to be under the final-stretch threshold on its very
 * first tick) - the caller uses [TimerAudioAction.CROSSFADE_TO_FINAL] vs [TimerAudioAction.PLAY_FINAL_FRESH]
 * to tell those two cases apart.
 */
class TimerAudioStateMachine {
    var state: TimerAudioState = TimerAudioState.NONE
        private set

    fun requestNormal(): TimerAudioAction {
        if (state == TimerAudioState.NORMAL) return TimerAudioAction.NO_OP
        state = TimerAudioState.NORMAL
        return TimerAudioAction.PLAY_NORMAL_FRESH
    }

    fun requestFinal(): TimerAudioAction {
        if (state == TimerAudioState.FINAL) return TimerAudioAction.NO_OP
        val wasNormal = state == TimerAudioState.NORMAL
        state = TimerAudioState.FINAL
        return if (wasNormal) TimerAudioAction.CROSSFADE_TO_FINAL else TimerAudioAction.PLAY_FINAL_FRESH
    }

    fun requestStop(): TimerAudioAction {
        if (state == TimerAudioState.NONE) return TimerAudioAction.NO_OP
        state = TimerAudioState.NONE
        return TimerAudioAction.STOP
    }
}
