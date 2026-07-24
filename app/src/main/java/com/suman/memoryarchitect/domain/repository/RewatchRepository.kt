package com.suman.memoryarchitect.domain.repository

/**
 * Local-only, like [HintRepository]/[RedoRepository] - rewatch usage has no server counterpart.
 * Now backs a small free tier too (see [com.suman.memoryarchitect.domain.model.RewatchRules],
 * levels 50+) using this exact same per-level count, the same way [HintRepository]/[RedoRepository]
 * already gate their own free tiers - watching the rewarded ad itself stays unlimited either way.
 */
interface RewatchRepository {
    suspend fun getRewatchesUsed(levelNumber: Int): Int
    suspend fun recordRewatchUsed(levelNumber: Int)

    /** Clears this level's usage back to zero - called once at the start of every fresh attempt,
     * mirroring [com.suman.memoryarchitect.domain.repository.HintRepository.resetHintUsage]. */
    suspend fun resetRewatchUsage(levelNumber: Int)
}
