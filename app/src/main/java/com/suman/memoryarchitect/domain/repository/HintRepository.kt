package com.suman.memoryarchitect.domain.repository

/**
 * Local-only, like [LevelCampaignRepository] - hint usage has no server counterpart. Usage is
 * keyed by level number and persists for the life of one attempt at that level - surviving a
 * process death or backgrounding mid-round - but [resetHintUsage] clears it back to zero at the
 * start of every fresh attempt (see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]'s
 * `loadLevel`), so retrying or replaying a level always starts with a full budget again.
 */
interface HintRepository {
    suspend fun getHintsUsed(levelNumber: Int): Int

    /** How many rewarded-ad grants have already been redeemed for this level attempt - capped by
     * [com.suman.memoryarchitect.domain.model.RewardedAssistLimits.maxRewardedHints], independent
     * of the free-tier count [getHintsUsed] tracks. Resets to zero on the same "fresh attempt"
     * schedule as [getHintsUsed] (see [resetHintUsage]) - never carries across levels. */
    suspend fun getRewardedHintsUsed(levelNumber: Int): Int

    suspend fun recordHintUsed(levelNumber: Int)

    /** A rewarded-ad bonus hint refunds one real use (reusing the exact same persisted
     * [getHintsUsed] field, so the granted hint needs no separate "spend" step) while also
     * incrementing [getRewardedHintsUsed]'s own counter, which is what actually caps how many
     * times this can happen per level. */
    suspend fun grantBonusHint(levelNumber: Int)

    /** Clears this level's usage back to zero - called once at the start of every fresh attempt,
     * never mid-round (a live session resumes via [getHintsUsed], not this). */
    suspend fun resetHintUsage(levelNumber: Int)
}
