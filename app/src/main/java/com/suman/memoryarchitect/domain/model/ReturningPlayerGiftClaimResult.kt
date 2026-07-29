package com.suman.memoryarchitect.domain.model

/** The outcome of [com.suman.memoryarchitect.domain.repository.ProgressionRepository.claimReturningPlayerGift] -
 * a single Mystery Chest into Inventory, never coins/xp (this is a welcome-back gift, not a
 * progression reward - see the retention plan's "never claw back, but also never a second economy"
 * framing). [inventory] is the player's full balance after the grant, the same "never a separate
 * re-fetch" shape [MissionClaimResult.inventory]/[DailyRewardClaimResult.inventory] already use. */
data class ReturningPlayerGiftClaimResult(
    val inventory: Inventory,
)
