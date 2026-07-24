package com.suman.memoryarchitect.core.feedback.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager as AndroidAudioManager
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** How a [ManagedTheme]'s one-time [ManagedTheme.initialOffsetMs] gets (re)armed - see
 * [MusicManagerImpl.acquireManagedTheme]. */
private enum class OffsetResetStrategy {
    /** The offset is consumed once for the lifetime of the owning [MusicManagerImpl] - only a
     * real process restart (a fresh singleton) re-arms it. Meant to pair with
     * [ManagedTheme.retainAcrossTrackSwitches] = true: a retained player is itself only ever
     * built once anyway, so "once per process" and "once per player instance" coincide. */
    ONCE_PER_PROCESS,

    /** The offset is re-applied every time this theme's player is rebuilt from scratch. Meant to
     * pair with [ManagedTheme.retainAcrossTrackSwitches] = false: a track that's always rebuilt
     * fresh when requested is, by construction, always starting a new "session" - Gameplay's
     * per-level/per-attempt reset falls straight out of this with no extra bookkeeping. */
    EVERY_FRESH_PLAYBACK,
}

/** Static, declarative config for one [MusicTrack] that plays with a one-time initial offset
 * before settling into an ordinary from-zero loop - see [MusicManagerImpl]'s `MANAGED_THEMES`. A
 * future theme wanting this same "start partway in, then loop from 0" shape (Countdown, Boss,
 * Event, Seasonal, ...) is exactly one new entry in that map; [MusicManagerImpl.acquireManagedTheme]
 * needs no changes to support it - no new playback code, no per-theme special-casing. */
private data class ManagedTheme(
    val rawResId: Int,
    val initialOffsetMs: Long,
    val resetStrategy: OffsetResetStrategy,
    /** Whether leaving this track pauses-and-keeps its player alive (so a later return resumes
     * exactly where it left off) or releases it outright (so a later return always starts fresh,
     * which is what makes [OffsetResetStrategy.EVERY_FRESH_PLAYBACK] mean "every session" for a
     * track like Gameplay). */
    val retainAcrossTrackSwitches: Boolean,
)

/** Per-[MusicTrack] runtime state for [ManagedTheme] playback - one instance per managed track,
 * created lazily the first time that track is requested and never discarded afterwards (even once
 * [player] itself is released), so [OffsetResetStrategy.ONCE_PER_PROCESS] always has somewhere
 * permanent to remember "already consumed." */
private class ManagedThemeState {
    var player: ExoPlayer? = null
    var offsetConsumed: Boolean = false
}

/**
 * One active looping (or, for [MusicTrack.VICTORY]/[MusicTrack.FAILURE], one-shot)
 * [ExoPlayer] at a time for the main bed, crossfaded on every [setTrack] track-identity change so
 * navigation never produces a hard music cut - the seamless loop itself comes from ExoPlayer's own
 * `REPEAT_MODE_ONE` (gapless, no manual loop-point math needed). A second, independent [ExoPlayer]
 * exists only transiently for the countdown overlay (see [startCountdownOverlay]) - never more
 * than these two instances total, and the countdown one is released the instant it's no longer
 * needed.
 *
 * Requests audio focus the first time any track plays and ducks/pauses/resumes in response to
 * focus changes (another app's ringtone, a call, etc.) exactly as a well-behaved Android media app
 * should. Every player is released the moment it's no longer needed - see [crossfade]/
 * [fadeOutAndRelease]/[stopCountdownOverlay] - so this never leaks native ExoPlayer instances.
 *
 * All ExoPlayer calls happen on [DispatcherProvider.main] - ExoPlayer instances are only safe to
 * use from the single thread (with a [android.os.Looper]) they were created on, and the app's
 * main thread is the natural, always-available choice.
 *
 * Some tracks ([MusicTrack.HOME], [MusicTrack.GAMEPLAY]) play with a one-time initial offset
 * before settling into an ordinary from-zero loop, scoped differently per track (Home: once per
 * process; Gameplay: once per gameplay session). That entire concern - offset, loop reset, retain
 * vs. rebuild - lives in one reusable mechanism ([ManagedTheme]/[ManagedThemeState]/
 * [acquireManagedTheme]/`MANAGED_THEMES`) rather than being duplicated per track; every other
 * track (Victory/Failure) is entirely unaffected and keeps the original plain build-fresh/
 * release-on-exit behavior.
 */
@Singleton
class MusicManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : MusicManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private val androidAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AndroidAudioManager
    private var focusRequest: AudioFocusRequest? = null

    private var currentPlayer: ExoPlayer? = null
    private var currentTrackId: MusicTrack = MusicTrack.NONE
    private var baseVolume = 0f
    private var focusDucked = false
    private var transitionJob: Job? = null

    // Runtime state for every MANAGED_THEMES-listed track - see ManagedTheme/ManagedThemeState/
    // acquireManagedTheme's docs. Keyed by MusicTrack so adding a future managed theme (Countdown/
    // Boss/Event/Seasonal/...) needs no new field here, just a new MANAGED_THEMES entry.
    private val managedThemeStates = mutableMapOf<MusicTrack, ManagedThemeState>()

    // Countdown overlay state - independent of the main bed above, see MusicTrack's doc for why
    // this is a second player rather than a setTrack target.
    private var countdownPlayer: ExoPlayer? = null
    private var mainDuckedVolume = 0f
    private var countdownOverlayVolume = 0f
    private var countdownJob: Job? = null

    // Pause/resume bookkeeping - which players (main bed, countdown overlay) were actually
    // playing at the moment pause() was called, so resume() only restarts what was really
    // running rather than assuming both, or neither. isExternallyPaused makes both idempotent:
    // this app has two independent pause triggers that can legitimately overlap (a whole-app
    // background observer and, on the Gameplay screen specifically, the ViewModel's own
    // ad-pause/background-pause) - without this guard, a second pause() call while already
    // paused would recapture isPlaying as false (since the player is already paused by then) and
    // silently overwrite the correct flag, so resume() would never restart it.
    private var isExternallyPaused = false
    private var pausedMainWasPlaying = false
    private var pausedCountdownWasPlaying = false

    override fun setTrack(track: MusicTrack, volume: Float) {
        if (track == MusicTrack.UNCHANGED) return
        scope.launch {
            if (track == currentTrackId) return@launch
            // A change in the main bed's identity always means the countdown-overlay moment (if
            // any) is over - Victory/Failure/a fresh Gameplay bed/a menu theme never plays under
            // a leftover countdown layer.
            countdownJob?.cancel()
            countdownPlayer?.release()
            countdownPlayer = null
            transitionJob?.cancel()
            val outgoing = currentPlayer
            val outgoingTrackId = currentTrackId
            // Whenever the track we're leaving is a retained managed theme, it must be paused and
            // kept alive, never released - this is the one flag that turns the generic
            // build-fresh/release-on-exit flow below into "this bed survives the trip."
            val retainOutgoing = outgoing != null && MANAGED_THEMES[outgoingTrackId]?.retainAcrossTrackSwitches == true
            currentTrackId = track
            baseVolume = volume

            val resId = track.rawResId()
            if (track == MusicTrack.NONE || resId == null) {
                // Fully stopping, not just navigating away - a retained theme's remembered player
                // must be forgotten too, or a later request for it would be handed back an
                // instance that's about to be released.
                forgetManagedPlayer(outgoingTrackId, outgoing)
                currentPlayer = null
                transitionJob = launch { fadeOutAndRelease(outgoing, retain = false) }
                return@launch
            }

            requestAudioFocusIfNeeded()
            val managedTheme = MANAGED_THEMES[track]
            val incoming = if (managedTheme != null) acquireManagedTheme(track, managedTheme) else buildPlayer(resId, loop = track.isLooping())
            currentPlayer = incoming
            transitionJob = launch { crossfade(outgoing, incoming, volume, retainOutgoing) }
        }
    }

    override fun setVolume(volume: Float) {
        scope.launch {
            baseVolume = volume
            if (!focusDucked && countdownPlayer == null) currentPlayer?.volume = volume.coerceIn(0f, 1f)
        }
    }

    override fun pause() {
        scope.launch {
            if (isExternallyPaused) return@launch // already paused - don't overwrite the flags below
            isExternallyPaused = true
            pausedMainWasPlaying = currentPlayer?.isPlaying == true
            pausedCountdownWasPlaying = countdownPlayer?.isPlaying == true
            currentPlayer?.pause()
            countdownPlayer?.pause()
        }
    }

    override fun resume() {
        scope.launch {
            if (!isExternallyPaused) return@launch // nothing paused - a second/stray resume() call
            isExternallyPaused = false
            if (pausedMainWasPlaying) currentPlayer?.play()
            if (pausedCountdownWasPlaying) countdownPlayer?.play()
            pausedMainWasPlaying = false
            pausedCountdownWasPlaying = false
        }
    }

    override fun stop() {
        scope.launch {
            transitionJob?.cancel()
            countdownJob?.cancel()
            val outgoingMain = currentPlayer
            val outgoingCountdown = countdownPlayer
            // A full stop always fully releases, even if outgoingMain is a retained managed
            // theme's player - otherwise that theme's state would keep pointing at an instance
            // stop() is about to release, and a later setTrack() for it would hand back an
            // already-released player.
            forgetManagedPlayer(currentTrackId, outgoingMain)
            currentPlayer = null
            countdownPlayer = null
            currentTrackId = MusicTrack.NONE
            transitionJob = launch {
                fadeOutAndRelease(outgoingMain, retain = false)
                outgoingCountdown?.release()
            }
            abandonAudioFocus()
        }
    }

    override fun startCountdownOverlay(duckedVolume: Float, overlayVolume: Float) {
        scope.launch {
            if (countdownPlayer != null) return@launch // already running - never a second overlay stacked on top
            countdownJob?.cancel()
            mainDuckedVolume = duckedVolume
            countdownOverlayVolume = overlayVolume

            val overlay = buildPlayer(COUNTDOWN_RAW_RES_ID, loop = true)
            countdownPlayer = overlay
            countdownJob = launch {
                // Duck the main bed down while the overlay rises - both ramp together over the
                // same window so the blend reads as one mix leaning in, not two separate fades.
                val outgoingMain = currentPlayer
                overlay.play()
                val steps = DUCK_MS / STEP_MS
                for (step in 1..steps) {
                    val t = step.toFloat() / steps
                    outgoingMain?.volume = (baseVolume - (baseVolume - duckedVolume) * t).coerceIn(0f, 1f)
                    overlay.volume = (overlayVolume * t).coerceIn(0f, 1f)
                    delay(STEP_MS.toLong())
                }
            }
        }
    }

    override fun stopCountdownOverlay(volume: Float) {
        scope.launch {
            countdownJob?.cancel()
            baseVolume = volume
            val outgoingCountdown = countdownPlayer
            countdownPlayer = null
            if (outgoingCountdown == null) {
                // Nothing was overlaid - still make sure the main bed sits at the right volume
                // (covers the "timer never reached zero because the round ended early" path).
                if (!focusDucked) currentPlayer?.volume = volume.coerceIn(0f, 1f)
                return@launch
            }
            countdownJob = launch {
                // Same finally-release reasoning as crossfade/fadeOutAndRelease - a rapid screen
                // switch (or the round ending) can cancel this mid-fade too.
                try {
                    val incomingMain = currentPlayer
                    val steps = DUCK_MS / STEP_MS
                    for (step in 1..steps) {
                        val t = step.toFloat() / steps
                        outgoingCountdown.volume = (countdownOverlayVolume * (1f - t)).coerceIn(0f, 1f)
                        if (!focusDucked) incomingMain?.volume = (mainDuckedVolume + (volume - mainDuckedVolume) * t).coerceIn(0f, 1f)
                        delay(STEP_MS.toLong())
                    }
                } finally {
                    outgoingCountdown.release()
                }
            }
        }
    }

    /** Builds an `android.resource://` URI for a `res/raw` file directly, rather than the now
     * deprecated [RawResourceDataSource.buildRawResourceUri] - the same URI shape ExoPlayer's
     * default resource data source resolves either way. */
    private fun rawResourceUri(resId: Int): android.net.Uri =
        android.net.Uri.Builder()
            .scheme(android.content.ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .path(resId.toString())
            .build()

    /** The one reusable entry point for every [ManagedTheme]-backed track: builds (or, for a
     * retained theme whose player is still alive, simply returns as-is) the [ExoPlayer] for
     * [track], applying [ManagedTheme.initialOffsetMs] exactly when [ManagedTheme.resetStrategy]
     * says to. This is the *only* place that offset is ever applied - callers ([setTrack]) never
     * need to know which tracks start partway in or how their reset is scoped, and adding a future
     * managed theme never touches this function, only `MANAGED_THEMES`. */
    private fun acquireManagedTheme(track: MusicTrack, theme: ManagedTheme): ExoPlayer {
        val state = managedThemeStates.getOrPut(track) { ManagedThemeState() }
        if (theme.retainAcrossTrackSwitches) {
            state.player?.let { return it } // still alive, paused wherever it was left - no rebuild, no seek
        }
        val player = buildPlayer(theme.rawResId, loop = true)
        val shouldApplyOffset = theme.initialOffsetMs > 0L && when (theme.resetStrategy) {
            OffsetResetStrategy.ONCE_PER_PROCESS -> !state.offsetConsumed
            OffsetResetStrategy.EVERY_FRESH_PLAYBACK -> true // a fresh build IS a new session, always
        }
        if (shouldApplyOffset) {
            player.seekTo(theme.initialOffsetMs)
            state.offsetConsumed = true
        }
        state.player = player
        return player
    }

    /** Clears a managed track's remembered player reference when it's about to be fully released
     * (never just paused) - so a later request for that same track can't be handed back an
     * already-released instance. No-op for tracks that aren't managed, or if [player] isn't
     * actually the one currently on record (e.g. it was already superseded). */
    private fun forgetManagedPlayer(track: MusicTrack, player: ExoPlayer?) {
        val state = managedThemeStates[track] ?: return
        if (state.player === player) state.player = null
    }

    private fun buildPlayer(resId: Int, loop: Boolean): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        player.setAudioAttributes(
            Media3AudioAttributes.Builder()
                .setUsage(C.USAGE_GAME)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ false, // this class owns focus handling itself, see requestAudioFocusIfNeeded
        )
        player.setMediaItem(MediaItem.fromUri(rawResourceUri(resId)))
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.volume = 0f
        player.prepare()
        return player
    }

    /** Fades [outgoing] to silence while [incoming] rises to [targetVolume] over
     * [CROSSFADE_MS], then either pauses or releases [outgoing] depending on [retainOutgoing] - so
     * a screen/mode change is heard as one bed dissolving into the next rather than a jump cut.
     * [retainOutgoing] is true exactly when [outgoing] is a retained managed theme's player (see
     * [acquireManagedTheme]): it must survive this fade paused, not released, so a later return to
     * that track resumes it from this exact position instead of rebuilding from scratch. The
     * pause-or-release happens in a `finally` block on purpose: switching screens quickly enough
     * to cancel this job mid-fade (a very ordinary thing to do - rapid navigation) must still
     * settle [outgoing] into one of those two states, or it's simply left running forever at
     * whatever volume it was cancelled at, stacking up as audibly overlapping tracks the more
     * times it happens.
     *
     * Both ramps start from each player's *actual current* volume - [outgoing]'s and [incoming]'s
     * - rather than assuming "outgoing is at [targetVolume], incoming is at 0." That assumption
     * only holds for an uninterrupted fade; the moment a second navigation cancels one crossfade
     * and starts another (trivially easy - just bounce in and out of a screen quickly), the
     * previous fade left both players partway through their ramp. Snapping to the old assumed
     * endpoints there means the outgoing track jumps back up toward full volume before fading out
     * again, and the incoming track dips back to silence even if it was already most of the way in
     * - audible as a track "coming back"/alternating rather than one continuous dissolve. Starting
     * from wherever each player actually is makes every fade continuous no matter how many times
     * it gets interrupted mid-flight. */
    private suspend fun crossfade(outgoing: ExoPlayer?, incoming: ExoPlayer, targetVolume: Float, retainOutgoing: Boolean) {
        val outgoingStartVolume = outgoing?.volume ?: 0f
        val incomingStartVolume = incoming.volume
        try {
            incoming.play()
            val steps = CROSSFADE_MS / STEP_MS
            for (step in 1..steps) {
                val t = step.toFloat() / steps
                incoming.volume = (incomingStartVolume + (targetVolume - incomingStartVolume) * t).coerceIn(0f, 1f)
                outgoing?.volume = (outgoingStartVolume * (1f - t)).coerceIn(0f, 1f)
                delay(STEP_MS.toLong())
            }
        } finally {
            if (retainOutgoing) outgoing?.pause() else outgoing?.release()
        }
    }

    /** See [crossfade]'s doc on why the pause-or-release happens in `finally`, what [retain]
     * means, and why the fade starts from [outgoing]'s actual current volume rather than an
     * assumed one - same reasoning applies here. */
    private suspend fun fadeOutAndRelease(outgoing: ExoPlayer?, retain: Boolean) {
        if (outgoing == null) return
        val startVolume = outgoing.volume
        try {
            val steps = CROSSFADE_MS / STEP_MS
            for (step in 1..steps) {
                val t = 1f - step.toFloat() / steps
                outgoing.volume = (startVolume * t).coerceIn(0f, 1f)
                delay(STEP_MS.toLong())
            }
        } finally {
            if (retain) outgoing.pause() else outgoing.release()
        }
    }

    private fun requestAudioFocusIfNeeded() {
        if (focusRequest != null) return
        val attributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_GAME)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AndroidAudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(::onAudioFocusChanged)
            .build()
        focusRequest = request
        androidAudioManager.requestAudioFocus(request)
    }

    /** A well-behaved Android media app pauses on a real loss (a call ringing), gently ducks
     * (halves volume) for a transient duck request (a notification chime), and resumes/restores
     * on gain - never just keeps playing over another app's audio. Applies to the countdown
     * overlay too, when one happens to be active.
     *
     * The gain branch only actually resumes playback if [isExternallyPaused] is false. Without
     * that check, a focus-gain event (a call ending, another app releasing focus) would force
     * [currentPlayer] to start playing even while the app is intentionally silent - backgrounded,
     * or mid-rewarded-ad - which is exactly "a track playing when it shouldn't be," heard as music
     * starting up on its own while the screen shows nothing playing. [resume] (called once the app
     * itself decides it's really time to resume) still restores focus-ducked volume/playback
     * correctly afterwards, since it only ever restarts what [pause] actually captured as playing. */
    private fun onAudioFocusChanged(change: Int) {
        scope.launch {
            when (change) {
                AndroidAudioManager.AUDIOFOCUS_LOSS, AndroidAudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    currentPlayer?.pause()
                    countdownPlayer?.pause()
                }
                AndroidAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    focusDucked = true
                    val mainTarget = if (countdownPlayer != null) mainDuckedVolume else baseVolume
                    currentPlayer?.volume = (mainTarget * 0.35f).coerceIn(0f, 1f)
                    countdownPlayer?.volume = (countdownOverlayVolume * 0.35f).coerceIn(0f, 1f)
                }
                AndroidAudioManager.AUDIOFOCUS_GAIN -> {
                    if (focusDucked) {
                        focusDucked = false
                        val mainTarget = if (countdownPlayer != null) mainDuckedVolume else baseVolume
                        currentPlayer?.volume = mainTarget.coerceIn(0f, 1f)
                        countdownPlayer?.volume = countdownOverlayVolume.coerceIn(0f, 1f)
                    }
                    if (!isExternallyPaused) {
                        currentPlayer?.play()
                        countdownPlayer?.play()
                    }
                }
            }
        }
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { androidAudioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private companion object {
        // Was 400ms - long enough now that a screen-to-screen bed change reads as one track
        // genuinely dissolving into the next, never a cut a player could put a number on.
        const val CROSSFADE_MS = 1500
        // The countdown overlay's own duck-in/duck-out - shorter than CROSSFADE_MS on purpose
        // (it still needs to read as "the clock is speeding up," not a lazy fade), but longer
        // than the old 600ms so it doesn't feel like a separate, jarring event either.
        const val DUCK_MS = 900
        const val STEP_MS = 20
        val COUNTDOWN_RAW_RES_ID = R.raw.countdown_theme

        /** The Home bed's very first playback this process starts partway in rather than at 0 -
         * the single source of truth for that offset; never hardcode 40_000 anywhere else. */
        const val HOME_THEME_INITIAL_OFFSET_MS = 40_000L

        /** The Gameplay bed's first playback *each gameplay session* starts partway in rather
         * than at 0 - the single source of truth for that offset; never hardcode 36_000 anywhere
         * else. */
        const val GAMEPLAY_THEME_INITIAL_OFFSET_MS = 36_000L

        /** Every [MusicTrack] that plays with a one-time initial offset before settling into an
         * ordinary from-zero loop, and how each one's offset is scoped - see [ManagedTheme]'s doc.
         * [MusicTrack.HOME] resets only on a process restart (its player is retained across every
         * trip through Gameplay); [MusicTrack.GAMEPLAY] resets every gameplay session, which falls
         * straight out of it never being retained - every fresh build already is a new session. A
         * future theme with this same shape (Countdown/Boss/Event/Seasonal/...) is exactly one new
         * entry here; [acquireManagedTheme] needs no changes to support it. */
        val MANAGED_THEMES: Map<MusicTrack, ManagedTheme> = mapOf(
            MusicTrack.HOME to ManagedTheme(
                rawResId = R.raw.home_theme,
                initialOffsetMs = HOME_THEME_INITIAL_OFFSET_MS,
                resetStrategy = OffsetResetStrategy.ONCE_PER_PROCESS,
                retainAcrossTrackSwitches = true,
            ),
            MusicTrack.GAMEPLAY to ManagedTheme(
                rawResId = R.raw.gameplay_theme,
                initialOffsetMs = GAMEPLAY_THEME_INITIAL_OFFSET_MS,
                resetStrategy = OffsetResetStrategy.EVERY_FRESH_PLAYBACK,
                retainAcrossTrackSwitches = false,
            ),
        )
    }
}
