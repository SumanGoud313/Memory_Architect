package com.suman.memoryarchitect.domain.model

/**
 * Tuning for [com.suman.memoryarchitect.domain.progression.StreakCalculator] - mirrors
 * `mock-backend/progression.js`'s own copy of these same numbers, the established "keep
 * Kotlin/JS in sync" convention already used for [com.suman.memoryarchitect.domain.progression.ProgressionRules]
 * and [com.suman.memoryarchitect.domain.progression.DailyRewardCatalog].
 *
 * [milestoneDays] is the full celebration timeline shown on Profile; [shieldMilestoneDays] is the
 * subset ("major" milestones) that also grants a Streak Shield, capped at [maxStoredShields] so
 * shields stay a rare, earned safety net rather than something to stockpile indefinitely - a
 * milestone crossed while already holding the cap simply doesn't grant another.
 */
data class StreakRules(
    val milestoneDays: List<Int> = listOf(3, 7, 14, 30, 60, 90, 180, 365),
    val shieldMilestoneDays: Set<Int> = setOf(7, 30, 90, 365),
    val maxStoredShields: Int = 3,
) {
    companion object {
        val Default = StreakRules()
    }
}
