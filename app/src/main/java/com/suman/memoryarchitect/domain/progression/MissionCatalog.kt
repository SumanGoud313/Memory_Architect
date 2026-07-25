package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionDefinition
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.domain.model.MissionReward

/**
 * The full mission pool plus deterministic rotation. Mirrors `mock-backend/missions.js`'s
 * `MISSION_CATALOG`/`activeMissionIds` and `functions/src/missions.ts`'s copy - all three kept in
 * sync by hand, the same convention [DailyRewardCatalog]/[StreakCalculator] already use for
 * progression math. A given (period, periodKey) always resolves to the exact same active set on
 * every platform, which is what lets a Firestore-side claim independently re-derive "was this
 * really one of today's missions" rather than trusting the client's word for it - see
 * [com.suman.memoryarchitect.data.repository.FirestoreMissionRemoteSource].
 *
 * Reward scale is deliberately modest relative to [DailyRewardCatalog]'s single check-in day
 * (40-250 coins) - three Daily Missions plus a check-in should never dwarf it, and Weekly/Monthly
 * step up gradually rather than making the daily loop feel like the "wrong" one to focus on.
 */
object MissionCatalog {

    val definitions: List<MissionDefinition> = listOf(
        // Daily
        MissionDefinition(MissionId.CLEAR_TWO_LEVELS, MissionPeriod.DAILY, MissionRequirementType.COMPLETE_LEVELS, targetCount = 2, reward = MissionReward(coins = 40L)),
        MissionDefinition(MissionId.CLEAR_PRACTICE_ROUND, MissionPeriod.DAILY, MissionRequirementType.COMPLETE_PRACTICE_ROUNDS, targetCount = 1, reward = MissionReward(coins = 25L)),
        MissionDefinition(MissionId.WIN_DAILY_CHALLENGE, MissionPeriod.DAILY, MissionRequirementType.COMPLETE_DAILY_CHALLENGE, targetCount = 1, reward = MissionReward(coins = 60L)),
        MissionDefinition(MissionId.EARN_150_COINS, MissionPeriod.DAILY, MissionRequirementType.EARN_COINS, targetCount = 150, reward = MissionReward(coins = 35L)),
        MissionDefinition(
            MissionId.ZERO_HINT_CLEAR, MissionPeriod.DAILY, MissionRequirementType.ZERO_HINT_LEVEL_CLEAR, targetCount = 1,
            reward = MissionReward(coins = 45L, inventoryGrants = mapOf(InventoryItemKind.HINT_TOKEN to 1)),
        ),
        MissionDefinition(MissionId.HIGH_ACCURACY_CLEAR, MissionPeriod.DAILY, MissionRequirementType.HIGH_ACCURACY_CLEAR, targetCount = 1, reward = MissionReward(coins = 50L, xp = 10L)),
        MissionDefinition(MissionId.UNLOCK_A_COSMETIC, MissionPeriod.DAILY, MissionRequirementType.UNLOCK_COSMETIC, targetCount = 1, reward = MissionReward(coins = 30L)),
        MissionDefinition(MissionId.EQUIP_A_COSMETIC, MissionPeriod.DAILY, MissionRequirementType.EQUIP_COSMETIC, targetCount = 1, reward = MissionReward(coins = 20L)),
        // The one repeatable ad-watch mission - reward-only, never required to keep pace, and
        // can't appear more than once since it's a single pool entry (see MissionRotationRules' doc).
        MissionDefinition(
            MissionId.WATCH_A_REWARDED_AD, MissionPeriod.DAILY, MissionRequirementType.WATCH_REWARDED_AD, targetCount = 1,
            reward = MissionReward(coins = 30L, inventoryGrants = mapOf(InventoryItemKind.XP_BOOST to 1)),
        ),

        // Weekly - larger scope, exclusive-leaning rewards (tickets/tokens rather than only coins).
        MissionDefinition(
            MissionId.CLEAR_FIFTEEN_LEVELS, MissionPeriod.WEEKLY, MissionRequirementType.COMPLETE_LEVELS, targetCount = 15,
            reward = MissionReward(coins = 200L, inventoryGrants = mapOf(InventoryItemKind.LUCKY_SPIN_TICKET to 1)),
        ),
        MissionDefinition(
            MissionId.THREE_STAR_TEN_TIMES, MissionPeriod.WEEKLY, MissionRequirementType.EARN_STARS, targetCount = 30,
            reward = MissionReward(coins = 180L, inventoryGrants = mapOf(InventoryItemKind.REDO_TOKEN to 2)),
        ),
        MissionDefinition(
            MissionId.WIN_WEEKLY_CHALLENGE, MissionPeriod.WEEKLY, MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE, targetCount = 1,
            reward = MissionReward(coins = 220L, inventoryGrants = mapOf(InventoryItemKind.LUCKY_SPIN_TICKET to 1)),
        ),
        MissionDefinition(MissionId.EARN_800_COINS, MissionPeriod.WEEKLY, MissionRequirementType.EARN_COINS, targetCount = 800, reward = MissionReward(coins = 150L)),
        MissionDefinition(
            MissionId.FIVE_ZERO_HINT_CLEARS, MissionPeriod.WEEKLY, MissionRequirementType.ZERO_HINT_LEVEL_CLEAR, targetCount = 5,
            reward = MissionReward(coins = 170L, inventoryGrants = mapOf(InventoryItemKind.HINT_TOKEN to 2)),
        ),

        // Monthly - one long arc, the biggest single reward in the loop short of a Streak milestone.
        MissionDefinition(
            MissionId.CLEAR_FORTY_LEVELS, MissionPeriod.MONTHLY, MissionRequirementType.COMPLETE_LEVELS, targetCount = 40,
            reward = MissionReward(coins = 500L, inventoryGrants = mapOf(InventoryItemKind.MYSTERY_CHEST to 1)),
        ),
        MissionDefinition(
            MissionId.FOUR_WEEKLY_SETS, MissionPeriod.MONTHLY, MissionRequirementType.EARN_STARS, targetCount = 120,
            reward = MissionReward(coins = 550L, inventoryGrants = mapOf(InventoryItemKind.DISCOUNT_COUPON to 1)),
        ),
        MissionDefinition(
            MissionId.EARN_2500_COINS_MONTHLY, MissionPeriod.MONTHLY, MissionRequirementType.EARN_COINS, targetCount = 2500,
            reward = MissionReward(coins = 400L, inventoryGrants = mapOf(InventoryItemKind.LUCKY_SPIN_TICKET to 2)),
        ),
    )

    private val byId: Map<MissionId, MissionDefinition> = definitions.associateBy { it.id }

    fun definitionFor(id: MissionId): MissionDefinition = byId.getValue(id)

    private fun poolFor(period: MissionPeriod): List<MissionDefinition> = definitions.filter { it.period == period }

    /** A 32-bit polynomial string hash with overflow wraparound - deliberately the exact same
     * shape as `mock-backend/index.js`'s `hashStringToSeed` (already used to seed Daily/Weekly
     * Challenge generation). Kotlin's [Int] arithmetic overflow is bit-for-bit equivalent to JS's
     * `| 0` truncation to a 32-bit signed integer, so a hash computed here and one computed from
     * the same string in JS/TS always agree - that agreement is the entire basis for
     * [activeMissionIds] being independently re-derivable server-side. */
    private fun hashStringToSeed(value: String): Int {
        var hash = 0
        for (char in value) {
            hash = hash * 31 + char.code
        }
        return hash
    }

    /** Deterministically picks [rules]' active-count for [period] out of that period's pool,
     * keyed on [periodKey] (see [periodKeyFor]) - the same (period, periodKey) always yields the
     * same set on every platform. Ties in [hashStringToSeed] (astronomically unlikely at this
     * pool size, but not impossible) are broken by [MissionId] name so the ordering is total and
     * stable regardless. */
    fun activeMissionIds(
        period: MissionPeriod,
        periodKey: Long,
        rules: MissionRotationRules = MissionRotationRules.Default,
    ): List<MissionId> {
        val pool = poolFor(period)
        val activeCount = rules.activeCountFor(period).coerceAtMost(pool.size)
        return pool
            .sortedWith(compareBy({ hashStringToSeed("${it.id.name}_$periodKey") }, { it.id.name }))
            .take(activeCount)
            .map { it.id }
    }

    /** [todayEpochDay] itself for [MissionPeriod.DAILY]; simple integer-division buckets for the
     * coarser windows - no calendar-aligned ISO week/month edge cases to keep in sync across
     * three languages. "Roughly weekly/monthly" is all the rotation cadence needs to be. */
    fun periodKeyFor(period: MissionPeriod, todayEpochDay: Long): Long = when (period) {
        MissionPeriod.DAILY -> todayEpochDay
        MissionPeriod.WEEKLY -> Math.floorDiv(todayEpochDay, 7L)
        MissionPeriod.MONTHLY -> Math.floorDiv(todayEpochDay, 30L)
    }
}
