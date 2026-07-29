/**
 * Mirrors domain/progression/MissionCatalog.kt's mission pool and deterministic rotation exactly -
 * keep both in sync, the same "mirrors X" convention shopCatalog.ts/premiumCatalog.ts already use.
 * Used by index.ts's validateMissionClaims/validateInventoryWrite to independently re-derive
 * whether a missionClaims/inventory write was ever legitimate - the same "re-verify after the
 * fact" layer validateProfileWrite/validatePurchaseReceipt already provide for profile/shop writes.
 */

export interface MissionReward {
  coins?: number;
  xp?: number;
  inventoryGrants?: Record<string, number>;
}

export interface MissionDefinition {
  id: string;
  requirement: string;
  targetCount: number;
  reward: MissionReward;
}

export type MissionPeriod = "DAILY" | "WEEKLY" | "MONTHLY" | "EVENT";

// Every individual mission grants coins only - no xp, no inventoryGrants. The only place Lucky
// Spin Tickets/Redo/Hint/xp enter the mission loop is a period's category-completion bonus (see
// CATEGORY_BONUS_BY_PERIOD below) once all 3 active missions in that period are done.
export const MISSION_CATALOG: Record<MissionPeriod, MissionDefinition[]> = {
  DAILY: [
    { id: "CLEAR_TWO_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 2, reward: { coins: 40 } },
    { id: "CLEAR_PRACTICE_ROUND", requirement: "COMPLETE_PRACTICE_ROUNDS", targetCount: 1, reward: { coins: 25 } },
    { id: "WIN_DAILY_CHALLENGE", requirement: "COMPLETE_DAILY_CHALLENGE", targetCount: 1, reward: { coins: 60 } },
    { id: "EARN_150_COINS", requirement: "EARN_COINS", targetCount: 150, reward: { coins: 35 } },
    { id: "ZERO_HINT_CLEAR", requirement: "ZERO_HINT_LEVEL_CLEAR", targetCount: 1, reward: { coins: 45 } },
    { id: "HIGH_ACCURACY_CLEAR", requirement: "HIGH_ACCURACY_CLEAR", targetCount: 1, reward: { coins: 50 } },
    { id: "UNLOCK_A_COSMETIC", requirement: "UNLOCK_COSMETIC", targetCount: 1, reward: { coins: 30 } },
    { id: "EQUIP_A_COSMETIC", requirement: "EQUIP_COSMETIC", targetCount: 1, reward: { coins: 20 } },
    { id: "WATCH_A_REWARDED_AD", requirement: "WATCH_REWARDED_AD", targetCount: 1, reward: { coins: 30 } },
  ],
  WEEKLY: [
    { id: "CLEAR_FIFTEEN_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 15, reward: { coins: 200 } },
    { id: "THREE_STAR_TEN_TIMES", requirement: "EARN_STARS", targetCount: 30, reward: { coins: 180 } },
    { id: "WIN_WEEKLY_CHALLENGE", requirement: "COMPLETE_WEEKLY_CHALLENGE", targetCount: 1, reward: { coins: 220 } },
    { id: "EARN_800_COINS", requirement: "EARN_COINS", targetCount: 800, reward: { coins: 150 } },
    { id: "FIVE_ZERO_HINT_CLEARS", requirement: "ZERO_HINT_LEVEL_CLEAR", targetCount: 5, reward: { coins: 170 } },
  ],
  MONTHLY: [
    // Every Monthly target is sized so a player who plays every day clears it within ~4-5 days -
    // not stretched anywhere near the full 30-day rotation window. See
    // domain/progression/MissionCatalog.kt's doc.
    { id: "CLEAR_FORTY_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 40, reward: { coins: 500 } },
    { id: "FOUR_WEEKLY_SETS", requirement: "EARN_STARS", targetCount: 80, reward: { coins: 550 } },
    { id: "EARN_2500_COINS_MONTHLY", requirement: "EARN_COINS", targetCount: 2500, reward: { coins: 400 } },
    // One win per real day/week already - targetCount is small enough that 4-5 days of daily play
    // is exactly enough, not a multi-week grind.
    { id: "WIN_DAILY_CHALLENGE_MONTHLY", requirement: "COMPLETE_DAILY_CHALLENGE", targetCount: 4, reward: { coins: 350 } },
    { id: "WIN_WEEKLY_CHALLENGE_MONTHLY", requirement: "COMPLETE_WEEKLY_CHALLENGE", targetCount: 1, reward: { coins: 450 } },
  ],
  // One generic pool shared by every LiveEventCatalog template - never rotates (pool size ==
  // ACTIVE_COUNT.EVENT), so all three are always active together whenever an event is live.
  EVENT: [
    { id: "EVENT_CLEAR_FIVE_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 5, reward: { coins: 150 } },
    { id: "EVENT_EARN_FORTY_STARS", requirement: "EARN_STARS", targetCount: 40, reward: { coins: 180 } },
    { id: "EVENT_EARN_1000_COINS", requirement: "EARN_COINS", targetCount: 1000, reward: { coins: 120 } },
  ],
};

const ALL_DEFINITIONS_BY_ID: Record<string, MissionDefinition> = Object.fromEntries(
  Object.values(MISSION_CATALOG).flat().map((def) => [def.id, def]),
);

export function definitionFor(missionId: string): MissionDefinition | undefined {
  return ALL_DEFINITIONS_BY_ID[missionId];
}

export function periodFor(missionId: string): MissionPeriod | null {
  const found = (Object.keys(MISSION_CATALOG) as MissionPeriod[]).find((period) =>
    MISSION_CATALOG[period].some((def) => def.id === missionId),
  );
  return found ?? null;
}

const ACTIVE_COUNT: Record<MissionPeriod, number> = { DAILY: 3, WEEKLY: 3, MONTHLY: 3, EVENT: 3 };

// Bit-for-bit identical to MissionCatalog.kt's private hashStringToSeed and missions.js's copy -
// JS/TS's `| 0` truncation to a 32-bit signed integer is equivalent to Kotlin's Int overflow,
// which is the entire basis for activeMissionIds resolving identically on every platform.
function hashStringToSeed(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return hash;
}

// Mirrors MissionCatalog.kt's private fmix32 - Murmur3's 32-bit finalizer, thoroughly avalanches
// periodKey so it and periodKey+1 land on wildly different mixed values. See that Kotlin
// function's own doc for why hashing periodKey's own decimal string (the previous scheme) instead
// left Weekly/Monthly rotation effectively frozen in real-world use.
function fmix32(hIn: number): number {
  let h = hIn | 0;
  h = h ^ (h >>> 16);
  h = Math.imul(h, 0x85ebca6b);
  h = h ^ (h >>> 13);
  h = Math.imul(h, 0xc2b2ae35);
  h = h ^ (h >>> 16);
  return h | 0;
}

export function activeMissionIds(period: MissionPeriod, periodKey: number): string[] {
  const pool = MISSION_CATALOG[period] || [];
  const activeCount = Math.min(ACTIVE_COUNT[period], pool.length);
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

// Generous ceiling above the single largest per-kind grant any category-completion bonus makes (at
// most 1 per kind - see domain/progression/MissionCategoryBonusCatalog.kt) - individual missions
// themselves are coins-only now. Bounds a forged claimedKeys/inventory write the same
// "plausibility, not full re-derivation" way MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE already bounds
// profile writes.
export const MAX_MISSION_INVENTORY_GRANT_PER_KIND = 5;
