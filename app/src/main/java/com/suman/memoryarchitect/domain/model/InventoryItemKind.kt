package com.suman.memoryarchitect.domain.model

/**
 * The permanent home for every earned consumable that previously had nowhere to live - see
 * [DailyRewardKind]'s doc for why these were deliberately left out of the check-in pool until this
 * existed. Each is a plain count, server-mirrored the same way [PlayerProfile.streakShields] is
 * (see [com.suman.memoryarchitect.domain.repository.InventoryRepository]).
 *
 * [MYSTERY_CHEST] is granted and stored here but not yet *opened* - that's
 * [com.suman.memoryarchitect.domain.progression.MysteryChestOdds], a later phase's job. A chest
 * sitting unopened in Inventory today is expected, not a bug.
 *
 * Streak Shields are deliberately not modeled here - they already have their own dedicated,
 * anti-cheat-bounded field ([PlayerProfile.streakShields]) predating Inventory, and moving them
 * would be a pure refactor with no behavior change, so they stay where they are.
 */
enum class InventoryItemKind {
    HINT_TOKEN,
    REDO_TOKEN,
    REWATCH_TICKET,
    LUCKY_SPIN_TICKET,
    XP_BOOST,
    DISCOUNT_COUPON,
    MYSTERY_CHEST,
}
