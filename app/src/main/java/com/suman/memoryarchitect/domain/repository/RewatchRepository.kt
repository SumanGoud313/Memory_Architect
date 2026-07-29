package com.suman.memoryarchitect.domain.repository

/**
 * Local-only, like [HintRepository]/[RedoRepository] - rewatch usage has no server counterpart.
 * Now backs a small free tier too (see [com.suman.memoryarchitect.domain.model.RewatchRules],
 * levels 50+) using this exact same per-level count, the same way [HintRepository]/[RedoRepository]
 * already gate their own free tiers - watching the rewarded ad itself stays unlimited either way.
 */
interface RewatchRepository {
    suspend fun getRewatchesUsed(levelNumber: Int): Int

    /** How many rewarded-ad rewatches have already been redeemed for this level attempt - capped
     * by [com.suman.memoryarchitect.domain.model.RewardedAssistLimits.maxRewardedRewatches],
     * tracked independently of [getRewatchesUsed] (which also covers any future free tier - see
     * [com.suman.memoryarchitect.domain.model.RewatchRules]). Resets on the same schedule as
     * [getRewatchesUsed] (see [resetRewatchUsage]) - never carries across levels. */
    suspend fun getRewardedRewatchesUsed(levelNumber: Int): Int

    suspend fun recordRewatchUsed(levelNumber: Int)

    /** Records one rewarded-ad-granted rewatch specifically - separate from [recordRewatchUsed]
     * (which a future free tier would also call) so the two counters, and their two independent
     * caps, can never conflate a free use with an ad-gated one. */
    suspend fun recordRewardedRewatchUsed(levelNumber: Int)

    /** Clears this level's usage back to zero - called once at the start of every fresh attempt,
     * mirroring [com.suman.memoryarchitect.domain.repository.HintRepository.resetHintUsage]. */
    suspend fun resetRewatchUsage(levelNumber: Int)
}
