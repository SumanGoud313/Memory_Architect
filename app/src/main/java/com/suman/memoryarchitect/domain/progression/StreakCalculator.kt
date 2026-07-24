package com.suman.memoryarchitect.domain.progression

/** Pure day-based streak update, given epoch days so callers control "today" for testability. */
class StreakCalculator {

    fun updateStreak(
        lastPlayedEpochDay: Long?,
        todayEpochDay: Long,
        previousCurrentStreak: Int,
        previousLongestStreak: Int,
    ): Pair<Int, Int> {
        val newCurrent = when (lastPlayedEpochDay) {
            null -> 1
            todayEpochDay -> previousCurrentStreak.coerceAtLeast(1)
            todayEpochDay - 1 -> previousCurrentStreak + 1
            else -> 1
        }
        val newLongest = maxOf(previousLongestStreak, newCurrent)
        return newCurrent to newLongest
    }
}
