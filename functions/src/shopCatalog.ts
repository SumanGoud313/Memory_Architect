/**
 * Mirrors domain/progression/ShopCatalog.kt's prices exactly - keep both in sync, the same
 * "mirrors X" convention mock-backend/progression.js already uses for the Kotlin progression
 * rules. Used only for server-side price validation (validatePurchaseReceipt below) - the full
 * catalog (names/rarity/rendering) lives client-side only, same as every other reward/achievement
 * catalog in this app.
 */
export const SHOP_CATALOG_PRICES: Record<string, number> = {
  FRAME_WOVEN_CORD: 560, FRAME_SILVER_LAUREL: 1070, FRAME_MOLTEN_BRONZE: 2070, FRAME_CELESTIAL_HALO: 4570,
  // Premium Borders (flagship category) - 12 items, 3 per rarity. Mirrors ShopCatalog.kt exactly.
  BORDER_CLASSIC_SILVER: 540, BORDER_PEARL_WHITE: 620, BORDER_OBSIDIAN: 720,
  BORDER_PLATINUM: 980, BORDER_EMERALD: 1150, BORDER_SAPPHIRE: 1320,
  BORDER_RUBY: 1930, BORDER_CYBER_NEON: 2200, BORDER_LAVA: 2470,
  BORDER_ROYAL_GOLD: 4030, BORDER_DIAMOND_GLOW: 4700, BORDER_GALAXY: 5370,
  NAME_COLOR_SLATE: 500, NAME_COLOR_OCEAN_FADE: 930, NAME_COLOR_SUNSET_GRADIENT: 1870, NAME_COLOR_PRISM_SHIFT: 4030,
  TIMER_CLASSIC_DOT: 600, TIMER_PULSE_RING: 1030, TIMER_EMBER_SWEEP: 2000, TIMER_STARLIGHT_ARC: 4700,
  VICTORY_CONFETTI_POP: 640, VICTORY_GOLDEN_SPARKS: 1150, VICTORY_FIREWORK_BURST: 2200, VICTORY_SUPERNOVA: 5100,
  CONFETTI_PAPER_TOSS: 540, CONFETTI_RIBBON_FALL: 950, CONFETTI_STAR_SHOWER: 1910, CONFETTI_RAINBOW_CASCADE: 4170,
  STICKERS_DOODLE_PACK: 580, STICKERS_ADVENTURE_PACK: 1020, STICKERS_COSMIC_PACK: 2130, STICKERS_MYTHIC_PACK: 4830,
  TROPHY_BRONZE_CUP: 620, TROPHY_SILVER_COMPASS: 1100, TROPHY_GOLDEN_HOURGLASS: 2330, TROPHY_DIAMOND_CROWN: 5500,
  BACKGROUND_MISTY_DAWN: 580, BACKGROUND_TWILIGHT_HAZE: 1030, BACKGROUND_AURORA_DRIFT: 2000, BACKGROUND_STARFIELD_DEEP: 4700,
  BADGE_BRONZE_LAUREL: 600, BADGE_SILVER_SEAL: 1070, BADGE_GOLDEN_CREST: 2070, BADGE_ARCHITECT_SIGIL: 4570,
};

// Mirrors SpinRules.Default (domain/progression/SpinRules.kt) - spins are free now (gated by a
// daily/ad/ticket allowance, not a coin cost), and can resolve to either a bounded coin grant or a
// real cosmetic. Used by validatePurchaseReceipt below to bound a SPIN-kind receipt's priceCoins
// (which is now a coins *delta* - a grant amount or a duplicate refund - not a fixed cost).
export const SPIN_COIN_OUTCOME_AMOUNTS = [150, 250, 500];
export const SPIN_DUPLICATE_REFUND_FRACTION = 0.5;
