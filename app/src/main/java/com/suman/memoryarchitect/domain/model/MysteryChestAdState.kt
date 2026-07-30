package com.suman.memoryarchitect.domain.model

/**
 * The ad-gated Mystery Chest claim's own daily-gate state - deliberately its own small server-
 * authoritative document, same "Shop keeps its own small per-concern documents" convention
 * [LuckySpinState]'s doc already establishes, kept separate from that class even though both
 * gate features on the same screen: a Mystery Chest claim is a distinct daily allowance from
 * spinning the wheel, with its own cap (see
 * [com.suman.memoryarchitect.domain.progression.MysteryChestAdRules]), so folding it into
 * [LuckySpinState] would make the two allowances impossible to reason about independently.
 */
data class MysteryChestAdState(
    /** Which epoch-day [claimsUsedToday] counts against - same "only applies when this equals
     * today" shape [LuckySpinState.lastAdSpinEpochDay] already has. */
    val lastClaimEpochDay: Long? = null,
    /** How many of [com.suman.memoryarchitect.domain.progression.MysteryChestAdRules.maxClaimsPerDay]'s
     * ad-gated claims have been spent on [lastClaimEpochDay] - only meaningful when that field
     * equals "today". */
    val claimsUsedToday: Int = 0,
) {
    companion object {
        val EMPTY = MysteryChestAdState()
    }
}
