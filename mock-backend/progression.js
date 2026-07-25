'use strict';

// Mirrors ProgressionRules.Default (domain/progression/ProgressionRules.kt).
const XP_PER_SCORE_POINT = 1.0;
const COINS_PER_SCORE_POINT = 0.2;
const COMBO_BONUS_COINS_PER_STEP = 5;
// Daily/Weekly Challenge bypass the score-based coin formula entirely - a flat win bonus (or
// nothing, short of a win) instead of a variable one, matching their fixed object count.
const DAILY_CHALLENGE_WIN_COINS = 200;
const WEEKLY_CHALLENGE_WIN_COINS = 500;
const CHALLENGE_WIN_ACCURACY_THRESHOLD = 0.7;

// Mirrors StreakRules.Default (domain/model/StreakRules.kt).
const STREAK_MILESTONE_DAYS = [3, 7, 14, 30, 60, 90, 180, 365];
const STREAK_SHIELD_MILESTONE_DAYS = [7, 30, 90, 365];
const MAX_STORED_STREAK_SHIELDS = 3;

// Mirrors StreakCalculator.updateStreak (domain/progression/StreakCalculator.kt) - including the
// Streak Shield auto-consume (a one-day gap with at least one shield banked extends the streak
// instead of resetting it) and milestone-shield-grant logic, not just the plain streak length.
function updateStreak(lastPlayedEpochDay, todayEpochDay, previousCurrentStreak, previousLongestStreak, previousStreakShields) {
  const gapDays = lastPlayedEpochDay === null || lastPlayedEpochDay === undefined ? null : todayEpochDay - lastPlayedEpochDay;
  let shields = previousStreakShields || 0;
  let shieldConsumed = false;
  let newCurrent;
  let isFreshDay;

  if (lastPlayedEpochDay === null || lastPlayedEpochDay === undefined) {
    newCurrent = 1;
    isFreshDay = true;
  } else if (gapDays === 0) {
    newCurrent = Math.max(previousCurrentStreak, 1);
    isFreshDay = false;
  } else if (gapDays === 1) {
    newCurrent = previousCurrentStreak + 1;
    isFreshDay = true;
  } else if (gapDays === 2 && shields > 0) {
    shields -= 1;
    shieldConsumed = true;
    newCurrent = previousCurrentStreak + 1;
    isFreshDay = true;
  } else {
    newCurrent = 1;
    isFreshDay = true;
  }

  const newLongest = Math.max(previousLongestStreak, newCurrent);
  const milestoneReached = isFreshDay && STREAK_MILESTONE_DAYS.includes(newCurrent) ? newCurrent : null;
  const shieldGranted = milestoneReached !== null && STREAK_SHIELD_MILESTONE_DAYS.includes(milestoneReached) && shields < MAX_STORED_STREAK_SHIELDS;
  if (shieldGranted) shields += 1;

  return {
    currentStreak: newCurrent,
    longestStreak: newLongest,
    streakShields: shields,
    shieldConsumed,
    shieldGranted,
    milestoneReached,
  };
}

function isChallengeWin(mode, sceneAccuracy) {
  if (mode !== 'DAILY_CHALLENGE' && mode !== 'WEEKLY_CHALLENGE') return false;
  return (sceneAccuracy || 0) >= CHALLENGE_WIN_ACCURACY_THRESHOLD;
}

function coinsAwardedFor(mode, finalScore, comboCount, sceneAccuracy) {
  if (mode === 'DAILY_CHALLENGE' || mode === 'WEEKLY_CHALLENGE') {
    if (!isChallengeWin(mode, sceneAccuracy)) return 0;
    return mode === 'DAILY_CHALLENGE' ? DAILY_CHALLENGE_WIN_COINS : WEEKLY_CHALLENGE_WIN_COINS;
  }
  const comboBonus = Math.max(0, (comboCount || 0) - 1) * COMBO_BONUS_COINS_PER_STEP;
  return Math.round(finalScore * COINS_PER_SCORE_POINT) + comboBonus;
}

// A win locks that mode's card client-side for a fixed window (24h daily / 168h weekly - see
// GameModeDisplay.kt's challengeLockDurationSeconds()). Stamped here with the server's own clock
// so it's the authoritative value the client's optimistic local guess gets reconciled against.
function applyScoreSubmission(profile, mode, finalScore, comboCount, sceneAccuracy, playedOnEpochDay) {
  const xpAwarded = finalScore * XP_PER_SCORE_POINT;
  const coinsAwarded = coinsAwardedFor(mode, finalScore, comboCount, sceneAccuracy);
  const streakUpdate = updateStreak(
    profile.lastPlayedEpochDay,
    playedOnEpochDay,
    profile.currentStreak,
    profile.longestStreak,
    profile.streakShields,
  );
  const won = isChallengeWin(mode, sceneAccuracy);
  const nowEpochSecond = Math.floor(Date.now() / 1000);
  return {
    xp: profile.xp + xpAwarded,
    coins: (profile.coins || 0) + coinsAwarded,
    currentStreak: streakUpdate.currentStreak,
    longestStreak: streakUpdate.longestStreak,
    streakShields: streakUpdate.streakShields,
    lastPlayedEpochDay: playedOnEpochDay,
    dailyChallengeWonAtEpochSecond: mode === 'DAILY_CHALLENGE' && won ? nowEpochSecond : (profile.dailyChallengeWonAtEpochSecond ?? null),
    weeklyChallengeWonAtEpochSecond: mode === 'WEEKLY_CHALLENGE' && won ? nowEpochSecond : (profile.weeklyChallengeWonAtEpochSecond ?? null),
  };
}

// Mirrors DailyRewardCatalog (domain/progression/DailyRewardCatalog.kt) -- keep both in sync.
// Deliberately modest and non-escalating-to-absurd: a full week never demands more than logging
// in, no currency you'd feel pressured to top up, no "you'll lose it all" framing anywhere. Day 5
// is a Mystery Chest (same fixed amount, just revealed only on claim - no server-side randomness
// to keep in sync); day 7 also grants a Streak Shield, capped by MAX_STORED_STREAK_SHIELDS.
const DAILY_REWARD_TABLE = [
  { coins: 40, xp: 0 },
  { coins: 60, xp: 0 },
  { coins: 80, xp: 20 },
  { coins: 100, xp: 0 },
  { coins: 130, xp: 30 },
  { coins: 160, xp: 0 },
  { coins: 250, xp: 75, bonusShield: true },
];

// A missed day never docks anything already earned -- it just quietly restarts at day 1, the
// same "no punishment, only forward progress" spirit as StreakCalculator.
function nextDailyRewardCycleDay(lastClaimedEpochDay, cycleDay, todayEpochDay) {
  if (lastClaimedEpochDay === null || lastClaimedEpochDay === undefined) return 1;
  if (lastClaimedEpochDay === todayEpochDay) return cycleDay;
  if (lastClaimedEpochDay === todayEpochDay - 1) return cycleDay >= DAILY_REWARD_TABLE.length ? 1 : cycleDay + 1;
  return 1;
}

function canClaimDailyReward(lastClaimedEpochDay, todayEpochDay) {
  return lastClaimedEpochDay !== todayEpochDay;
}

function claimDailyReward(dailyRewardState, profile, todayEpochDay) {
  const cycleDay = nextDailyRewardCycleDay(dailyRewardState.lastClaimedEpochDay, dailyRewardState.cycleDay, todayEpochDay);
  const entry = DAILY_REWARD_TABLE[cycleDay - 1];
  const shieldAwarded = Boolean(entry.bonusShield) && (profile.streakShields || 0) < MAX_STORED_STREAK_SHIELDS;
  const updatedProfile = {
    ...profile,
    coins: (profile.coins || 0) + entry.coins,
    xp: profile.xp + entry.xp,
    streakShields: shieldAwarded ? (profile.streakShields || 0) + 1 : (profile.streakShields || 0),
  };
  const updatedState = { cycleDay, lastClaimedEpochDay: todayEpochDay };
  return { profile: updatedProfile, state: updatedState, coinsAwarded: entry.coins, xpAwarded: entry.xp, shieldAwarded };
}

module.exports = {
  updateStreak,
  applyScoreSubmission,
  DAILY_REWARD_TABLE,
  nextDailyRewardCycleDay,
  canClaimDailyReward,
  claimDailyReward,
};
