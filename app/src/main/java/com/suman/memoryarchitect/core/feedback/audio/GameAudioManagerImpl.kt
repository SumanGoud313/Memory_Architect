package com.suman.memoryarchitect.core.feedback.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.suman.memoryarchitect.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays short one-shot sound effects from real asset files via [SoundPool] - the standard, low-
 * latency Android API for exactly this (many short clips, played frequently, sometimes
 * overlapping). [preload] loads every variant of every [SfxId] whose asset file actually exists up
 * front so the first real playback of any sound never pays a load-latency hit mid-gameplay;
 * anything without a file yet (see [AudioAssetManager]) is silently skipped rather than crashing or
 * substituting a generated tone.
 *
 * An [SfxId] with more than one file in [variantAssetPaths] (currently the 3 gameplay-object
 * sounds) plays a random variant each time, never immediately repeating the previous one - the
 * same "no consecutive repeat" pattern a real sound designer uses so a rapid pickup/place flurry
 * doesn't sound like one clip on a loop. An id with only its single [SfxId.assetPath] behaves
 * exactly as before.
 */
@Singleton
class GameAudioManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetManager: AudioAssetManager,
    private val dispatchers: DispatcherProvider,
) : GameAudioManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val loadMutex = Mutex()
    private val soundIds = ConcurrentHashMap<SfxId, List<Int>>()
    private val variantPicker = VariantPicker()
    private val pendingLoads = ConcurrentHashMap<Int, CompletableDeferred<Unit>>()

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
            for (sfx in SfxId.entries) loadIfNeeded(sfx)
        }
    }

    override fun play(sfx: SfxId, volume: Float) {
        if (volume <= 0f) return
        scope.launch {
            val ids = loadIfNeeded(sfx)
            if (ids.isEmpty()) return@launch
            val index = variantPicker.next(sfx, ids.size)
            soundPool.play(ids[index], volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f), 1, 0, 1f)
        }
    }

    private suspend fun loadIfNeeded(sfx: SfxId): List<Int> = loadMutex.withLock {
        soundIds[sfx]?.let { return@withLock it }
        val paths = sfx.variantAssetPaths()
        val loaded = paths.mapNotNull { path -> loadOne(path) }
        soundIds[sfx] = loaded
        loaded
    }

    private suspend fun loadOne(path: String): Int? {
        if (!assetManager.assetExists(path)) return null // no asset file yet - silent no-op, never a fallback tone
        val afd = runCatching { context.assets.openFd("audio/$path") }.getOrNull() ?: return null
        val sampleId = afd.use { soundPool.load(it, 1) }
        val ready = CompletableDeferred<Unit>()
        pendingLoads[sampleId] = ready
        withTimeoutOrNull(LOAD_TIMEOUT_MS) { ready.await() }
        return sampleId
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val MAX_CONCURRENT_STREAMS = 8
        const val LOAD_TIMEOUT_MS = 2_000L
    }
}
