package com.suman.memoryarchitect.domain.model

/**
 * How many redos (undo last placement) a level grants, keyed purely by level number - same
 * tiering as [HintRules]: levels 1-24 grant exactly 1 free redo per attempt, levels 25-100 grant
 * exactly 3. Fixed per level number, never carried over between levels (see `RedoUsageDao`).
 */
data class RedoRules(
    val tierOneMaxLevel: Int = 24,
    val redosForTierOne: Int = 1,
    val redosForTierTwo: Int = 3,
) {
    fun redosAllowedForLevel(levelNumber: Int): Int =
        if (levelNumber <= tierOneMaxLevel) redosForTierOne else redosForTierTwo

    companion object {
        val Default = RedoRules()
    }
}
