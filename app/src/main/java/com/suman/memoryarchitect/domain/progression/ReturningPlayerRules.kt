package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.ReturningPlayerTier

/** Mirrors `mock-backend/progression.js`'s own copy of these same thresholds, the established
 * "keep Kotlin/JS in sync" convention already used for [StreakCalculator]/[DailyRewardCatalog].
 * Gaps are measured in whole days since [com.suman.memoryarchitect.domain.model.PlayerProfile.lastPlayedEpochDay] -
 * a `null` (never played) is never a "returning" player, just a new one. */
data class ReturningPlayerRules(
    val shortGapDays: Long = 3,
    val mediumGapDays: Long = 7,
    val longGapDays: Long = 30,
) {
    fun tierFor(gapDays: Long): ReturningPlayerTier = when {
        gapDays >= longGapDays -> ReturningPlayerTier.LONG
        gapDays >= mediumGapDays -> ReturningPlayerTier.MEDIUM
        gapDays >= shortGapDays -> ReturningPlayerTier.SHORT
        else -> ReturningPlayerTier.NONE
    }

    companion object {
        val Default = ReturningPlayerRules()
    }
}
