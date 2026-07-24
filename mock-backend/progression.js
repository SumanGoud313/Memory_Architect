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

// Mirrors StreakCalculator.updateStreak (domain/progression/StreakCalculator.kt).
function updateStreak(lastPlayedEpochDay, todayEpochDay, previousCurrentStreak, previousLongestStreak) {
  let newCurrent;
  if (lastPlayedEpochDay === null || lastPlayedEpochDay === undefined) {
    newCurrent = 1;
  } else if (lastPlayedEpochDay === todayEpochDay) {
    newCurrent = Math.max(previousCurrentStreak, 1);
  } else if (lastPlayedEpochDay === todayEpochDay - 1) {
    newCurrent = previousCurrentStreak + 1;
  } else {
    newCurrent = 1;
  }
  const newLongest = Math.max(previousLongestStreak, newCurrent);
  return { currentStreak: newCurrent, longestStreak: newLongest };
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
  const { currentStreak, longestStreak } = updateStreak(
    profile.lastPlayedEpochDay,
    playedOnEpochDay,
    profile.currentStreak,
    profile.longestStreak,
  );
  const won = isChallengeWin(mode, sceneAccuracy);
  const nowEpochSecond = Math.floor(Date.now() / 1000);
  return {
    xp: profile.xp + xpAwarded,
    coins: (profile.coins || 0) + coinsAwarded,
    currentStreak,
    longestStreak,
    lastPlayedEpochDay: playedOnEpochDay,
    dailyChallengeWonAtEpochSecond: mode === 'DAILY_CHALLENGE' && won ? nowEpochSecond : (profile.dailyChallengeWonAtEpochSecond ?? null),
    weeklyChallengeWonAtEpochSecond: mode === 'WEEKLY_CHALLENGE' && won ? nowEpochSecond : (profile.weeklyChallengeWonAtEpochSecond ?? null),
  };
}

// Mirrors DailyRewardCatalog (domain/progression/DailyRewardCatalog.kt) -- keep both in sync.
// Deliberately modest and non-escalating-to-absurd: a full week never demands more than logging
// in, no currency you'd feel pressured to top up, no "you'll lose it all" framing anywhere.
const DAILY_REWARD_TABLE = [
  { coins: 40, xp: 0 },
  { coins: 60, xp: 0 },
  { coins: 80, xp: 20 },
  { coins: 100, xp: 0 },
  { coins: 130, xp: 30 },
  { coins: 160, xp: 0 },
  { coins: 250, xp: 75 },
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
  const updatedProfile = {
    ...profile,
    coins: (profile.coins || 0) + entry.coins,
    xp: profile.xp + entry.xp,
  };
  const updatedState = { cycleDay, lastClaimedEpochDay: todayEpochDay };
  return { profile: updatedProfile, state: updatedState, coinsAwarded: entry.coins, xpAwarded: entry.xp };
}

module.exports = {
  updateStreak,
  applyScoreSubmission,
  DAILY_REWARD_TABLE,
  nextDailyRewardCycleDay,
  canClaimDailyReward,
  claimDailyReward,
};
