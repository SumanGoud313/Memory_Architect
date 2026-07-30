package com.suman.memoryarchitect.domain.model

/** The result of successfully claiming one ad-gated Mystery Chest - see [MysteryChestAdState]'s
 * doc. [inventory] is the full, post-grant balance (same "whole snapshot back, not just a delta"
 * shape [com.suman.memoryarchitect.domain.repository.InventoryRepository]'s methods already
 * return), so the caller can cache it directly with no separate re-fetch. */
data class MysteryChestAdClaimResult(
    val inventory: Inventory,
    val claimState: MysteryChestAdState,
)
