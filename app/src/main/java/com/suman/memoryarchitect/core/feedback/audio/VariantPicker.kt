package com.suman.memoryarchitect.core.feedback.audio

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Picks a random index into a same-sound variant list, never immediately repeating the index it
 * handed out last time for that same key - the "no consecutive repeat" rule behind [SfxId]'s
 * pickup/rotate/place variety. Pulled out of [GameAudioManagerImpl] as its own pure class (no
 * [android.media.SoundPool]/Context dependency) purely so this small but easy-to-get-wrong
 * algorithm is unit-testable on the plain JVM test runner this project uses - everything else in
 * that class touches real Android audio APIs and can't be.
 */
class VariantPicker(private val random: Random = Random.Default) {
    private val lastIndex = ConcurrentHashMap<Any, Int>()

    /** Returns 0 immediately for [variantCount] <= 1 without touching [random] or [lastIndex] -
     * a single-variant [key] behaves identically to having no variant system at all. */
    fun next(key: Any, variantCount: Int): Int {
        if (variantCount <= 1) return 0
        val previous = lastIndex[key]
        var candidate = random.nextInt(variantCount)
        if (candidate == previous) candidate = (candidate + 1) % variantCount
        lastIndex[key] = candidate
        return candidate
    }
}
