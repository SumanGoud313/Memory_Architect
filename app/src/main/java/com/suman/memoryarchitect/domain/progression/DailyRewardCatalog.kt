package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.DailyRewardKind

data class DailyRewardEntry(
    val day: Int,
    val coins: Long,
    val xp: Long,
    val kind: DailyRewardKind = DailyRewardKind.COINS,
    /** True for the one day per cycle whose exact amount is hidden in the check-in calendar until
     * claimed - the "mystery" is deliberately about *when you learn it*, not about randomness: the
     * amount below is fixed, same as every other day, so client-optimistic and server-authoritative
     * values can never disagree. */
    val isMysteryChest: Boolean = false,
    /** True for the one day per cycle (the finale, day 7) that also grants a Streak Shield on top
     * of its coins/xp - subject to [com.suman.memoryarchitect.domain.model.StreakRules.maxStoredShields]
     * the same way a streak-milestone shield grant is (see [StreakCalculator]). */
    val bonusShield: Boolean = false,
)

/**
 * Mirrors mock-backend/progression.js's `DAILY_REWARD_TABLE` — keep both in sync. A 7-day cycle,
 * deliberately modest and non-escalating-to-absurd (no currency you'd ever feel pressured to top
 * up), where a missed day never punishes: [nextCycleDay] quietly restarts at day 1 rather than
 * losing anything already banked — the same "no punishment, only forward progress" spirit as
 * [StreakCalculator].
 *
 * Every day still adds up to the exact same lifetime value this cycle always has - day 5 simply
 * wraps its existing amount in a Mystery Chest reveal, and day 7's existing amount now also comes
 * with a Streak Shield, so returning daily stays worthwhile without the cycle becoming a second,
 * bigger economy of its own.
 */
object DailyRewardCatalog {
    val entries: List<DailyRewardEntry> = listOf(
        DailyRewardEntry(day = 1, coins = 40, xp = 0),
        DailyRewardEntry(day = 2, coins = 60, xp = 0),
        DailyRewardEntry(day = 3, coins = 80, xp = 20, kind = DailyRewardKind.XP),
        DailyRewardEntry(day = 4, coins = 100, xp = 0),
        DailyRewardEntry(day = 5, coins = 130, xp = 30, kind = DailyRewardKind.MYSTERY_CHEST, isMysteryChest = true),
        DailyRewardEntry(day = 6, coins = 160, xp = 0),
        DailyRewardEntry(day = 7, coins = 250, xp = 75, kind = DailyRewardKind.XP, bonusShield = true),
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
