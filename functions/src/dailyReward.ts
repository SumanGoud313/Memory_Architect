/**
 * Mirrors domain/progression/DailyRewardCatalog.kt's reward table and cycle-day derivation
 * exactly - keep both in sync, the same "mirrors X" convention missions.ts/shopCatalog.ts already
 * use. Used by index.ts's validateDailyReward to independently re-derive whether a
 * dailyRewards/{uid} write was ever legitimate, the same "re-verify after the fact" layer every
 * other claim collection (missionClaims, returningPlayerGifts) already has.
 */

export interface DailyRewardEntry {
  coins: number;
  xp: number;
  bonusShield?: boolean;
  inventoryGrants?: Record<string, number>;
}

export const DAILY_REWARD_TABLE: DailyRewardEntry[] = [
  { coins: 40, xp: 0 },
  { coins: 60, xp: 0, inventoryGrants: { HINT_TOKEN: 1 } },
  { coins: 80, xp: 20 },
  { coins: 100, xp: 0, inventoryGrants: { REDO_TOKEN: 1 } },
  { coins: 130, xp: 30, inventoryGrants: { MYSTERY_CHEST: 1 } },
  { coins: 160, xp: 0, inventoryGrants: { REWATCH_TICKET: 1 } },
  { coins: 250, xp: 75, bonusShield: true, inventoryGrants: { LUCKY_SPIN_TICKET: 1 } },
];

// Mirrors DailyRewardCatalog.nextCycleDay - a missed day never docks anything already earned, it
// just quietly restarts at day 1.
export function nextDailyRewardCycleDay(
  lastClaimedEpochDay: number | null | undefined,
  cycleDay: number,
  todayEpochDay: number,
): number {
  if (lastClaimedEpochDay === null || lastClaimedEpochDay === undefined) return 1;
  if (lastClaimedEpochDay === todayEpochDay) return cycleDay;
  if (lastClaimedEpochDay === todayEpochDay - 1) return cycleDay >= DAILY_REWARD_TABLE.length ? 1 : cycleDay + 1;
  return 1;
}

// Mirrors DailyRewardCatalog.canClaim.
export function canClaimDailyReward(lastClaimedEpochDay: number | null | undefined, todayEpochDay: number): boolean {
  return lastClaimedEpochDay !== todayEpochDay;
}
