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
    suspend fun recordRedoUsed(levelNumber: Int)

    /** A rewarded-ad bonus redo is modeled as refunding one real use, rather than a separate
     * counter - it reuses the exact same persisted field, so it needs no schema of its own,
     * mirroring [com.suman.memoryarchitect.domain.repository.HintRepository.grantBonusHint]. */
    suspend fun grantBonusRedo(levelNumber: Int)

    /** Clears this level's usage back to zero - called once at the start of every fresh attempt,
     * never mid-round (a live session resumes via [getRedosUsed], not this). */
    suspend fun resetRedoUsage(levelNumber: Int)
}
