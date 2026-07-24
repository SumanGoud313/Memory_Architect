'use strict';

const express = require('express');
const { TIER_INDEX, computeConstraints, generateLevel } = require('./generation');
const { applyScoreSubmission, canClaimDailyReward, nextDailyRewardCycleDay, claimDailyReward } = require('./progression');
const { purchaseCosmetic, spinLuckySpin, equipCosmetic, unequipCosmetic } = require('./shop');

const PORT = process.env.PORT || 4000;
const app = express();
app.use(express.json());

// Dev-only in-memory state -- resets whenever the server restarts. Not a real database.
let remoteConfig = {
  ad_cadence: '3',
  difficulty_curve_base: '5.0',
  seasonal_event_active: 'false',
};

let playerProfile = {
  xp: 0,
  coins: 0,
  currentStreak: 0,
  longestStreak: 0,
  lastPlayedEpochDay: null,
  dailyChallengeWonAtEpochSecond: null,
  weeklyChallengeWonAtEpochSecond: null,
};

let dailyReward = {
  cycleDay: 0,
  lastClaimedEpochDay: null,
};

let playerCosmetics = {
  ownedSkus: [],
  equipped: {},
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
  const { mode, finalScore, comboCount, sceneAccuracy, playedOnEpochDay } = req.body || {};
  if (typeof finalScore !== 'number' || typeof playedOnEpochDay !== 'number') {
    res.status(400).json({ error: 'finalScore and playedOnEpochDay are required numbers' });
    return;
  }
  playerProfile = applyScoreSubmission(playerProfile, mode, finalScore, comboCount, sceneAccuracy, playedOnEpochDay);
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
    dailyChallengeWonAtEpochSecond: null,
    weeklyChallengeWonAtEpochSecond: null,
  };
  dailyReward = { cycleDay: 0, lastClaimedEpochDay: null };
  playerCosmetics = { ownedSkus: [], equipped: {} };
  res.json(playerProfile);
});

app.get('/v1/cosmetics', (req, res) => {
  res.json(playerCosmetics);
});

app.post('/v1/cosmetics/purchase', (req, res) => {
  const { sku } = req.body || {};
  if (typeof sku !== 'string') {
    res.status(400).json({ error: 'sku is required' });
    return;
  }
  const result = purchaseCosmetic(playerProfile, playerCosmetics, sku);
  if (result.error === 'already_owned') {
    res.status(409).json({ error: result.error });
    return;
  }
  if (result.error) {
    res.status(402).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  playerCosmetics = result.state;
  res.json({ purchasedSku: sku, profile: playerProfile, cosmetics: playerCosmetics });
});

app.post('/v1/cosmetics/spin', (req, res) => {
  const { chosenSku } = req.body || {};
  if (typeof chosenSku !== 'string') {
    res.status(400).json({ error: 'chosenSku is required' });
    return;
  }
  const result = spinLuckySpin(playerProfile, playerCosmetics, chosenSku);
  if (result.error) {
    res.status(402).json({ error: result.error });
    return;
  }
  playerProfile = result.profile;
  playerCosmetics = result.state;
  res.json({
    awardedSku: chosenSku,
    rarity: (req.body && req.body.rarity) || 'COMMON',
    wasDuplicate: result.wasDuplicate,
    coinsRefunded: result.coinsRefunded,
    profile: playerProfile,
    cosmetics: playerCosmetics,
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
  const result = claimDailyReward(dailyReward, playerProfile, claimedOnEpochDay);
  playerProfile = result.profile;
  dailyReward = result.state;
  res.json({ cycleDay: result.state.cycleDay, coinsAwarded: result.coinsAwarded, xpAwarded: result.xpAwarded, profile: playerProfile });
});

app.listen(PORT, () => {
  console.log(`Memory Architect mock backend listening on http://0.0.0.0:${PORT}`);
});
