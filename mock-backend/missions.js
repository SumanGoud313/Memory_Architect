'use strict';

// Mirrors MissionRotationRules.Default (domain/progression/MissionRotationRules.kt).
const ACTIVE_DAILY_COUNT = 3;
const ACTIVE_WEEKLY_COUNT = 3;
const ACTIVE_MONTHLY_COUNT = 3;
const ACTIVE_EVENT_COUNT = 3;

// Mirrors MissionCatalog.definitions (domain/progression/MissionCatalog.kt) -- keep both in sync.
// id strings match MissionId's enum entry names so a claim's missionId round-trips byte-for-byte;
// requirement/targetCount/reward shape matches MissionDefinition/MissionReward exactly.
// Every individual mission grants coins only - no xp, no inventoryGrants. The only place Lucky
// Spin Tickets/Redo/Hint/xp enter the mission loop is a period's category-completion bonus (see
// CATEGORY_BONUS_BY_PERIOD below) once all 3 active missions in that period are done.
const MISSION_CATALOG = {
  DAILY: [
    { id: 'CLEAR_TWO_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 2, reward: { coins: 40 } },
    { id: 'CLEAR_PRACTICE_ROUND', requirement: 'COMPLETE_PRACTICE_ROUNDS', targetCount: 1, reward: { coins: 25 } },
    { id: 'WIN_DAILY_CHALLENGE', requirement: 'COMPLETE_DAILY_CHALLENGE', targetCount: 1, reward: { coins: 60 } },
    { id: 'EARN_150_COINS', requirement: 'EARN_COINS', targetCount: 150, reward: { coins: 35 } },
    { id: 'ZERO_HINT_CLEAR', requirement: 'ZERO_HINT_LEVEL_CLEAR', targetCount: 1, reward: { coins: 45 } },
    { id: 'HIGH_ACCURACY_CLEAR', requirement: 'HIGH_ACCURACY_CLEAR', targetCount: 1, reward: { coins: 50 } },
    { id: 'UNLOCK_A_COSMETIC', requirement: 'UNLOCK_COSMETIC', targetCount: 1, reward: { coins: 30 } },
    { id: 'EQUIP_A_COSMETIC', requirement: 'EQUIP_COSMETIC', targetCount: 1, reward: { coins: 20 } },
    // The one repeatable ad-watch mission - can't appear more than once, see MissionRotationRules' doc.
    { id: 'WATCH_A_REWARDED_AD', requirement: 'WATCH_REWARDED_AD', targetCount: 1, reward: { coins: 30 } },
  ],
  WEEKLY: [
    { id: 'CLEAR_FIFTEEN_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 15, reward: { coins: 200 } },
    { id: 'THREE_STAR_TEN_TIMES', requirement: 'EARN_STARS', targetCount: 30, reward: { coins: 180 } },
    { id: 'WIN_WEEKLY_CHALLENGE', requirement: 'COMPLETE_WEEKLY_CHALLENGE', targetCount: 1, reward: { coins: 220 } },
    { id: 'EARN_800_COINS', requirement: 'EARN_COINS', targetCount: 800, reward: { coins: 150 } },
    { id: 'FIVE_ZERO_HINT_CLEARS', requirement: 'ZERO_HINT_LEVEL_CLEAR', targetCount: 5, reward: { coins: 170 } },
  ],
  MONTHLY: [
    // Every Monthly target is sized so a player who plays every day clears it within ~4-5 days -
    // not stretched anywhere near the full 30-day rotation window. See MissionCatalog.kt's doc.
    { id: 'CLEAR_FORTY_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 40, reward: { coins: 500 } },
    { id: 'FOUR_WEEKLY_SETS', requirement: 'EARN_STARS', targetCount: 80, reward: { coins: 550 } },
    { id: 'EARN_2500_COINS_MONTHLY', requirement: 'EARN_COINS', targetCount: 2500, reward: { coins: 400 } },
    // One win per real day/week already - targetCount is small enough that 4-5 days of daily play
    // is exactly enough, not a multi-week grind.
    { id: 'WIN_DAILY_CHALLENGE_MONTHLY', requirement: 'COMPLETE_DAILY_CHALLENGE', targetCount: 4, reward: { coins: 350 } },
    { id: 'WIN_WEEKLY_CHALLENGE_MONTHLY', requirement: 'COMPLETE_WEEKLY_CHALLENGE', targetCount: 1, reward: { coins: 450 } },
  ],
  // One generic pool shared by every LiveEventCatalog template - never rotates (pool size ==
  // ACTIVE_EVENT_COUNT), so all three are always active together whenever an event is live.
  EVENT: [
    { id: 'EVENT_CLEAR_FIVE_LEVELS', requirement: 'COMPLETE_LEVELS', targetCount: 5, reward: { coins: 150 } },
    { id: 'EVENT_EARN_FORTY_STARS', requirement: 'EARN_STARS', targetCount: 40, reward: { coins: 180 } },
    { id: 'EVENT_EARN_1000_COINS', requirement: 'EARN_COINS', targetCount: 1000, reward: { coins: 120 } },
  ],
};

// Mirrors MemoryJourneyRules.Default (domain/model/MemoryJourneyRules.kt) - a deliberate
// simplification of the plan doc's "weekly mission set completed" bonus, scaling by period rather
// than detecting a full set (see that class's doc).
const JOURNEY_POINTS_BY_PERIOD = { DAILY: 5, WEEKLY: 15, MONTHLY: 50, EVENT: 10 };

// Mirrors MissionCategoryBonusCatalog.kt (domain/progression/) - the bonus granted once every
// currently-active mission in a period's set is claimed, on top of each mission's own reward. No
// entry for EVENT - a live event's short window doesn't get one. coinRange is what a claim's
// client-rolled coinsAwarded is bound-checked against (never re-rolled server-side) - inventory
// grants stay fixed, always applied from this table, never trusted from the client.
const CATEGORY_BONUS_BY_PERIOD = {
  DAILY: { coinRange: [100, 150] },
  WEEKLY: { coinRange: [200, 250], xp: 40, inventoryGrants: { LUCKY_SPIN_TICKET: 1 } },
  MONTHLY: { coinRange: [350, 450], inventoryGrants: { LUCKY_SPIN_TICKET: 1, REDO_TOKEN: 1, HINT_TOKEN: 1 } },
};

const UNLOCK_ALL_COST_COINS = 1000;

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
  if (period === 'EVENT') return ACTIVE_EVENT_COUNT;
  return ACTIVE_MONTHLY_COUNT;
}

// Mirrors MissionCatalog.kt's private fmix32 - Murmur3's 32-bit finalizer, thoroughly avalanches
// periodKey so it and periodKey+1 land on wildly different mixed values. See that Kotlin
// function's own doc for why hashing periodKey's own decimal string (the previous scheme) instead
// left Weekly/Monthly rotation effectively frozen in real-world use.
function fmix32(hIn) {
  let h = hIn | 0;
  h = h ^ (h >>> 16);
  h = Math.imul(h, 0x85ebca6b);
  h = h ^ (h >>> 13);
  h = Math.imul(h, 0xc2b2ae35);
  h = h ^ (h >>> 16);
  return h | 0;
}

// Mirrors MissionCatalog.activeMissionIds - same (period, periodKey) always yields the same set.
function activeMissionIds(period, periodKey) {
  const pool = MISSION_CATALOG[period] || [];
  const activeCount = Math.min(activeCountFor(period), pool.length);
  const periodMixed = fmix32(periodKey);
  return pool
    .slice()
    .sort((a, b) => {
      const hashA = (hashStringToSeed(a.id) ^ periodMixed) | 0;
      const hashB = (hashStringToSeed(b.id) ^ periodMixed) | 0;
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

// Mirrors FirestoreMissionRemoteSource.claimCategoryBonus's transaction: re-derives eligibility
// from state.claimedKeys (the same map claimMissionReward itself writes) rather than trusting the
// caller's belief that the whole set is done. state.bonusClaimedKeys is this bonus's own
// double-grant guard, kept separate from state.claimedKeys - see FirestoreMissionRemoteSource's
// doc for why (functions/src/index.ts's validateMissionClaims rejects a non-MissionId-shaped key).
// coinsAwarded is the client-rolled amount (see MissionCategoryBonusCatalog.roll) - only bound-
// checked against CATEGORY_BONUS_BY_PERIOD's coinRange here, never re-rolled; inventoryGrants is
// never accepted from the caller at all, always applied from this same table.
function claimCategoryBonus(state, profile, period, periodKey, coinsAwarded, xpAwarded) {
  const bonus = CATEGORY_BONUS_BY_PERIOD[period];
  if (!bonus) return { error: 'no_bonus_for_period' };
  if (typeof coinsAwarded !== 'number' || coinsAwarded < bonus.coinRange[0] || coinsAwarded > bonus.coinRange[1]) {
    return { error: 'invalid_coins_awarded' };
  }
  if ((xpAwarded || 0) !== (bonus.xp || 0)) {
    return { error: 'invalid_xp_awarded' };
  }

  const bonusKey = `${period}_${periodKey}`;
  if ((state.bonusClaimedKeys || {})[bonusKey]) return { error: 'already_claimed' };

  const activeIds = activeMissionIds(period, periodKey);
  const allClaimed = activeIds.length > 0 && activeIds.every((id) => state.claimedKeys[`${id}_${periodKey}`]);
  if (!allClaimed) return { error: 'not_all_claimed' };

  const updatedProfile = {
    ...profile,
    coins: (profile.coins || 0) + coinsAwarded,
    xp: (profile.xp || 0) + (bonus.xp || 0),
  };
  const updatedInventory = { ...state.inventory };
  Object.entries(bonus.inventoryGrants || {}).forEach(([kind, amount]) => {
    updatedInventory[kind] = (updatedInventory[kind] || 0) + amount;
  });
  const updatedState = {
    ...state,
    bonusClaimedKeys: { ...(state.bonusClaimedKeys || {}), [bonusKey]: Date.now() },
    inventory: updatedInventory,
  };

  return {
    profile: updatedProfile,
    state: updatedState,
    coinsAwarded,
    xpAwarded: bonus.xp || 0,
    inventoryGrants: bonus.inventoryGrants || {},
  };
}

// Mirrors MissionCatalog.nextDifferentPeriodKey - the first candidate periodKey strictly after
// fromPeriodKey whose active set isn't identical to justFinishedActiveIds. See that Kotlin
// function's own doc for why full disjointness isn't required (pigeonhole: Weekly/Monthly's 3-of-5
// pools can never produce two fully-disjoint 3-subsets).
function nextDifferentPeriodKey(period, fromPeriodKey, justFinishedActiveIds) {
  const justFinishedSet = new Set(justFinishedActiveIds);
  let candidate = fromPeriodKey + 1;
  for (let i = 0; i < 20; i++) {
    const candidateSet = new Set(activeMissionIds(period, candidate));
    const identical = candidateSet.size === justFinishedSet.size && [...candidateSet].every((id) => justFinishedSet.has(id));
    if (!identical) return candidate;
    candidate++;
  }
  return candidate;
}

// Mirrors FirestoreMissionRemoteSource.unlockAllMissionsEarly's transaction: re-derives eligibility
// itself (never trusts the caller), deducts UNLOCK_ALL_COST_COINS, and rolls each period forward to
// a periodKey whose active set differs from the one just finished.
function unlockAllMissionsEarly(profile, claimedKeys, dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey) {
  const periods = [
    ['DAILY', dailyPeriodKey],
    ['WEEKLY', weeklyPeriodKey],
    ['MONTHLY', monthlyPeriodKey],
  ];
  const allDone = periods.every(([period, periodKey]) => {
    const activeIds = activeMissionIds(period, periodKey);
    return activeIds.length > 0 && activeIds.every((id) => claimedKeys[`${id}_${periodKey}`]);
  });
  if (!allDone) return { error: 'not_all_claimed' };
  if ((profile.coins || 0) < UNLOCK_ALL_COST_COINS) return { error: 'insufficient_coins' };

  const updatedProfile = { ...profile, coins: profile.coins - UNLOCK_ALL_COST_COINS };
  const refreshState = {
    dailyForcedPeriodKey: nextDifferentPeriodKey('DAILY', dailyPeriodKey, activeMissionIds('DAILY', dailyPeriodKey)),
    weeklyForcedPeriodKey: nextDifferentPeriodKey('WEEKLY', weeklyPeriodKey, activeMissionIds('WEEKLY', weeklyPeriodKey)),
    monthlyForcedPeriodKey: nextDifferentPeriodKey('MONTHLY', monthlyPeriodKey, activeMissionIds('MONTHLY', monthlyPeriodKey)),
  };
  return { profile: updatedProfile, refreshState };
}

function consumeInventoryItem(state, kind, quantity) {
  const current = state.inventory[kind] || 0;
  if (current < quantity) return { error: 'insufficient_inventory' };
  return { state: { ...state, inventory: { ...state.inventory, [kind]: current - quantity } } };
}

// Consumes one MYSTERY_CHEST and grants coinsAwarded - coinsAwarded is client-rolled (see
// MysteryChestOdds.kt), this dev-only mock just applies it, same trust level every other mock
// endpoint here already has.
function openMysteryChest(state, profile, coinsAwarded) {
  const owned = state.inventory.MYSTERY_CHEST || 0;
  if (owned < 1) return { error: 'insufficient_inventory' };
  return {
    profile: { ...profile, coins: (profile.coins || 0) + coinsAwarded },
    state: { ...state, inventory: { ...state.inventory, MYSTERY_CHEST: owned - 1 } },
  };
}

// Consumes one XP_BOOST and grants xpAwarded flat XP - see XpBoostRules.kt.
function applyXpBoost(state, profile, xpAwarded) {
  const owned = state.inventory.XP_BOOST || 0;
  if (owned < 1) return { error: 'insufficient_inventory' };
  return {
    profile: { ...profile, xp: (profile.xp || 0) + xpAwarded },
    state: { ...state, inventory: { ...state.inventory, XP_BOOST: owned - 1 } },
  };
}

module.exports = {
  MISSION_CATALOG,
  definitionFor,
  activeMissionIds,
  periodKeyFor,
  claimMissionReward,
  consumeInventoryItem,
  openMysteryChest,
  applyXpBoost,
  claimCategoryBonus,
  unlockAllMissionsEarly,
};
