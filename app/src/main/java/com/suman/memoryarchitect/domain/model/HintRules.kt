package com.suman.memoryarchitect.domain.model

/**
 * How many hints a level grants, keyed purely by level number. Levels 1-24 grant exactly 1 free
 * hint per attempt; levels 25-100 grant exactly 3. Every attempt (a fresh level, or a retry of
 * the same level number) gets this full budget again - see `HintUsageDao`, which keys usage by
 * level number, never a global running total.
 */
data class HintRules(
    val tierOneMaxLevel: Int = 24,
    val hintsForTierOne: Int = 1,
    val hintsForTierTwo: Int = 3,
) {
    fun hintsAllowedForLevel(levelNumber: Int): Int =
        if (levelNumber <= tierOneMaxLevel) hintsForTierOne else hintsForTierTwo

    companion object {
        val Default = HintRules()
    }
}
