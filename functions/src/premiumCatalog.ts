/**
 * Mirrors domain/progression/PremiumShopCatalog.kt + domain/progression/PremiumCatalog.kt exactly
 * - keep all three in sync, the same "mirrors X" convention shopCatalog.ts already established for
 * the coin catalog. Used by verifyPremiumPurchase (which skus to grant for a given productId) and
 * validateCosmeticsWrite (which skus are legitimately premium-only, never coin-priced, so they
 * don't get rejected as "unknown sku" the way an actually-forged sku would be).
 */
export const PREMIUM_PRODUCT_GRANTS: Record<string, string[]> = {
  founders_pack: [
    "BORDER_FOUNDER_INAUGURAL", "FRAME_FOUNDER_PIONEER", "BACKGROUND_FOUNDER_GENESIS", "NAME_COLOR_FOUNDER_ORIGIN",
    "TIMER_FOUNDER_LEGACY", "VICTORY_FOUNDER_TRIUMPH", "CONFETTI_FOUNDER_JUBILEE", "BADGE_FOUNDER_EMBLEM",
    "ROOM_FOUNDER_HERITAGE", "MATERIAL_FOUNDER_BRASS",
  ],
  starter_bundle: [
    "BORDER_STARTER_SUNRISE", "BACKGROUND_STARTER_HORIZON", "TIMER_STARTER_COMPASS", "VICTORY_STARTER_SPARK", "FRAME_STARTER_WAYFARER",
    "ROOM_STARTER_DAWN", "MATERIAL_STARTER_CANVAS",
  ],
  royal_collection: [
    "BORDER_ROYAL_CROWN", "FRAME_ROYAL_CREST", "NAME_COLOR_ROYAL_VELVET", "TIMER_ROYAL_HOURGLASS",
    "VICTORY_ROYAL_FANFARE", "CONFETTI_ROYAL_PETALS", "BACKGROUND_ROYAL_THRONE", "BADGE_ROYAL_SEAL",
    "ROOM_ROYAL_PALACE", "MATERIAL_ROYAL_GILDED_MARBLE",
  ],
  cyber_collection: [
    "BORDER_CYBER_CIRCUIT", "FRAME_CYBER_VISOR", "NAME_COLOR_CYBER_GLITCH", "TIMER_CYBER_PULSE",
    "VICTORY_CYBER_OVERDRIVE", "CONFETTI_CYBER_SPARKS", "BACKGROUND_CYBER_GRID", "BADGE_CYBER_CHIP",
    "ROOM_CYBER_GRIDWORKS", "MATERIAL_CYBER_CHROME_CIRCUIT",
  ],
  space_collection: [
    "BORDER_SPACE_ORBIT", "FRAME_SPACE_NEBULA", "NAME_COLOR_SPACE_COSMOS", "TIMER_SPACE_PULSAR",
    "VICTORY_SPACE_SUPERNOVA", "CONFETTI_SPACE_STARDUST", "BACKGROUND_SPACE_GALAXY", "BADGE_SPACE_COMET",
    "ROOM_SPACE_NEBULA_DOCK", "MATERIAL_SPACE_GUNMETAL",
  ],
  nature_collection: [
    "BORDER_NATURE_VINE", "FRAME_NATURE_LEAF", "NAME_COLOR_NATURE_MOSS", "TIMER_NATURE_BLOOM",
    "VICTORY_NATURE_BLOSSOM", "CONFETTI_NATURE_PETALS", "BACKGROUND_NATURE_FOREST", "BADGE_NATURE_ACORN",
    "ROOM_NATURE_GROVE", "MATERIAL_NATURE_MOSS_WOOD",
  ],
  luxury_collection: [
    "BORDER_LUXURY_ONYX", "FRAME_LUXURY_DIAMOND", "NAME_COLOR_LUXURY_PLATINUM", "TIMER_LUXURY_CHRONOGRAPH",
    "VICTORY_LUXURY_SPOTLIGHT", "CONFETTI_LUXURY_GOLDLEAF", "BACKGROUND_LUXURY_PENTHOUSE", "BADGE_LUXURY_CREST",
    "ROOM_LUXURY_SUITE", "MATERIAL_LUXURY_SMOKED_GLASS",
  ],
};

/** Flattened for O(1) "is this a legitimate premium sku" checks in validateCosmeticsWrite -
 * deliberately excludes remove_ads_lifetime, which grants no cosmetics (see BillingManager.kt,
 * untouched by this feature). */
export const PREMIUM_GRANTED_SKUS: Set<string> = new Set(Object.values(PREMIUM_PRODUCT_GRANTS).flat());
