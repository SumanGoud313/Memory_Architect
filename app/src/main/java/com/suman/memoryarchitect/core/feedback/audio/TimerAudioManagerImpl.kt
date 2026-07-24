package com.suman.memoryarchitect.core.feedback.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.os.SystemClock
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

/**
 * See [TimerAudioManager] for the contract/why-SoundPool. All actual "should this be a no-op"
 * decisions are delegated to [stateMachine] - this class only ever translates its returned
 * [TimerAudioAction] into real [SoundPool] calls, never re-derives the decision itself.
 *
 * The crossfade is *loop-boundary-aware*: `normal_timer`/`final_timer` are different tempos (a
 * slower and a faster Tick...Tock), so no amplitude fade alone can hide a rhythm change landing
 * mid-tick - that reads as a stutter no matter how smooth the volume curve is. Firing the actual
 * fade at the normal loop's next natural repeat boundary instead (see [computeAlignmentDelayMs])
 * means the outgoing track always finishes its current Tick...Tock phrase cleanly before the new
 * tempo begins, which is what actually reads as "one continuous timer" rather than the amplitude
 * curve doing that alone.
 */
@Singleton
class TimerAudioManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : TimerAudioManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val loadMutex = Mutex()
    private val stateMachine = TimerAudioStateMachine()
    private val pendingLoads = ConcurrentHashMap<Int, CompletableDeferred<Unit>>()

    private var normalSampleId: Int? = null
    private var finalSampleId: Int? = null
    private var normalLoopDurationMs: Long? = null
    private var normalStartedAtElapsedMs: Long = 0L

    // The stream currently representing "what the player hears right now" - the fade-in target
    // during a crossfade, or the only stream outside one. outgoingStreamId is non-null only once
    // the fade has actually begun swapping streams (see pendingFinalSwap for the sub-phase before
    // that, while still waiting for a clean loop boundary); both null once stopped.
    private var activeStreamId: Int? = null
    private var outgoingStreamId: Int? = null
    private var crossfadeJob: Job? = null
    private var isPaused = false
    private var currentVolume = 1f

    // True only during a scheduled crossfade's alignment wait, before it's actually swapped
    // streams yet - activeStreamId still points at the *outgoing* track during this window, and
    // pendingTargetSampleId records what it's waiting to become. See finishCrossfadeImmediately.
    private var pendingFinalSwap = false
    private var pendingTargetSampleId: Int? = null

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_CONCURRENT_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .apply {
            setOnLoadCompleteListener { _, sampleId, _ -> pendingLoads.remove(sampleId)?.complete(Unit) }
        }

    override fun preload() {
        scope.launch {
            loadMutex.withLock {
                if (normalSampleId == null) normalSampleId = loadRaw(R.raw.normal_timer)
                if (finalSampleId == null) finalSampleId = loadRaw(R.raw.final_timer)
                if (normalLoopDurationMs == null) normalLoopDurationMs = measureDurationMs(R.raw.normal_timer)
            }
        }
    }

    override fun startNormal(volume: Float) {
        currentVolume = volume
        when (stateMachine.requestNormal()) {
            TimerAudioAction.PLAY_NORMAL_FRESH -> {
                cancelCrossfade()
                stopActiveStreams()
                val sampleId = normalSampleId
                activeStreamId = sampleId?.let { playLoop(it, volume) }
                if (activeStreamId != null) normalStartedAtElapsedMs = SystemClock.elapsedRealtime()
            }
            else -> Unit
        }
    }

    override fun crossfadeToFinal(volume: Float) {
        currentVolume = volume
        when (stateMachine.requestFinal()) {
            TimerAudioAction.CROSSFADE_TO_FINAL -> {
                val from = activeStreamId
                val toSample = finalSampleId
                if (from == null || toSample == null) {
                    // Nothing loaded/playing to fade from (e.g. preload hasn't finished yet) -
                    // start directly rather than fade from silence, which would just be a delay.
                    stopActiveStreams()
                    activeStreamId = toSample?.let { playLoop(it, volume) }
                    return
                }
                beginCrossfade(from, toSample, volume)
            }
            TimerAudioAction.PLAY_FINAL_FRESH -> {
                cancelCrossfade()
                stopActiveStreams()
                activeStreamId = finalSampleId?.let { playLoop(it, volume) }
            }
            else -> Unit
        }
    }

    override fun stop() {
        when (stateMachine.requestStop()) {
            TimerAudioAction.STOP -> {
                cancelCrossfade()
                stopActiveStreams()
            }
            else -> Unit
        }
        isPaused = false
    }

    override fun pause() {
        if (isPaused || stateMachine.state == TimerAudioState.NONE) return
        finishCrossfadeImmediately()
        isPaused = true
        activeStreamId?.let { soundPool.pause(it) }
    }

    override fun resume() {
        if (!isPaused) return
        isPaused = false
        activeStreamId?.let { soundPool.resume(it) }
    }

    private fun playLoop(sampleId: Int, volume: Float): Int =
        soundPool.play(sampleId, volume, volume, PRIORITY, LOOP_FOREVER, 1f)

    /** How long to wait before actually starting the fade so it lands on the outgoing loop's next
     * natural repeat rather than cutting off mid-tick - capped at [MAX_ALIGNMENT_WAIT_MS] so a
     * long loop (or an unmeasured duration) never meaningfully delays the requested 4s/8s
     * threshold; past that cap it's better to fade immediately than wait almost a full loop. */
    private fun computeAlignmentDelayMs(): Long {
        val loopDuration = normalLoopDurationMs
        if (loopDuration == null || loopDuration <= 0) return 0L
        val elapsed = SystemClock.elapsedRealtime() - normalStartedAtElapsedMs
        val intoLoop = elapsed % loopDuration
        val untilBoundary = loopDuration - intoLoop
        return if (untilBoundary in 1 until MAX_ALIGNMENT_WAIT_MS) untilBoundary else 0L
    }

    /** Equal-power crossfade (volume updates every [STEP_MS] over [CROSSFADE_DURATION_MS]) so
     * perceived loudness stays constant throughout rather than dipping in the middle the way a
     * plain linear fade would - `cos`/`sin` sum to a constant total power at every step. Optionally
     * preceded by [computeAlignmentDelayMs]'s wait, during which [pendingFinalSwap] is true and the
     * outgoing track keeps playing unchanged (see class doc for why). */
    private fun beginCrossfade(fromStreamId: Int, toSampleId: Int, targetVolume: Float) {
        cancelCrossfade()
        val alignmentDelayMs = computeAlignmentDelayMs()
        pendingFinalSwap = alignmentDelayMs > 0
        pendingTargetSampleId = toSampleId
        crossfadeJob = scope.launch {
            if (alignmentDelayMs > 0) delay(alignmentDelayMs)
            pendingFinalSwap = false
            val toStreamId = playLoop(toSampleId, 0f)
            activeStreamId = toStreamId
            outgoingStreamId = fromStreamId
            val steps = (CROSSFADE_DURATION_MS / STEP_MS).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                val t = (step.toFloat() / steps) * (Math.PI.toFloat() / 2f)
                val outVolume = cos(t) * targetVolume
                val inVolume = sin(t) * targetVolume
                soundPool.setVolume(fromStreamId, outVolume, outVolume)
                soundPool.setVolume(toStreamId, inVolume, inVolume)
                delay(STEP_MS)
            }
            soundPool.setVolume(toStreamId, targetVolume, targetVolume)
            soundPool.stop(fromStreamId)
            outgoingStreamId = null
            crossfadeJob = null
        }
    }

    /** Called from [pause] - rather than leave a crossfade straddling an unpredictable pause
     * duration (an ad, backgrounding, the Privacy Shield), snap immediately to the fully-crossfaded
     * end state. Nothing is audible during the pause either way, so there's no perceptible
     * difference versus letting the fade finish "in the background" while paused - this is simply
     * the deterministic version of that. Two sub-cases: if the crossfade job was still in its
     * pre-swap alignment wait ([pendingFinalSwap]), [activeStreamId] is still the *outgoing* track,
     * so jump straight to [pendingTargetSampleId] instead of just restoring the current stream's
     * volume. */
    private fun finishCrossfadeImmediately() {
        if (crossfadeJob == null) return
        val wasPendingSwap = pendingFinalSwap
        val target = pendingTargetSampleId
        cancelCrossfade()
        pendingFinalSwap = false
        pendingTargetSampleId = null
        if (wasPendingSwap) {
            activeStreamId?.let { soundPool.stop(it) }
            activeStreamId = target?.let { playLoop(it, currentVolume) }
            return
        }
        outgoingStreamId?.let { soundPool.stop(it) }
        outgoingStreamId = null
        activeStreamId?.let { soundPool.setVolume(it, currentVolume, currentVolume) }
    }

    private fun cancelCrossfade() {
        crossfadeJob?.cancel()
        crossfadeJob = null
    }

    private fun stopActiveStreams() {
        activeStreamId?.let { soundPool.stop(it) }
        outgoingStreamId?.let { soundPool.stop(it) }
        activeStreamId = null
        outgoingStreamId = null
        pendingFinalSwap = false
        pendingTargetSampleId = null
    }

    private suspend fun loadRaw(resId: Int): Int? {
        val sampleId = runCatching { soundPool.load(context, resId, PRIORITY) }.getOrNull() ?: return null
        val ready = CompletableDeferred<Unit>()
        pendingLoads[sampleId] = ready
        withTimeoutOrNull(LOAD_TIMEOUT_MS) { ready.await() }
        return sampleId
    }

    /** Real playback duration of a `res/raw` file in ms, or null if it couldn't be read - used
     * only to align the crossfade to a loop boundary (see [computeAlignmentDelayMs]); a null result
     * (an unusual device/codec quirk) just means every crossfade falls back to firing immediately,
     * never a crash or a missing sound. */
    private fun measureDurationMs(resId: Int): Long? = runCatching {
        context.resources.openRawResourceFd(resId).use { afd ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }
    }.getOrNull()

    fun release() {
        cancelCrossfade()
        soundPool.release()
    }

    private companion object {
        const val MAX_CONCURRENT_STREAMS = 4
        const val PRIORITY = 1
        const val LOOP_FOREVER = -1
        const val LOAD_TIMEOUT_MS = 2_000L
        // Top of the spec's 80-150ms "imperceptible as two separate files" window - paired with
        // loop-boundary alignment above, the longer end reads smoother than 120ms did alone.
        const val CROSSFADE_DURATION_MS = 150L
        const val STEP_MS = 15L
        // Cap on how long computeAlignmentDelayMs will wait for a clean boundary before just
        // fading immediately instead - keeps the crossfade from meaningfully drifting away from
        // the requested exact 4s/8s remaining-time trigger.
        const val MAX_ALIGNMENT_WAIT_MS = 300L
    }
}
