package com.suman.memoryarchitect.domain.repository

/**
 * Local-only, like [HintRepository] - redo usage has no server counterpart. Usage is keyed by
 * level number and persists for the life of one attempt at that level, but [resetRedoUsage]
 * clears it back to zero at the start of every fresh attempt, mirroring
 * [HintRepository.resetHintUsage] - retrying or replaying a level always starts with a full
 * budget again.
 */
interface RedoRepository {
    suspend fun getRedosUsed(levelNumber: Int): Int

    /** How many rewarded-ad grants have already been redeemed for this level attempt - capped by
     * [com.suman.memoryarchitect.domain.model.RewardedAssistLimits.maxRewardedRedos], independent
     * of the free-tier count [getRedosUsed] tracks. Resets on the same schedule as [getRedosUsed]
     * (see [resetRedoUsage]) - never carries across levels. */
    suspend fun getRewardedRedosUsed(levelNumber: Int): Int

    suspend fun recordRedoUsed(levelNumber: Int)

    /** A rewarded-ad bonus redo refunds one real use (reusing the exact same persisted
     * [getRedosUsed] field) while also incrementing [getRewardedRedosUsed]'s own counter, which is
     * what actually caps how many times this can happen per level, mirroring
     * [com.suman.memoryarchitect.domain.repository.HintRepository.grantBonusHint]. */
    suspend fun grantBonusRedo(levelNumber: Int)

    /** Clears this level's usage back to zero - called once at the start of every fresh attempt,
     * never mid-round (a live session resumes via [getRedosUsed], not this). */
    suspend fun resetRedoUsage(levelNumber: Int)
}
