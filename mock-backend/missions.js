'use strict';

// Mirrors MissionRotationRules.Default (domain/progression/MissionRotationRules.kt).
const ACTIVE_DAILY_COUNT = 3;
const ACTIVE_WEEKLY_COUNT = 3;
const ACTIVE_MONTHLY_COUNT = 1;

// Mirrors MissionCatalog.definitions (domain/progression/MissionCatalog.kt) -- keep both in sync.
// id strings match MissionId's enum entry names so a claim's missionId round-trips byte-for-byte;
// requirement/targetCount/reward shape matches MissionDefinition/MissionReward exactly.
const MISSION_CATALOG = {
  DAILY: [
    { id: 'CLEAR_TWO_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 2, reward: { coins: 40 } },
    { id: 'CLEAR_PRACTICE_ROUND', requirement: 'COMPLETE_PRACTICE_ROUNDS', targetCount: 1, reward: { coins: 25 } },
    { id: 'WIN_DAILY_CHALLENGE', requirement: 'COMPLETE_DAILY_CHALLENGE', targetCount: 1, reward: { coins: 60 } },
    { id: 'EARN_150_COINS', requirement: 'EARN_COINS', targetCount: 150, reward: { coins: 35 } },
    { id: 'ZERO_HINT_CLEAR', requirement: 'ZERO_HINT_LEVEL_CLEAR', targetCount: 1, reward: { coins: 45, inventoryGrants: { HINT_TOKEN: 1 } } },
    { id: 'HIGH_ACCURACY_CLEAR', requirement: 'HIGH_ACCURACY_CLEAR', targetCount: 1, reward: { coins: 50, xp: 10 } },
    { id: 'UNLOCK_A_COSMETIC', requirement: 'UNLOCK_COSMETIC', targetCount: 1, reward: { coins: 30 } },
    { id: 'EQUIP_A_COSMETIC', requirement: 'EQUIP_COSMETIC', targetCount: 1, reward: { coins: 20 } },
    // The one repeatable ad-watch mission - can't appear more than once, see MissionRotationRules' doc.
    { id: 'WATCH_A_REWARDED_AD', requirement: 'WATCH_REWARDED_AD', targetCount: 1, reward: { coins: 30, inventoryGrants: { XP_BOOST: 1 } } },
  ],
  WEEKLY: [
    { id: 'CLEAR_FIFTEEN_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 15, reward: { coins: 200, inventoryGrants: { LUCKY_SPIN_TICKET: 1 } } },
    { id: 'THREE_STAR_TEN_TIMES', requirement: 'EARN_STARS', targetCount: 30, reward: { coins: 180, inventoryGrants: { REDO_TOKEN: 2 } } },
    { id: 'WIN_WEEKLY_CHALLENGE', requirement: 'COMPLETE_WEEKLY_CHALLENGE', targetCount: 1, reward: { coins: 220, inventoryGrants: { LUCKY_SPIN_TICKET: 1 } } },
    { id: 'EARN_800_COINS', requirement: 'EARN_COINS', targetCount: 800, reward: { coins: 150 } },
    { id: 'FIVE_ZERO_HINT_CLEARS', requirement: 'ZERO_HINT_LEVEL_CLEAR', targetCount: 5, reward: { coins: 170, inventoryGrants: { HINT_TOKEN: 2 } } },
  ],
  MONTHLY: [
    { id: 'CLEAR_FORTY_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 40, reward: { coins: 500, inventoryGrants: { MYSTERY_CHEST: 1 } } },
    { id: 'FOUR_WEEKLY_SETS', requirement: 'EARN_STARS', targetCount: 120, reward: { coins: 550, inventoryGrants: { DISCOUNT_COUPON: 1 } } },
    { id: 'EARN_2500_COINS_MONTHLY', requirement: 'EARN_COINS', targetCount: 2500, reward: { coins: 400, inventoryGrants: { LUCKY_SPIN_TICKET: 2 } } },
  ],
};

// Mirrors MemoryJourneyRules.Default (domain/model/MemoryJourneyRules.kt) - a deliberate
// simplification of the plan doc's "weekly mission set completed" bonus, scaling by period rather
// than detecting a full set (see that class's doc).
const JOURNEY_POINTS_BY_PERIOD = { DAILY: 5, WEEKLY: 15, MONTHLY: 50 };

const ALL_DEFINITIONS_BY_ID = Object.fromEntries(Object.values(MISSION_CATALOG).flat().map((def) => [def.id, def]));

function definitionFor(missionId) {
  return ALL_DEFINITIONS_BY_ID[missionId];
}

function periodFor(missionId) {
  return Object.keys(MISSION_CATALOG).find((period) => MISSION_CATALOG[period].some((def) => def.id === missionId)) || null;
}

// Mirrors MissionCatalog's private hashStringToSeed - a 32-bit polynomial hash with overflow
// wraparound. JS's `| 0` truncation to a 32-bit signed integer is bit-for-bit equivalent to
// Kotlin's Int overflow, which is the entire basis for activeMissionIds resolving identically on
// both platforms (already the same trick `index.js`'s own hashStringToSeed uses for challenge seeds).
function hashStringToSeed(value) {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return hash;
}

function activeCountFor(period) {
  if (period === 'DAILY') return ACTIVE_DAILY_COUNT;
  if (period === 'WEEKLY') return ACTIVE_WEEKLY_COUNT;
  return ACTIVE_MONTHLY_COUNT;
}

// Mirrors MissionCatalog.activeMissionIds - same (period, periodKey) always yields the same set.
function activeMissionIds(period, periodKey) {
  const pool = MISSION_CATALOG[period] || [];
  const activeCount = Math.min(activeCountFor(period), pool.length);
  return pool
    .slice()
    .sort((a, b) => {
      const hashA = hashStringToSeed(`${a.id}_${periodKey}`);
      const hashB = hashStringToSeed(`${b.id}_${periodKey}`);
      if (hashA !== hashB) return hashA - hashB;
      return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
    })
    .slice(0, activeCount)
    .map((def) => def.id);
}

// Mirrors MissionCatalog.periodKeyFor - simple integer-division buckets, no calendar-aligned ISO
// week/month edge cases to keep in sync across languages.
function periodKeyFor(period, todayEpochDay) {
  if (period === 'DAILY') return todayEpochDay;
  if (period === 'WEEKLY') return Math.floor(todayEpochDay / 7);
  return Math.floor(todayEpochDay / 30);
}

// Claims a mission's reward against in-memory state - mirrors FirestoreMissionRemoteSource's
// transaction: independently re-derives eligibility (active-set membership + progress reached)
// rather than trusting the client's claim to be well-formed. Only progressCount itself is trusted,
// the same way a submitted score's accuracy/combo are (see MissionRemoteSource's doc).
function claimMissionReward(state, profile, missionId, periodKey, progressCount) {
  const definition = definitionFor(missionId);
  if (!definition) return { error: 'unknown_mission' };

  const active = activeMissionIds(periodFor(missionId), periodKey);
  if (!active.includes(missionId)) return { error: 'not_active' };
  if (progressCount < definition.targetCount) return { error: 'progress_incomplete' };

  const claimKey = `${missionId}_${periodKey}`;
  if (state.claimedKeys[claimKey]) return { error: 'already_claimed' };

  const reward = definition.reward || {};
  const journeyPointsAwarded = JOURNEY_POINTS_BY_PERIOD[periodFor(missionId)] || 0;
  const updatedProfile = {
    ...profile,
    coins: (profile.coins || 0) + (reward.coins || 0),
    xp: (profile.xp || 0) + (reward.xp || 0),
    journeyPoints: (profile.journeyPoints || 0) + journeyPointsAwarded,
  };
  const updatedInventory = { ...state.inventory };
  Object.entries(reward.inventoryGrants || {}).forEach(([kind, amount]) => {
    updatedInventory[kind] = (updatedInventory[kind] || 0) + amount;
  });
  const updatedState = {
    claimedKeys: { ...state.claimedKeys, [claimKey]: Date.now() },
    inventory: updatedInventory,
  };

  return {
    profile: updatedProfile,
    state: updatedState,
    coinsAwarded: reward.coins || 0,
    xpAwarded: reward.xp || 0,
    inventoryGrants: reward.inventoryGrants || {},
  };
}

function consumeInventoryItem(state, kind, quantity) {
  const current = state.inventory[kind] || 0;
  if (current < quantity) return { error: 'insufficient_inventory' };
  return { state: { ...state, inventory: { ...state.inventory, [kind]: current - quantity } } };
}

module.exports = {
  MISSION_CATALOG,
  definitionFor,
  activeMissionIds,
  periodKeyFor,
  claimMissionReward,
  consumeInventoryItem,
};
