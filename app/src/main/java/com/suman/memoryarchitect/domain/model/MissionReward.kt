package com.suman.memoryarchitect.domain.model

/** What claiming a completed [MissionDefinition] grants - always at least coins, everything else
 * optional. Kept as a single shape (rather than a sealed hierarchy) since a claim can plausibly
 * grant more than one thing at once (e.g. a Weekly Mission paying both coins and a Lucky Spin
 * Ticket) - see the plan doc's "3 active Weekly Missions... rewarding exclusive cosmetic fragments
 * and a guaranteed Lucky Spin Ticket or two." Cosmetic fragments are a later phase's job and are
 * deliberately not modeled here yet, same reasoning as [InventoryItemKind]'s doc. */
data class MissionReward(
    val coins: Long = 0L,
    val xp: Long = 0L,
    val inventoryGrants: Map<InventoryItemKind, Int> = emptyMap(),
)
