package com.suman.memoryarchitect.core.feedback.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.domain.model.SfxMaterialFamily
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
    // Keyed by (sfx, family), not just sfx - a themed OBJECT_MATERIAL variant set (see
    // SfxId.variantAssetPaths) needs its own loaded-sample-id list, independent of the untheme'd
    // baseline's. See loadIfNeeded's doc for how a missing themed asset falls back to the baseline
    // without a nested lock acquisition.
    private val soundIds = ConcurrentHashMap<Pair<SfxId, SfxMaterialFamily?>, List<Int>>()
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
            // Only the untheme'd baseline is preloaded eagerly - a themed OBJECT_MATERIAL variant
            // set (see loadIfNeeded's doc) loads lazily on first play(), same as any other
            // not-yet-preloaded sound in this class.
            for (sfx in SfxId.entries) loadIfNeeded(sfx, family = null)
        }
    }

    override fun play(sfx: SfxId, volume: Float, family: SfxMaterialFamily?) {
        if (volume <= 0f) return
        scope.launch {
            val ids = loadIfNeeded(sfx, family)
            if (ids.isEmpty()) return@launch
            val index = variantPicker.next(sfx to family, ids.size)
            soundPool.play(ids[index], volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f), 1, 0, 1f)
        }
    }

    /** A themed [family]'s variant set falls back to the untheme'd baseline's ids if none of its
     * own files exist yet (`loadOne` returns `null` per missing asset) - resolved and cached
     * inline here, never via a nested call back into this same [loadMutex]-guarded function
     * (Kotlin's [Mutex] isn't reentrant), so equipping a premium material before its sfx assets
     * are recorded never means silence, just the baseline sound. */
    private suspend fun loadIfNeeded(sfx: SfxId, family: SfxMaterialFamily?): List<Int> = loadMutex.withLock {
        val key = sfx to family
        soundIds[key]?.let { return@withLock it }
        val themedPaths = sfx.variantAssetPaths(family)
        var loaded = themedPaths.mapNotNull { path -> loadOne(path) }
        if (loaded.isEmpty() && family != null) {
            val baseKey = sfx to null
            loaded = soundIds[baseKey] ?: sfx.variantAssetPaths(null).mapNotNull { path -> loadOne(path) }.also { soundIds[baseKey] = it }
        }
        soundIds[key] = loaded
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
