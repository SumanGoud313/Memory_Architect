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
    suspend fun recordHintUsed(levelNumber: Int)

    /** A rewarded-ad bonus hint is modeled as refunding one real use, rather than a separate
     * counter - it reuses the exact same persisted field, so it needs no schema of its own and
     * survives a level reload/process death exactly like a normal hint's usage does. */
    suspend fun grantBonusHint(levelNumber: Int)

    /** Clears this level's usage back to zero - called once at the start of every fresh attempt,
     * never mid-round (a live session resumes via [getHintsUsed], not this). */
    suspend fun resetHintUsage(levelNumber: Int)
}
