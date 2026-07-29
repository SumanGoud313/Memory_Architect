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

    /** Recolors the room backdrop (a translucent wash) + `LightingOverlay` tint + object
     * grounding-shadow tint during gameplay - see `ui/theme/RoomSkinVisualCatalog.kt`. Named
     * ROOM_SKIN, not ROOM_THEME, to stay unambiguous against [RewardKind.ROOM_THEME], an
     * unrelated free milestone-unlock concept (see this enum's own doc above). Premium-only,
     * never coin-priced - see `PremiumCatalog.kt`. */
    ROOM_SKIN,

    /** A paint-transform over every object's existing Canvas art (gold-foil/marble/neon/wood/glass),
     * applied uniformly regardless of which of the 8 rooms/103 objects a level's generator picked
     * that round - see `ui/theme/ObjectMaterialVisualCatalog.kt`. Also selects which pickup/
     * rotate/place sound family plays (see `core/feedback/FeedbackManagerImpl.kt`). Premium-only,
     * never coin-priced - see `PremiumCatalog.kt`. */
    OBJECT_MATERIAL,
}
