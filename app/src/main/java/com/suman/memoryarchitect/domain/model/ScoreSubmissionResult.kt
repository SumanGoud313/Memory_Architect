package com.suman.memoryarchitect.domain.model

/**
 * [isPendingSync] is true when the submission couldn't reach the server and was queued
 * locally instead — [profile] is then an optimistic client-side estimate, not yet
 * confirmed by the server's authoritative recompute. [statistics], [newlyUnlockedAchievements]
 * and [newlyUnlockedRewards] are evaluated against local data regardless of server
 * reachability — they're recognition, not a competitive/anti-cheat concern.
 *
 * [streakMilestoneReached]/[streakShieldGranted]/[streakShieldConsumed] are read straight off this
 * same round's [com.suman.memoryarchitect.domain.model.StreakUpdateResult] (computed client-side
 * either way, same "recognition" reasoning as the achievement/reward fields above - the persisted
 * [PlayerProfile.streakShields] count itself is still server-authoritative, only the "did this
 * exact call just cross a milestone" notification is inferred locally), never re-derived by
 * diffing [profile] against a previous snapshot.
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
    val streakMilestoneReached: Int? = null,
    val streakShieldGranted: Boolean = false,
    val streakShieldConsumed: Boolean = false,
    /** How many Memory Journey points this exact round granted - see
     * [com.suman.memoryarchitect.domain.model.MemoryJourneyRules]. Purely informational (the
     * persisted [PlayerProfile.journeyPoints] total is what actually matters); `0` for Practice,
     * which never earns any. */
    val journeyPointsAwarded: Long = 0L,
    /** The [com.suman.memoryarchitect.domain.model.MemoryJourneyTierId] just crossed this round,
     * or null - computed by comparing the pre-round total against [profile.journeyPoints], the
     * same "only on the real transition" guarantee [streakMilestoneReached] already documents. */
    val journeyTierReached: MemoryJourneyTierId? = null,
)
