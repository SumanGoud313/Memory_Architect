package com.suman.memoryarchitect.domain.progression

data class DailyRewardEntry(val day: Int, val coins: Long, val xp: Long)

/**
 * Mirrors mock-backend/progression.js's `DAILY_REWARD_TABLE` — keep both in sync. A 7-day cycle,
 * deliberately modest and non-escalating-to-absurd (no currency you'd ever feel pressured to top
 * up), where a missed day never punishes: [nextCycleDay] quietly restarts at day 1 rather than
 * losing anything already banked — the same "no punishment, only forward progress" spirit as
 * [StreakCalculator].
 */
object DailyRewardCatalog {
    val entries: List<DailyRewardEntry> = listOf(
        DailyRewardEntry(day = 1, coins = 40, xp = 0),
        DailyRewardEntry(day = 2, coins = 60, xp = 0),
        DailyRewardEntry(day = 3, coins = 80, xp = 20),
        DailyRewardEntry(day = 4, coins = 100, xp = 0),
        DailyRewardEntry(day = 5, coins = 130, xp = 30),
        DailyRewardEntry(day = 6, coins = 160, xp = 0),
        DailyRewardEntry(day = 7, coins = 250, xp = 75),
    )

    fun entryForDay(day: Int): DailyRewardEntry = entries[(day - 1).coerceIn(0, entries.lastIndex)]

    fun nextCycleDay(lastClaimedEpochDay: Long?, currentCycleDay: Int, todayEpochDay: Long): Int = when {
        lastClaimedEpochDay == null -> 1
        lastClaimedEpochDay == todayEpochDay -> currentCycleDay
        lastClaimedEpochDay == todayEpochDay - 1 -> if (currentCycleDay >= entries.size) 1 else currentCycleDay + 1
        else -> 1
    }

    fun canClaim(lastClaimedEpochDay: Long?, todayEpochDay: Long): Boolean = lastClaimedEpochDay != todayEpochDay
}
