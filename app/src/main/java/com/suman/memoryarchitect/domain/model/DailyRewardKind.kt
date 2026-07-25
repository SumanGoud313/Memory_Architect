package com.suman.memoryarchitect.domain.model

/**
 * What flavor a [com.suman.memoryarchitect.domain.progression.DailyRewardEntry] mainly reads as,
 * purely for the check-in calendar's display (icon, and whether the exact amount is hidden until
 * claimed) - the actual grant is always coins/xp/a shield, computed the same way regardless of
 * [kind][DailyRewardEntry.kind].
 *
 * [COINS]/[XP] are today's only two "revealed up front" kinds. [MYSTERY_CHEST] hides its exact
 * amount until claimed (see [DailyRewardEntry.isMysteryChest]) for surprise/anticipation without
 * any actual randomness - the amount is fixed, only *when you learn it* changes.
 *
 * Hint/Redo/Rewatch Tokens, Lucky Spin Tickets, Cosmetic Fragments, and Discount Coupons are
 * deliberately not modeled here yet - each needs a redemption system (Inventory, Fragment
 * assembly) that doesn't exist in this codebase yet. Granting one of those today would be a
 * reward with nowhere to spend it, so they wait for the phase that builds their consumer.
 */
enum class DailyRewardKind {
    COINS,
    XP,
    MYSTERY_CHEST,
}
