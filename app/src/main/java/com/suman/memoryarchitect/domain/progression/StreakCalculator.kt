package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.StreakRules
import com.suman.memoryarchitect.domain.model.StreakUpdateResult

/**
 * Pure day-based streak update, given epoch days so callers control "today" for testability.
 * Mirrors `mock-backend/progression.js`'s `updateStreak` - keep both in sync.
 *
 * A same-day resubmission (already played today) leaves the streak untouched and never re-fires
 * [StreakUpdateResult.milestoneReached]/`shieldGranted` - those only ever fire on the exact call
 * whose streak value actually changes, never on every subsequent round played the same day.
 *
 * A one-day gap (missed exactly one day) is no longer an automatic reset: if the player is
 * holding at least one Streak Shield, it's spent automatically to cover the gap and the streak
 * extends as if the day hadn't been missed - see [StreakUpdateResult.shieldConsumed]. A gap of two
 * or more days, or a one-day gap with no shield banked, resets to 1 exactly as before.
 */
class StreakCalculator(private val rules: StreakRules = StreakRules.Default) {

    fun updateStreak(
        lastPlayedEpochDay: Long?,
        todayEpochDay: Long,
        previousCurrentStreak: Int,
        previousLongestStreak: Int,
        previousStreakShields: Int,
    ): StreakUpdateResult {
        val gapDays = lastPlayedEpochDay?.let { todayEpochDay - it }
        var shields = previousStreakShields
        var shieldConsumed = false

        val (newCurrent, isFreshDay) = when {
            lastPlayedEpochDay == null -> 1 to true
            gapDays == 0L -> previousCurrentStreak.coerceAtLeast(1) to false
            gapDays == 1L -> (previousCurrentStreak + 1) to true
            gapDays == 2L && shields > 0 -> {
                shields -= 1
                shieldConsumed = true
                (previousCurrentStreak + 1) to true
            }
            else -> 1 to true
        }
        val newLongest = maxOf(previousLongestStreak, newCurrent)

        // Only ever evaluated on a call that actually moved the streak forward (isFreshDay) - a
        // same-day resubmission sitting at an already-reached milestone value must never re-grant.
        val milestoneReached = if (isFreshDay) rules.milestoneDays.firstOrNull { it == newCurrent } else null
        val shieldGranted = milestoneReached != null &&
            milestoneReached in rules.shieldMilestoneDays &&
            shields < rules.maxStoredShields
        if (shieldGranted) shields += 1

        return StreakUpdateResult(
            currentStreak = newCurrent,
            longestStreak = newLongest,
            streakShields = shields,
            shieldConsumed = shieldConsumed,
            shieldGranted = shieldGranted,
            milestoneReached = milestoneReached,
        )
    }
}
