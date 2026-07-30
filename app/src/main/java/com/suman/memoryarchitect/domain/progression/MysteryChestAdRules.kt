package com.suman.memoryarchitect.domain.progression

/**
 * Data-driven tuning for the ad-gated Mystery Chest claim shown on
 * [com.suman.memoryarchitect.ui.screens.shop.LuckySpinScreen], right alongside the spin wheel -
 * mirrors [SpinRules]'s "balancing is a data change" philosophy. Up to [maxClaimsPerDay] rewarded-
 * ad watches per day, each granting exactly one
 * [com.suman.memoryarchitect.domain.model.InventoryItemKind.MYSTERY_CHEST] to Inventory (opening
 * one for its coin reward is [MysteryChestOdds]'s job, unrelated to earning one in the first
 * place). Deliberately watch-ad-only - unlike Lucky Spin itself, there is no free/ticket
 * alternative here, matching the request this was built for verbatim ("only watch ad to get one
 * mystery box").
 */
data class MysteryChestAdRules(
    val maxClaimsPerDay: Int = 3,
) {
    companion object {
        val Default = MysteryChestAdRules()
    }
}
