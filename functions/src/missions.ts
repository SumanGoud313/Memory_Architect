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

export type MissionPeriod = "DAILY" | "WEEKLY" | "MONTHLY";

export const MISSION_CATALOG: Record<MissionPeriod, MissionDefinition[]> = {
  DAILY: [
    { id: "CLEAR_TWO_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 2, reward: { coins: 40 } },
    { id: "CLEAR_PRACTICE_ROUND", requirement: "COMPLETE_PRACTICE_ROUNDS", targetCount: 1, reward: { coins: 25 } },
    { id: "WIN_DAILY_CHALLENGE", requirement: "COMPLETE_DAILY_CHALLENGE", targetCount: 1, reward: { coins: 60 } },
    { id: "EARN_150_COINS", requirement: "EARN_COINS", targetCount: 150, reward: { coins: 35 } },
    { id: "ZERO_HINT_CLEAR", requirement: "ZERO_HINT_LEVEL_CLEAR", targetCount: 1, reward: { coins: 45, inventoryGrants: { HINT_TOKEN: 1 } } },
    { id: "HIGH_ACCURACY_CLEAR", requirement: "HIGH_ACCURACY_CLEAR", targetCount: 1, reward: { coins: 50, xp: 10 } },
    { id: "UNLOCK_A_COSMETIC", requirement: "UNLOCK_COSMETIC", targetCount: 1, reward: { coins: 30 } },
    { id: "EQUIP_A_COSMETIC", requirement: "EQUIP_COSMETIC", targetCount: 1, reward: { coins: 20 } },
    { id: "WATCH_A_REWARDED_AD", requirement: "WATCH_REWARDED_AD", targetCount: 1, reward: { coins: 30, inventoryGrants: { XP_BOOST: 1 } } },
  ],
  WEEKLY: [
    { id: "CLEAR_FIFTEEN_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 15, reward: { coins: 200, inventoryGrants: { LUCKY_SPIN_TICKET: 1 } } },
    { id: "THREE_STAR_TEN_TIMES", requirement: "EARN_STARS", targetCount: 30, reward: { coins: 180, inventoryGrants: { REDO_TOKEN: 2 } } },
    { id: "WIN_WEEKLY_CHALLENGE", requirement: "COMPLETE_WEEKLY_CHALLENGE", targetCount: 1, reward: { coins: 220, inventoryGrants: { LUCKY_SPIN_TICKET: 1 } } },
    { id: "EARN_800_COINS", requirement: "EARN_COINS", targetCount: 800, reward: { coins: 150 } },
    { id: "FIVE_ZERO_HINT_CLEARS", requirement: "ZERO_HINT_LEVEL_CLEAR", targetCount: 5, reward: { coins: 170, inventoryGrants: { HINT_TOKEN: 2 } } },
  ],
  MONTHLY: [
    { id: "CLEAR_FORTY_LEVELS", requirement: "COMPLETE_LEVELS", targetCount: 40, reward: { coins: 500, inventoryGrants: { MYSTERY_CHEST: 1 } } },
    { id: "FOUR_WEEKLY_SETS", requirement: "EARN_STARS", targetCount: 120, reward: { coins: 550, inventoryGrants: { DISCOUNT_COUPON: 1 } } },
    { id: "EARN_2500_COINS_MONTHLY", requirement: "EARN_COINS", targetCount: 2500, reward: { coins: 400, inventoryGrants: { LUCKY_SPIN_TICKET: 2 } } },
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

const ACTIVE_COUNT: Record<MissionPeriod, number> = { DAILY: 3, WEEKLY: 3, MONTHLY: 1 };

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

export function activeMissionIds(period: MissionPeriod, periodKey: number): string[] {
  const pool = MISSION_CATALOG[period] || [];
  const activeCount = Math.min(ACTIVE_COUNT[period], pool.length);
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

// Generous ceilings above the single largest mission in MISSION_CATALOG (Monthly: 550 coins/10 xp/
// 2 per inventory kind) - bounds a forged claimedKeys/inventory write the same "plausibility, not
// full re-derivation" way MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE already bounds profile writes.
export const MAX_MISSION_INVENTORY_GRANT_PER_KIND = 5;
