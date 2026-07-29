package com.suman.memoryarchitect.domain.model

data class DailyRewardClaimResult(
    val cycleDay: Int,
    val coinsAwarded: Long,
    val xpAwarded: Long,
    val profile: PlayerProfile,
    /** True only when today's cycle day both grants a bonus Streak Shield (see
     * [com.suman.memoryarchitect.domain.progression.DailyRewardCatalog]'s `bonusShield` entries)
     * AND the player wasn't already holding the cap - a claim at the cap simply grants coins/xp
     * with no shield, same "cap silently absorbs it" behavior as a streak-milestone shield grant. */
    val shieldAwarded: Boolean = false,
    /** The player's full Inventory balance after this claim's
     * [com.suman.memoryarchitect.domain.progression.DailyRewardEntry.inventoryGrants] (if any) were
     * applied - the same "never a separate re-fetch" shape [MissionClaimResult.inventory] already
     * uses, not just today's delta. */
    val inventory: Inventory = Inventory.EMPTY,
)
