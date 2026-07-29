'use strict';

const express = require('express');
const { TIER_INDEX, computeConstraints, generateLevel } = require('./generation');
const { applyScoreSubmission, canClaimDailyReward, nextDailyRewardCycleDay, claimDailyReward, claimReturningPlayerGift } = require('./progression');
const { purchaseCosmetic, spinLuckySpin, equipCosmetic, unequipCosmetic } = require('./shop');
const {
  claimMissionReward, consumeInventoryItem, openMysteryChest, applyXpBoost,
  claimCategoryBonus, unlockAllMissionsEarly,
} = require('./missions');

const PORT = process.env.PORT || 4000;
const app = express();
app.use(express.json());

// Dev-only in-memory state -- resets whenever the server restarts. Not a real database.
let remoteConfig = {
  ad_cadence: '3',
  difficulty_curve_base: '5.0',
  // Mirrors FirebaseRemoteConfigSource.kt's own defaults - empty id / zero window means no event
  // is active. See LiveEventCatalog.activeEvent's doc.
  event_active_id: '',
  event_start_epoch: '0',
  event_end_epoch: '0',
  // Mirrors AdRemoteConfig.kt's own per-key defaults exactly - see that file's doc for what each
  // key gates/tunes. Dev-only; the real values live in the Firebase Remote Config console.
  emergency_ads_disabled: 'false',
  banner_ads_enabled: 'true',
  interstitial_ads_enabled: 'true',
  rewarded_ads_enabled: 'true',
  interstitial_cooldown_seconds: '180',
  interstitial_min_level_completions_before_first: '3',
  interstitial_min_session_count: '2',
  interstitial_session_cap: '4',
  interstitial_daily_cap: '6',
  interstitial_cooldown_after_rewarded_seconds: '60',
  reward_multiplier_enabled: 'false',
  reward_multiplier_value: '2.0',
};

let playerProfile = {
  xp: 0,
  coins: 0,
  currentStreak: 0,
  longestStreak: 0,
  lastPlayedEpochDay: null,
  streakShields: 0,
  dailyChallengeWonAtEpochSecond: null,
  weeklyChallengeWonAtEpochSecond: null,
  journeyPoints: 0,
};

let dailyReward = {
  cycleDay: 0,
  lastClaimedEpochDay: null,
};

let returningPlayerGift = {
  lastClaimedOnEpochDay: null,
};

let playerCosmetics = {
  ownedSkus: [],
  equipped: {},
};

let luckySpinState = {
  lastFreeSpinEpochDay: null,
  lastAdSpinEpochDay: null,
  hasEverSpun: false,
};

let missionState = {
  claimedKeys: {},
  bonusClaimedKeys: {},
  inventory: {},
};

let missionRefreshState = {
  dailyForcedPeriodKey: null,
  weeklyForcedPeriodKey: null,
  monthlyForcedPeriodKey: null,
};

function requestLogger(req, res, next) {
  console.log(`${new Date().toISOString()} ${req.method} ${req.originalUrl}`);
  next();
}
app.use(requestLogger);

function tierIndexFor(difficulty) {
  return Object.prototype.hasOwnProperty.call(TIER_INDEX, difficulty) ? TIER_INDEX[difficulty] : TIER_INDEX.MEDIUM;
}

function utcDateParts(date) {
  return { year: date.getUTCFullYear(), month: date.getUTCMonth(), day: date.getUTCDate() };
}

function hashStringToSeed(value) {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return hash;
}

function isoWeekNumber(date) {
  const target = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNumber = (target.getUTCDay() + 6) % 7;
  target.setUTCDate(target.getUTCDate() - dayNumber + 3);
  const firstThursday = new Date(Date.UTC(target.getUTCFullYear(), 0, 4));
  return 1 + Math.round((target - firstThursday) / (7 * 24 * 3600 * 1000));
}

app.get('/v1/config/remote', (req, res) => {
  res.json({ values: remoteConfig });
});

app.get('/v1/levels/generate', (req, res) => {
  const mode = req.query.mode || 'CLASSIC';
  const difficulty = req.query.difficulty || 'MEDIUM';
  const tierIndex = tierIndexFor(difficulty);
  const seed = Math.floor(Math.random() * 2 ** 31);
  const constraints = computeConstraints(tierIndex, 0, mode);
  res.json(generateLevel(seed, mode, tierIndex, constraints));
});

app.get('/v1/challenges/daily', (req, res) => {
  const { year, month, day } = utcDateParts(new Date());
  const seed = hashStringToSeed(`daily-${year}-${month}-${day}`);
  const tierIndex = TIER_INDEX.MEDIUM;
  const constraints = computeConstraints(tierIndex, 0, 'DAILY_CHALLENGE');
  const level = generateLevel(seed, 'DAILY_CHALLENGE', tierIndex, constraints);

  const expiry = new Date();
  expiry.setUTCHours(24, 0, 0, 0);

  res.json({ level, expiresAtEpochSecond: Math.floor(expiry.getTime() / 1000) });
});

app.get('/v1/challenges/weekly', (req, res) => {
  const now = new Date();
  const seed = hashStringToSeed(`weekly-${now.getUTCFullYear()}-${isoWeekNumber(now)}`);
  // One tier harder than Daily (HARD vs MEDIUM) so Weekly is genuinely tougher via more
  // objects/rotation/order-mode content -- not a shorter timer, which the dynamic memorize-time
  // floor in generation.js already ties to that same content, so the extra difficulty comes with
  // proportionally more time to actually take it in.
  const tierIndex = TIER_INDEX.HARD;
  const constraints = computeConstraints(tierIndex, 0, 'WEEKLY_CHALLENGE');
  const level = generateLevel(seed, 'WEEKLY_CHALLENGE', tierIndex, constraints);

  const dayNumber = (now.getUTCDay() + 6) % 7; // days since Monday
  const expiry = new Date(now);
  expiry.setUTCDate(now.getUTCDate() + (7 - dayNumber));
  expiry.setUTCHours(0, 0, 0, 0);

  res.json({ level, expiresAtEpochSecond: Math.floor(expiry.getTime() / 1000) });
});

app.get('/v1/profile', (req, res) => {
  res.json(playerProfile);
});

app.post('/v1/scores/submit', (req, res) => {
  const { mode, finalScore, comboCount, sceneAccuracy, playedOnEpochDay, newlyUnlockedAchievementCount, awardXp } = req.body || {};
  if (typeof finalScore !== 'number' || typeof playedOnEpochDay !== 'number') {
    res.status(400).json({ error: 'finalScore and playedOnEpochDay are required numbers' });
    return;
  }
  playerProfile = applyScoreSubmission(playerProfile, mode, finalScore, comboCount, sceneAccuracy, playedOnEpochDay, newlyUnlockedAchievementCount, awardXp !== false);
  res.json(playerProfile);
});

// Counterpart to the client's "Reset Progress" - without this, the client's local DB wipe gets
// silently undone the next time it fetches this in-memory profile (see ProgressionApi.resetProfile
// and LocalProgressResetRepositoryImpl). Also resets the daily-reward cycle, the only other piece
// of player-specific state this mock backend tracks.
app.post('/v1/profile/reset', (req, res) => {
  playerProfile = {
    xp: 0,
    coins: 0,
    currentStreak: 0,
    longestStreak: 0,
    lastPlayedEpochDay: null,
    streakShields: 0,
    dailyChallengeWonAtEpochSecond: null,
    weeklyChallengeWonAtEpochSecond: null,
    journeyPoints: 0,
  };
  dailyReward = { cycleDay: 0, lastClaimedEpochDay: null };
  returningPlayerGift = { lastClaimedOnEpochDay: null };
  playerCosmetics = { ownedSkus: [], equipped: {} };
  luckySpinState = { lastFreeSpinEpochDay: null, lastAdSpinEpochDay: null, hasEverSpun: false };
  missionState = { claimedKeys: {}, bonusClaimedKeys: {}, inventory: {} };
  missionRefreshState = { dailyForcedPeriodKey: null, weeklyForcedPeriodKey: null, monthlyForcedPeriodKey: null };
  res.json(playerProfile);
});

app.get('/v1/cosmetics', (req, res) => {
  res.json(playerCosmetics);
});

app.get('/v1/cosmetics/luckySpinState', (req, res) => {
  res.json(luckySpinState);
});

app.post('/v1/cosmetics/purchase', (req, res) => {
  const { sku, useDiscountCoupon } = req.body || {};
  if (typeof sku !== 'string') {
    res.status(400).json({ error: 'sku is required' });
    return;
  }
  const result = purchaseCosmetic(playerProfile, playerCosmetics, sku, missionState.inventory, !!useDiscountCoupon);
  if (result.error === 'already_owned') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error === 'insufficient_inventory') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(402).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  playerCosmetics = result.state;
  missionState = { ...missionState, inventory: result.inventory };
  res.json({ purchasedSku: sku, profile: playerProfile, cosmetics: playerCosmetics, inventory: { quantities: missionState.inventory } });
});

app.post('/v1/cosmetics/spin', (req, res) => {
  const { rewardKind, coinsAmount, chosenSku, rarity, source, todayEpochDay } = req.body || {};
  if (rewardKind !== 'COINS' && rewardKind !== 'COSMETIC') {
    res.status(400).json({ error: 'rewardKind must be COINS or COSMETIC' });
    return;
  }
  if (rewardKind === 'COSMETIC' && typeof chosenSku !== 'string') {
    res.status(400).json({ error: 'chosenSku is required for a COSMETIC reward' });
    return;
  }
  if (!['FREE', 'AD', 'TICKET'].includes(source)) {
    res.status(400).json({ error: 'source must be FREE, AD, or TICKET' });
    return;
  }
  if (!Number.isFinite(todayEpochDay)) {
    res.status(400).json({ error: 'todayEpochDay is required' });
    return;
  }
  const request = rewardKind === 'COINS' ? { rewardKind, coinsAmount } : { rewardKind, chosenSku };
  const result = spinLuckySpin(playerProfile, playerCosmetics, luckySpinState, request, missionState.inventory, source, todayEpochDay);
  if (result.error === 'spin_not_available' || result.error === 'insufficient_inventory') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(402).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  playerCosmetics = result.state;
  luckySpinState = result.luckySpinState;
  missionState = { ...missionState, inventory: result.inventory };
  res.json({
    rewardKind,
    coinsAwarded: rewardKind === 'COINS' ? coinsAmount : undefined,
    awardedSku: rewardKind === 'COSMETIC' ? chosenSku : undefined,
    rarity: rewardKind === 'COSMETIC' ? (rarity || 'COMMON') : undefined,
    wasDuplicate: result.wasDuplicate,
    coinsRefunded: result.coinsRefunded,
    profile: playerProfile,
    cosmetics: playerCosmetics,
    inventory: { quantities: missionState.inventory },
    luckySpinState,
  });
});

app.post('/v1/cosmetics/equip', (req, res) => {
  const { category, sku } = req.body || {};
  if (typeof category !== 'string' || typeof sku !== 'string') {
    res.status(400).json({ error: 'category and sku are required' });
    return;
  }
  playerCosmetics = equipCosmetic(playerCosmetics, category, sku);
  res.json(playerCosmetics);
});

app.post('/v1/cosmetics/unequip', (req, res) => {
  const { category } = req.body || {};
  if (typeof category !== 'string') {
    res.status(400).json({ error: 'category is required' });
    return;
  }
  playerCosmetics = unequipCosmetic(playerCosmetics, category);
  res.json(playerCosmetics);
});

// Client-supplied "today", same trust model as playedOnEpochDay above -- this is a dev mock with
// no auth, not a real anti-cheat boundary.
app.get('/v1/rewards/daily/status', (req, res) => {
  const todayEpochDay = Number(req.query.todayEpochDay);
  if (!Number.isFinite(todayEpochDay)) {
    res.status(400).json({ error: 'todayEpochDay query param is required' });
    return;
  }
  const canClaimToday = canClaimDailyReward(dailyReward.lastClaimedEpochDay, todayEpochDay);
  const previewCycleDay = nextDailyRewardCycleDay(dailyReward.lastClaimedEpochDay, dailyReward.cycleDay, todayEpochDay);
  res.json({
    cycleDay: canClaimToday ? previewCycleDay : dailyReward.cycleDay,
    canClaimToday,
    lastClaimedEpochDay: dailyReward.lastClaimedEpochDay,
  });
});

app.post('/v1/rewards/daily/claim', (req, res) => {
  const { claimedOnEpochDay } = req.body || {};
  if (typeof claimedOnEpochDay !== 'number') {
    res.status(400).json({ error: 'claimedOnEpochDay is required' });
    return;
  }
  if (!canClaimDailyReward(dailyReward.lastClaimedEpochDay, claimedOnEpochDay)) {
    res.status(409).json({ error: 'Daily reward already claimed today' });
    return;
  }
  const result = claimDailyReward(dailyReward, playerProfile, claimedOnEpochDay, missionState.inventory);
  playerProfile = result.profile;
  dailyReward = result.state;
  missionState = { ...missionState, inventory: result.inventory };
  res.json({
    cycleDay: result.state.cycleDay,
    coinsAwarded: result.coinsAwarded,
    xpAwarded: result.xpAwarded,
    profile: playerProfile,
    shieldAwarded: result.shieldAwarded,
    inventory: { quantities: missionState.inventory },
  });
});

app.post('/v1/rewards/returning-player/claim', (req, res) => {
  const { claimedOnEpochDay } = req.body || {};
  if (typeof claimedOnEpochDay !== 'number') {
    res.status(400).json({ error: 'claimedOnEpochDay is required' });
    return;
  }
  const result = claimReturningPlayerGift(returningPlayerGift, playerProfile, missionState.inventory, claimedOnEpochDay);
  if (result.error === 'already_claimed') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(400).json({ error: result.error });
    return;
  }
  returningPlayerGift = result.state;
  missionState = { ...missionState, inventory: result.inventory };
  res.json({ inventory: { quantities: missionState.inventory } });
});

app.get('/v1/inventory', (req, res) => {
  res.json({ quantities: missionState.inventory });
});

app.post('/v1/missions/claim', (req, res) => {
  const { missionId, periodKey, progressCount } = req.body || {};
  if (typeof missionId !== 'string' || typeof periodKey !== 'number' || typeof progressCount !== 'number') {
    res.status(400).json({ error: 'missionId, periodKey and progressCount are required' });
    return;
  }
  const result = claimMissionReward(missionState, playerProfile, missionId, periodKey, progressCount);
  if (result.error === 'already_claimed') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(400).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  missionState = result.state;
  res.json({
    missionId,
    coinsAwarded: result.coinsAwarded,
    xpAwarded: result.xpAwarded,
    inventoryGrants: result.inventoryGrants,
    profile: playerProfile,
    inventory: { quantities: missionState.inventory },
  });
});

app.post('/v1/missions/claimCategoryBonus', (req, res) => {
  const { period, periodKey, coinsAwarded, xpAwarded } = req.body || {};
  if (typeof period !== 'string' || typeof periodKey !== 'number' || typeof coinsAwarded !== 'number') {
    res.status(400).json({ error: 'period, periodKey and coinsAwarded are required' });
    return;
  }
  const result = claimCategoryBonus(missionState, playerProfile, period, periodKey, coinsAwarded, xpAwarded || 0);
  if (result.error === 'already_claimed') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(400).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  missionState = result.state;
  res.json({
    coinsAwarded: result.coinsAwarded,
    xpAwarded: result.xpAwarded,
    inventoryGrants: result.inventoryGrants,
    profile: playerProfile,
    inventory: { quantities: missionState.inventory },
  });
});

app.post('/v1/missions/unlockAllEarly', (req, res) => {
  const { dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey } = req.body || {};
  if (typeof dailyPeriodKey !== 'number' || typeof weeklyPeriodKey !== 'number' || typeof monthlyPeriodKey !== 'number') {
    res.status(400).json({ error: 'dailyPeriodKey, weeklyPeriodKey and monthlyPeriodKey are required' });
    return;
  }
  const result = unlockAllMissionsEarly(playerProfile, missionState.claimedKeys, dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey);
  if (result.error === 'not_all_claimed') {
    res.status(400).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(402).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  missionRefreshState = result.refreshState;
  res.json({ profile: playerProfile, refreshState: missionRefreshState });
});

app.post('/v1/inventory/consume', (req, res) => {
  const { kind, quantity } = req.body || {};
  if (typeof kind !== 'string' || typeof quantity !== 'number') {
    res.status(400).json({ error: 'kind and quantity are required' });
    return;
  }
  const result = consumeInventoryItem(missionState, kind, quantity);
  if (result.error) {
    res.status(409).json({ error: result.error });
    return;
  }
  missionState = result.state;
  res.json({ quantities: missionState.inventory });
});

app.post('/v1/inventory/mystery-chest', (req, res) => {
  const { coinsAwarded } = req.body || {};
  if (typeof coinsAwarded !== 'number') {
    res.status(400).json({ error: 'coinsAwarded is required' });
    return;
  }
  const result = openMysteryChest(missionState, playerProfile, coinsAwarded);
  if (result.error) {
    res.status(409).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  missionState = result.state;
  res.json({ profile: playerProfile, inventory: { quantities: missionState.inventory } });
});

app.post('/v1/inventory/xp-boost', (req, res) => {
  const { xpAwarded } = req.body || {};
  if (typeof xpAwarded !== 'number') {
    res.status(400).json({ error: 'xpAwarded is required' });
    return;
  }
  const result = applyXpBoost(missionState, playerProfile, xpAwarded);
  if (result.error) {
    res.status(409).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  missionState = result.state;
  res.json({ profile: playerProfile, inventory: { quantities: missionState.inventory } });
});

app.listen(PORT, () => {
  console.log(`Memory Architect mock backend listening on http://0.0.0.0:${PORT}`);
});
