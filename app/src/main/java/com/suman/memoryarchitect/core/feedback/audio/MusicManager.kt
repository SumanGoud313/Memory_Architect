package com.suman.memoryarchitect.core.feedback.audio

import com.suman.memoryarchitect.R

/**
 * Every distinct background bed this app plays. The real soundtrack shipped as exactly 5 tracks
 * in `res/raw/` (see [rawResId]) - one shared theme per mood, not a per-mode/per-phase library -
 * so a single [GAMEPLAY] bed covers Classic/Practice/Daily/Weekly and both Memorize and
 * Reconstruct alike (see [com.suman.memoryarchitect.core.feedback.GameModeMusic]): switching
 * phases never changes [MusicTrack] identity, so [MusicManagerImpl.setTrack] naturally no-ops
 * and the same bed just keeps playing instead of crossfading into itself.
 *
 * The final-10-seconds countdown bed is not a [MusicTrack]/[MusicManager.setTrack] target at all
 * - it plays as a bounded overlay layered on top of [GAMEPLAY] (see
 * [MusicManager.startCountdownOverlay]), ducking the gameplay bed underneath it rather than
 * replacing it, so the base music is never heard to "restart" once the countdown ends.
 *
 * [VICTORY]/[FAILURE] are short one-shots (see [isLooping]); everything else loops seamlessly for
 * as long as that screen/phase is active.
 */
enum class MusicTrack {
    /** Every non-gameplay screen - Home, Mode Select, Level Select, Profile, Settings, and any
     * future menu screen - shares this one bed on purpose (see [com.suman.memoryarchitect.ui.navigation.MemoryArchitectNavHost]),
     * so navigating between menus never restarts or crossfades anything: [MusicManagerImpl.setTrack]
     * already no-ops when the requested track matches what's already playing. */
    HOME,

    /** One continuous bed for every mode (Classic/Practice/Daily/Weekly) and both phases
     * (Memorize/Reconstruct) - see [com.suman.memoryarchitect.core.feedback.GameModeMusic]. */
    GAMEPLAY,

    VICTORY,
    FAILURE,

    /** Explicitly "leave whatever is already playing alone" - used only for destinations that
     * intentionally sit on top of another screen without their own mood (e.g. the debug-only
     * Analytics Dashboard reached from Settings). */
    UNCHANGED,
    /** Stops music entirely. */
    NONE,
}

/** Which `res/raw` resource backs this track. Null for the three control-only values, which
 * never resolve to a file. */
fun MusicTrack.rawResId(): Int? = when (this) {
    MusicTrack.HOME -> R.raw.home_theme
    MusicTrack.GAMEPLAY -> R.raw.gameplay_theme
    MusicTrack.VICTORY -> R.raw.victory_theme
    MusicTrack.FAILURE -> R.raw.failure_theme
    MusicTrack.UNCHANGED, MusicTrack.NONE -> null
}

/** Whether this track loops seamlessly ([androidx.media3.common.Player.REPEAT_MODE_ONE]) or
 * plays once through. */
fun MusicTrack.isLooping(): Boolean = this != MusicTrack.VICTORY && this != MusicTrack.FAILURE

/**
 * Owns the single active background-music player, crossfading between [MusicTrack]s so screen
 * navigation never produces a hard cut, plus a bounded second player exclusively for the
 * final-10-seconds countdown overlay (see [startCountdownOverlay]/[stopCountdownOverlay]) - the
 * only two [androidx.media3.exoplayer.ExoPlayer] instances this app ever creates, and the
 * countdown one only exists while a round is actually in its final stretch. The only class in
 * this app allowed to touch ExoPlayer - see [com.suman.memoryarchitect.core.feedback.FeedbackManager].
 * [volume] is a pre-computed 0..1 linear gain (already folded with master/music volume and mute
 * state by the caller).
 */
interface MusicManager {
    fun setTrack(track: MusicTrack, volume: Float)
    fun setVolume(volume: Float)

    /** Pauses the current bed (and countdown overlay, if active) in place - unlike [stop], the
     * player(s) are kept alive (not released), so [resume] restarts from the exact position/track
     * it was paused at, not from the top. */
    fun pause()

    /** Resumes exactly where [pause] left off. A no-op if nothing is paused (e.g. no track was
     * ever set), so it's always safe to call speculatively. */
    fun resume()

    fun stop()

    /** Ducks the current bed to [duckedVolume] and starts the bounded `countdown_theme` overlay
     * layered on top of it at [overlayVolume], looping until [stopCountdownOverlay] - the one
     * deliberate, documented exception to "one player at a time," since ducking-under-an-overlay
     * is structurally two simultaneous streams, not a track switch. */
    fun startCountdownOverlay(duckedVolume: Float, overlayVolume: Float)

    /** Releases the countdown overlay (if any) and restores the main bed to [volume] - always
     * safe to call even if no overlay is active. */
    fun stopCountdownOverlay(volume: Float)
}
