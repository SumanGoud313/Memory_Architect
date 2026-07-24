'use strict';

// Mirrors domain/progression/ShopCatalog.kt's prices exactly - keep both in sync, the same
// convention progression.js already uses for ProgressionRules.Default. Only used for server-side
// price validation here; the full catalog (names/rarity/rendering) lives client-side only, same
// as every other reward/achievement catalog in this app.
const CATALOG_PRICES = {
  FRAME_WOVEN_CORD: 180, FRAME_SILVER_LAUREL: 500, FRAME_MOLTEN_BRONZE: 1100, FRAME_CELESTIAL_HALO: 2800,
  // Premium Borders (flagship category) - 12 items, 3 per rarity. Mirrors ShopCatalog.kt exactly.
  BORDER_CLASSIC_SILVER: 170, BORDER_PEARL_WHITE: 210, BORDER_OBSIDIAN: 260,
  BORDER_PLATINUM: 450, BORDER_EMERALD: 550, BORDER_SAPPHIRE: 650,
  BORDER_RUBY: 1000, BORDER_CYBER_NEON: 1200, BORDER_LAVA: 1400,
  BORDER_ROYAL_GOLD: 2400, BORDER_DIAMOND_GLOW: 2900, BORDER_GALAXY: 3400,
  NAME_COLOR_SLATE: 150, NAME_COLOR_OCEAN_FADE: 420, NAME_COLOR_SUNSET_GRADIENT: 950, NAME_COLOR_PRISM_SHIFT: 2400,
  TIMER_CLASSIC_DOT: 200, TIMER_PULSE_RING: 480, TIMER_EMBER_SWEEP: 1050, TIMER_STARLIGHT_ARC: 2900,
  VICTORY_CONFETTI_POP: 220, VICTORY_GOLDEN_SPARKS: 550, VICTORY_FIREWORK_BURST: 1200, VICTORY_SUPERNOVA: 3200,
  CONFETTI_PAPER_TOSS: 170, CONFETTI_RIBBON_FALL: 430, CONFETTI_STAR_SHOWER: 980, CONFETTI_RAINBOW_CASCADE: 2500,
  STICKERS_DOODLE_PACK: 190, STICKERS_ADVENTURE_PACK: 470, STICKERS_COSMIC_PACK: 1150, STICKERS_MYTHIC_PACK: 3000,
  TROPHY_BRONZE_CUP: 210, TROPHY_SILVER_COMPASS: 520, TROPHY_GOLDEN_HOURGLASS: 1300, TROPHY_DIAMOND_CROWN: 3500,
  BACKGROUND_MISTY_DAWN: 190, BACKGROUND_TWILIGHT_HAZE: 480, BACKGROUND_AURORA_DRIFT: 1050, BACKGROUND_STARFIELD_DEEP: 2900,
  BADGE_BRONZE_LAUREL: 200, BADGE_SILVER_SEAL: 500, BADGE_GOLDEN_CREST: 1100, BADGE_ARCHITECT_SIGIL: 2800,
};

const SPIN_COST_COINS = 150;
const SPIN_DUPLICATE_REFUND_FRACTION = 0.5;

function priceOf(sku) {
  return CATALOG_PRICES[sku];
}

function purchaseCosmetic(profile, cosmeticsState, sku) {
  const price = priceOf(sku);
  if (price === undefined) {
    return { error: 'unknown_sku' };
  }
  if ((cosmeticsState.ownedSkus || []).includes(sku)) {
    return { error: 'already_owned' };
  }
  if ((profile.coins || 0) < price) {
    return { error: 'insufficient_coins' };
  }
  const updatedProfile = { ...profile, coins: profile.coins - price };
  const updatedState = { ...cosmeticsState, ownedSkus: [...(cosmeticsState.ownedSkus || []), sku] };
  return { profile: updatedProfile, state: updatedState };
}

function spinLuckySpin(profile, cosmeticsState, chosenSku) {
  const price = priceOf(chosenSku);
  if (price === undefined) {
    return { error: 'unknown_sku' };
  }
  if ((profile.coins || 0) < SPIN_COST_COINS) {
    return { error: 'insufficient_coins' };
  }
  const owned = cosmeticsState.ownedSkus || [];
  const wasDuplicate = owned.includes(chosenSku);
  const coinsRefunded = wasDuplicate ? Math.round(price * SPIN_DUPLICATE_REFUND_FRACTION) : 0;
  const updatedProfile = { ...profile, coins: profile.coins - SPIN_COST_COINS + coinsRefunded };
  const updatedState = wasDuplicate ? cosmeticsState : { ...cosmeticsState, ownedSkus: [...owned, chosenSku] };
  return { profile: updatedProfile, state: updatedState, wasDuplicate, coinsRefunded };
}

function equipCosmetic(cosmeticsState, category, sku) {
  return { ...cosmeticsState, equipped: { ...(cosmeticsState.equipped || {}), [category]: sku } };
}

function unequipCosmetic(cosmeticsState, category) {
  const { [category]: _removed, ...rest } = cosmeticsState.equipped || {};
  return { ...cosmeticsState, equipped: rest };
}

module.exports = {
  CATALOG_PRICES,
  SPIN_COST_COINS,
  purchaseCosmetic,
  spinLuckySpin,
  equipCosmetic,
  unequipCosmetic,
};
