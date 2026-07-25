package com.suman.memoryarchitect.domain.model

/**
 * The full outcome of one [com.suman.memoryarchitect.domain.progression.StreakCalculator.updateStreak]
 * call - not just the new streak length, but every event that call may have just caused, so a
 * caller (see `ProgressionRepositoryImpl.submitScore`/`FirestoreProgressionRemoteSource.submitScore`)
 * never has to separately diff before/after state to notice a shield was spent, earned, or a
 * milestone was just crossed - this is computed once, at the source.
 */
data class StreakUpdateResult(
    val currentStreak: Int,
    val longestStreak: Int,
    /** The player's Streak Shield balance after this call - authoritative, written straight back
     * to [PlayerProfile.streakShields]. */
    val streakShields: Int,
    /** True only on the exact call that spent a shield to cover a single missed day (a two-day
     * gap with at least one shield banked) - never true again for the same gap on a later call. */
    val shieldConsumed: Boolean,
    /** True only on the exact call whose streak increase crosses one of
     * [StreakRules.shieldMilestoneDays] and the shield balance was below [StreakRules.maxStoredShields]
     * at the time - never true on a same-day resubmission that leaves the streak unchanged. */
    val shieldGranted: Boolean,
    /** The [StreakRules.milestoneDays] entry just reached this call, or null - same "only on the
     * real transition, never a same-day resubmission" guarantee as [shieldGranted]. */
    val milestoneReached: Int?,
)
