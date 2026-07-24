package com.suman.memoryarchitect.domain.model

/**
 * [isPendingSync] is true when the submission couldn't reach the server and was queued
 * locally instead — [profile] is then an optimistic client-side estimate, not yet
 * confirmed by the server's authoritative recompute. [statistics], [newlyUnlockedAchievements]
 * and [newlyUnlockedRewards] are evaluated against local data regardless of server
 * reachability — they're recognition, not a competitive/anti-cheat concern.
 */
data class ScoreSubmissionResult(
    val profile: PlayerProfile,
    val xpAwarded: Long,
    val coinsAwarded: Long,
    val leveledUp: Boolean,
    val isPendingSync: Boolean,
    val statistics: PlayerStatistics,
    val newlyUnlockedAchievements: List<AchievementId>,
    val newlyUnlockedRewards: List<RewardDefinition> = emptyList(),
)
