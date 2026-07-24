package com.suman.memoryarchitect.domain.model

/** A Point Shop cosmetic slot. Deliberately disjoint from [RewardKind] - [RewardKind]'s
 * ROOM_THEME/PALETTE/TITLE/BADGE are free, level-milestone unlocks with no shop involvement at
 * all (see [com.suman.memoryarchitect.domain.progression.RewardCatalog]); these categories are
 * new, coin-purchasable cosmetics that never overlap with that system. */
enum class CosmeticCategory {
    AVATAR_FRAME,
    PROFILE_BORDER,
    NAME_COLOR,
    TIMER_STYLE,
    VICTORY_ANIMATION,
    CONFETTI_EFFECT,
    STICKER_PACK,
    TROPHY_RELIC,

    /** Recolors [com.suman.memoryarchitect.ui.components.AmbientBackground]'s gradient/glow/particle
     * palette - the backdrop of every top-level screen. Covers "UI Theme"/"Home Theme"/"Loading
     * Theme"/"Background Theme" as one category rather than four near-identical ones - see
     * `PremiumShopCatalog.kt`'s doc for why. */
    BACKGROUND_THEME,

    /** A small emblem shown in Profile's Showcase card, same icon-in-circle rendering
     * [STICKER_PACK]/[TROPHY_RELIC] already use. */
    PROFILE_BADGE,
}
