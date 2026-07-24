package com.suman.memoryarchitect.domain.generation

import kotlin.random.Random

/** A per-level atmospheric variant layered over the room's normal look — see
 * [com.suman.memoryarchitect.ui.components.LightingOverlay] for the actual rendering. */
enum class LightingMood { DAY, NIGHT, RAINY, SUNSET }

/**
 * Picks [LightingMood] from [seed] — its own isolated [Random] draw, same reasoning as
 * [LevelMirrorPolicy]: never perturbs [LevelGenerator]'s own random sequence. Weighted so the
 * room's normal warm look ([LightingMood.DAY]) stays the common case and the atmospheric
 * variants read as an occasional treat, not a constant distraction from the memorize task.
 */
object LightingMoodPolicy {
    private const val DAY_WEIGHT = 55
    private const val NIGHT_WEIGHT = 15
    private const val RAINY_WEIGHT = 15
    // SUNSET gets the remaining 15.

    fun forSeed(seed: Long): LightingMood {
        val roll = Random(seed xor MOOD_SALT).nextInt(100)
        return when {
            roll < DAY_WEIGHT -> LightingMood.DAY
            roll < DAY_WEIGHT + NIGHT_WEIGHT -> LightingMood.NIGHT
            roll < DAY_WEIGHT + NIGHT_WEIGHT + RAINY_WEIGHT -> LightingMood.RAINY
            else -> LightingMood.SUNSET
        }
    }

    /** XORed into the seed before drawing so this Random stream never lines up 1:1 with
     * [LevelMirrorPolicy]'s (both would otherwise draw from the identical seed). */
    private const val MOOD_SALT = 0x5A17L
}
