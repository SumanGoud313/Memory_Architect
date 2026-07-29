package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.DailyRewardKind
import com.suman.memoryarchitect.domain.model.InventoryItemKind

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
    /** Layered on top of [coins]/[xp] the same way [bonusShield] is - see this object's own doc for
     * why these were held back until [com.suman.memoryarchitect.domain.repository.InventoryRepository]
     * existed to be their redemption home. */
    val inventoryGrants: Map<InventoryItemKind, Int> = emptyMap(),
)

/**
 * Mirrors mock-backend/progression.js's `DAILY_REWARD_TABLE` — keep both in sync. A 7-day cycle,
 * deliberately modest and non-escalating-to-absurd (no currency you'd ever feel pressured to top
 * up), where a missed day never punishes: [nextCycleDay] quietly restarts at day 1 rather than
 * losing anything already banked — the same "no punishment, only forward progress" spirit as
 * [StreakCalculator].
 *
 * Every day still adds up to the exact same coins/xp lifetime value this cycle always has - day 5
 * simply wraps its existing amount in a Mystery Chest reveal, and day 7's existing amount now also
 * comes with a Streak Shield, so returning daily stays worthwhile without the cycle becoming a
 * second, bigger economy of its own. [DailyRewardEntry.inventoryGrants] layers small, fixed
 * Inventory items on top of four of the seven days the same way - never a substitute for the
 * coins/xp already there, only an addition.
 */
object DailyRewardCatalog {
    val entries: List<DailyRewardEntry> = listOf(
        DailyRewardEntry(day = 1, coins = 40, xp = 0),
        DailyRewardEntry(day = 2, coins = 60, xp = 0, inventoryGrants = mapOf(InventoryItemKind.HINT_TOKEN to 1)),
        DailyRewardEntry(day = 3, coins = 80, xp = 20, kind = DailyRewardKind.XP),
        DailyRewardEntry(day = 4, coins = 100, xp = 0, inventoryGrants = mapOf(InventoryItemKind.REDO_TOKEN to 1)),
        DailyRewardEntry(
            day = 5, coins = 130, xp = 30, kind = DailyRewardKind.MYSTERY_CHEST, isMysteryChest = true,
            inventoryGrants = mapOf(InventoryItemKind.MYSTERY_CHEST to 1),
        ),
        DailyRewardEntry(day = 6, coins = 160, xp = 0, inventoryGrants = mapOf(InventoryItemKind.REWATCH_TICKET to 1)),
        DailyRewardEntry(
            day = 7, coins = 250, xp = 75, kind = DailyRewardKind.XP, bonusShield = true,
            inventoryGrants = mapOf(InventoryItemKind.LUCKY_SPIN_TICKET to 1),
        ),
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
