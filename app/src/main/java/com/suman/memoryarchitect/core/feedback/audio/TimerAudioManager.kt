package com.suman.memoryarchitect.core.feedback.audio

/**
 * The looping "tick-tock" countdown bed heard throughout Memorize and Rebuild alike - distinct
 * from [GameAudioManager]'s one-shot [SfxId.TIMER_TICK_SOFT]/[SfxId.TIMER_TICK_URGENT] blips (those
 * stay exactly as they were) and from [MusicManager]'s ducked countdown music overlay (also
 * unchanged) - this is a third, independent layer: a continuous loop for the phase's full duration
 * that crossfades from a normal pace to a faster one near the end. See [TimerAudioStateMachine] for
 * the state/transition rules every method here defers to.
 *
 * Backed by its own private [android.media.SoundPool] instance, never [GameAudioManager]'s (that
 * class's own doc reserves its pool for one-shot SFX only) and never a
 * [com.suman.memoryarchitect.core.feedback.audio.MusicManager]-style `ExoPlayer` (SoundPool gives
 * zero-latency start/pause/resume-with-position and live per-stream volume control, exactly what a
 * gapless loop-to-loop crossfade needs; ExoPlayer's prepare()-then-play latency is the wrong shape
 * for "must already be prepared for instant playback").
 */
interface TimerAudioManager {
    /** Loads both timer tracks into memory so a later [startNormal]/[crossfadeToFinal] call never
     * pays a load-latency hit - call once, well before any phase begins. Safe to call more than
     * once (a no-op past the first successful load). */
    fun preload()

    /** Begins looping the normal-pace track from the beginning - a no-op if it's already the
     * active track (see [TimerAudioStateMachine.requestNormal]), so this is safe to call on every
     * phase-start tick without ever restarting an already-playing loop. */
    fun startNormal(volume: Float)

    /** Crossfades from the normal-pace loop to the faster one over a short, fixed duration - a
     * no-op if the faster track is already active. Falls back to starting the faster track
     * directly (no fade) if nothing was playing yet to fade from. */
    fun crossfadeToFinal(volume: Float)

    /** Immediately stops all timer audio and resets to [TimerAudioState.NONE] - a no-op if nothing
     * is playing. */
    fun stop()

    /** Freezes whatever's currently playing in place (preserving loop position) - for a rewarded
     * ad, the app backgrounding, or the Anti-Cheat Privacy Shield. A no-op if already paused or
     * nothing is playing. Any in-flight crossfade is resolved immediately rather than left
     * straddling the pause, since nothing is audible during the pause anyway. */
    fun pause()

    /** Resumes exactly where [pause] left off. A no-op if not currently paused. */
    fun resume()
}
