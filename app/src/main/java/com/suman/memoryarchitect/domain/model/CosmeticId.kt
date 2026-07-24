package com.suman.memoryarchitect.domain.model

/** One entry per Point Shop SKU - see [com.suman.memoryarchitect.domain.progression.ShopCatalog]
 * for the full definitions (category/rarity/price) each id maps to. Flat, not nested per category,
 * matching [RewardId]'s own convention. */
enum class CosmeticId {
    // Avatar Frames
    FRAME_WOVEN_CORD,
    FRAME_SILVER_LAUREL,
    FRAME_MOLTEN_BRONZE,
    FRAME_CELESTIAL_HALO,

    // Premium Borders (flagship category - applied app-wide to every primary button, see
    // ui/components/PremiumBorder.kt) - 12 items, 3 per rarity, not 4 like every other category.
    BORDER_CLASSIC_SILVER,
    BORDER_PEARL_WHITE,
    BORDER_OBSIDIAN,
    BORDER_PLATINUM,
    BORDER_EMERALD,
    BORDER_SAPPHIRE,
    BORDER_RUBY,
    BORDER_CYBER_NEON,
    BORDER_LAVA,
    BORDER_ROYAL_GOLD,
    BORDER_DIAMOND_GLOW,
    BORDER_GALAXY,

    // Name Colors
    NAME_COLOR_SLATE,
    NAME_COLOR_OCEAN_FADE,
    NAME_COLOR_SUNSET_GRADIENT,
    NAME_COLOR_PRISM_SHIFT,

    // Timer Styles
    TIMER_CLASSIC_DOT,
    TIMER_PULSE_RING,
    TIMER_EMBER_SWEEP,
    TIMER_STARLIGHT_ARC,

    // Victory Animations
    VICTORY_CONFETTI_POP,
    VICTORY_GOLDEN_SPARKS,
    VICTORY_FIREWORK_BURST,
    VICTORY_SUPERNOVA,

    // Confetti Effects
    CONFETTI_PAPER_TOSS,
    CONFETTI_RIBBON_FALL,
    CONFETTI_STAR_SHOWER,
    CONFETTI_RAINBOW_CASCADE,

    // Sticker Packs
    STICKERS_DOODLE_PACK,
    STICKERS_ADVENTURE_PACK,
    STICKERS_COSMIC_PACK,
    STICKERS_MYTHIC_PACK,

    // Trophy / Relics
    TROPHY_BRONZE_CUP,
    TROPHY_SILVER_COMPASS,
    TROPHY_GOLDEN_HOURGLASS,
    TROPHY_DIAMOND_CROWN,

    // Background Themes (coin-tier) - see ShopCatalog.kt
    BACKGROUND_MISTY_DAWN,
    BACKGROUND_TWILIGHT_HAZE,
    BACKGROUND_AURORA_DRIFT,
    BACKGROUND_STARFIELD_DEEP,

    // Profile Badges (coin-tier) - see ShopCatalog.kt
    BADGE_BRONZE_LAUREL,
    BADGE_SILVER_SEAL,
    BADGE_GOLDEN_CREST,
    BADGE_ARCHITECT_SIGIL,

    // Premium-only items below this line - never coin-priced, never Lucky Spin-eligible, see
    // PremiumCatalog.kt. Still one flat enum (not a second CosmeticId type) so the entire existing
    // rendering/equip/Collections pipeline - built around "one CosmeticId -> one CosmeticCategory
    // -> one renderer" - needs zero changes to support them; see AllCosmeticsCatalog.kt.

    // Founder's Pack
    BORDER_FOUNDER_INAUGURAL,
    FRAME_FOUNDER_PIONEER,
    BACKGROUND_FOUNDER_GENESIS,
    NAME_COLOR_FOUNDER_ORIGIN,
    TIMER_FOUNDER_LEGACY,
    VICTORY_FOUNDER_TRIUMPH,
    CONFETTI_FOUNDER_JUBILEE,
    BADGE_FOUNDER_EMBLEM,

    // Starter Bundle
    BORDER_STARTER_SUNRISE,
    BACKGROUND_STARTER_HORIZON,
    TIMER_STARTER_COMPASS,
    VICTORY_STARTER_SPARK,
    FRAME_STARTER_WAYFARER,

    // Royal Collection
    BORDER_ROYAL_CROWN,
    FRAME_ROYAL_CREST,
    NAME_COLOR_ROYAL_VELVET,
    TIMER_ROYAL_HOURGLASS,
    VICTORY_ROYAL_FANFARE,
    CONFETTI_ROYAL_PETALS,
    BACKGROUND_ROYAL_THRONE,
    BADGE_ROYAL_SEAL,

    // Cyber Collection
    BORDER_CYBER_CIRCUIT,
    FRAME_CYBER_VISOR,
    NAME_COLOR_CYBER_GLITCH,
    TIMER_CYBER_PULSE,
    VICTORY_CYBER_OVERDRIVE,
    CONFETTI_CYBER_SPARKS,
    BACKGROUND_CYBER_GRID,
    BADGE_CYBER_CHIP,

    // Space Collection
    BORDER_SPACE_ORBIT,
    FRAME_SPACE_NEBULA,
    NAME_COLOR_SPACE_COSMOS,
    TIMER_SPACE_PULSAR,
    VICTORY_SPACE_SUPERNOVA,
    CONFETTI_SPACE_STARDUST,
    BACKGROUND_SPACE_GALAXY,
    BADGE_SPACE_COMET,

    // Nature Collection
    BORDER_NATURE_VINE,
    FRAME_NATURE_LEAF,
    NAME_COLOR_NATURE_MOSS,
    TIMER_NATURE_BLOOM,
    VICTORY_NATURE_BLOSSOM,
    CONFETTI_NATURE_PETALS,
    BACKGROUND_NATURE_FOREST,
    BADGE_NATURE_ACORN,

    // Luxury Collection
    BORDER_LUXURY_ONYX,
    FRAME_LUXURY_DIAMOND,
    NAME_COLOR_LUXURY_PLATINUM,
    TIMER_LUXURY_CHRONOGRAPH,
    VICTORY_LUXURY_SPOTLIGHT,
    CONFETTI_LUXURY_GOLDLEAF,
    BACKGROUND_LUXURY_PENTHOUSE,
    BADGE_LUXURY_CREST,
}
