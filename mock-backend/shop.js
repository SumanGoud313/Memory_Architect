'use strict';

// Mirrors domain/progression/ShopCatalog.kt's prices exactly - keep both in sync, the same
// convention progression.js already uses for ProgressionRules.Default. Only used for server-side
// price validation here; the full catalog (names/rarity/rendering) lives client-side only, same
// as every other reward/achievement catalog in this app.
const CATALOG_PRICES = {
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

const SPIN_DUPLICATE_REFUND_FRACTION = 0.5;
// Mirrors DiscountCouponRules.Default.discountFraction (domain/progression/DiscountCouponRules.kt).
const DISCOUNT_COUPON_FRACTION = 0.25;
// Mirrors SpinRules.Default.maxAdSpinsPerDay (domain/progression/SpinRules.kt).
const MAX_AD_SPINS_PER_DAY = 3;
// Mirrors MysteryChestAdRules.Default.maxClaimsPerDay (domain/progression/MysteryChestAdRules.kt).
const MAX_MYSTERY_CHEST_AD_CLAIMS_PER_DAY = 3;

function priceOf(sku) {
  return CATALOG_PRICES[sku];
}

// inventory/useDiscountCoupon are optional - omitting them (or passing useDiscountCoupon=false)
// behaves exactly as before. Mirrors FirestoreShopRemoteSource.purchase's own coupon handling.
function purchaseCosmetic(profile, cosmeticsState, sku, inventory, useDiscountCoupon) {
  const price = priceOf(sku);
  if (price === undefined) {
    return { error: 'unknown_sku' };
  }
  if ((cosmeticsState.ownedSkus || []).includes(sku)) {
    return { error: 'already_owned' };
  }
  let updatedInventory = inventory;
  let effectivePrice = price;
  if (useDiscountCoupon) {
    const owned = (inventory && inventory.DISCOUNT_COUPON) || 0;
    if (owned < 1) return { error: 'insufficient_inventory' };
    effectivePrice = Math.floor(price * (1 - DISCOUNT_COUPON_FRACTION));
    updatedInventory = { ...inventory, DISCOUNT_COUPON: owned - 1 };
  }
  if ((profile.coins || 0) < effectivePrice) {
    return { error: 'insufficient_coins' };
  }
  const updatedProfile = { ...profile, coins: profile.coins - effectivePrice };
  const updatedState = { ...cosmeticsState, ownedSkus: [...(cosmeticsState.ownedSkus || []), sku] };
  return { profile: updatedProfile, state: updatedState, inventory: updatedInventory };
}

// Spins are free - gated by source ('FREE'/'AD'/'TICKET') instead of a coin cost. Mirrors
// FirestoreShopRemoteSource.spin's gating exactly: FREE/AD each allow one spin per todayEpochDay
// (re-checked against luckySpinState, not trusted from the caller), TICKET instead atomically
// consumes one LUCKY_SPIN_TICKET and skips the daily gate entirely. request is either
// { rewardKind: 'COINS', coinsAmount } or { rewardKind: 'COSMETIC', chosenSku, rarity } - the
// client-computed LuckySpinEngine roll, already resolved to one shape before this is ever called.
function spinLuckySpin(profile, cosmeticsState, luckySpinState, request, inventory, source, todayEpochDay) {
  let updatedInventory = inventory;
  const adSpinsUsedToday = luckySpinState.lastAdSpinEpochDay === todayEpochDay ? (luckySpinState.adSpinsUsedToday || 0) : 0;
  if (source === 'FREE') {
    if (luckySpinState.lastFreeSpinEpochDay === todayEpochDay) return { error: 'spin_not_available' };
  } else if (source === 'AD') {
    if (adSpinsUsedToday >= MAX_AD_SPINS_PER_DAY) return { error: 'spin_not_available' };
  } else if (source === 'TICKET') {
    const owned = (inventory && inventory.LUCKY_SPIN_TICKET) || 0;
    if (owned < 1) return { error: 'insufficient_inventory' };
    updatedInventory = { ...inventory, LUCKY_SPIN_TICKET: owned - 1 };
  }

  const updatedLuckySpinState = {
    lastFreeSpinEpochDay: source === 'FREE' ? todayEpochDay : luckySpinState.lastFreeSpinEpochDay,
    lastAdSpinEpochDay: source === 'AD' ? todayEpochDay : luckySpinState.lastAdSpinEpochDay,
    adSpinsUsedToday: source === 'AD' ? adSpinsUsedToday + 1 : luckySpinState.adSpinsUsedToday,
    hasEverSpun: true,
  };

  if (request.rewardKind === 'COINS') {
    const updatedProfile = { ...profile, coins: (profile.coins || 0) + request.coinsAmount };
    return {
      profile: updatedProfile, state: cosmeticsState, luckySpinState: updatedLuckySpinState,
      wasDuplicate: false, coinsRefunded: 0, inventory: updatedInventory,
    };
  }

  const price = priceOf(request.chosenSku);
  if (price === undefined) {
    return { error: 'unknown_sku' };
  }
  const owned = cosmeticsState.ownedSkus || [];
  const wasDuplicate = owned.includes(request.chosenSku);
  const coinsRefunded = wasDuplicate ? Math.round(price * SPIN_DUPLICATE_REFUND_FRACTION) : 0;
  const updatedProfile = { ...profile, coins: (profile.coins || 0) + coinsRefunded };
  const updatedState = wasDuplicate ? cosmeticsState : { ...cosmeticsState, ownedSkus: [...owned, request.chosenSku] };
  return {
    profile: updatedProfile, state: updatedState, luckySpinState: updatedLuckySpinState,
    wasDuplicate, coinsRefunded, inventory: updatedInventory,
  };
}

// Watch-ad-only, no coin cost, no free/ticket path - up to MAX_MYSTERY_CHEST_AD_CLAIMS_PER_DAY
// claims per day, each granting exactly one MYSTERY_CHEST to inventory. Mirrors
// FirestoreShopRemoteSource.claimAdMysteryChest's gating exactly.
function claimAdMysteryChest(mysteryChestAdState, inventory, todayEpochDay) {
  const claimsUsedToday = mysteryChestAdState.lastClaimEpochDay === todayEpochDay ? (mysteryChestAdState.claimsUsedToday || 0) : 0;
  if (claimsUsedToday >= MAX_MYSTERY_CHEST_AD_CLAIMS_PER_DAY) return { error: 'spin_not_available' };

  const owned = (inventory && inventory.MYSTERY_CHEST) || 0;
  const updatedInventory = { ...inventory, MYSTERY_CHEST: owned + 1 };
  const updatedState = { lastClaimEpochDay: todayEpochDay, claimsUsedToday: claimsUsedToday + 1 };
  return { inventory: updatedInventory, mysteryChestAdState: updatedState };
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
  purchaseCosmetic,
  spinLuckySpin,
  claimAdMysteryChest,
  equipCosmetic,
  unequipCosmetic,
};
