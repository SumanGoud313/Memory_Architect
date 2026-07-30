package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionDefinition
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.domain.model.MissionReward

/**
 * The full mission pool plus deterministic rotation. Mirrors `mock-backend/missions.js`'s
 * `MISSION_CATALOG`/`activeMissionIds` - both kept in sync by hand, the same convention
 * [DailyRewardCatalog]/[StreakCalculator] already use for progression math. A given (period,
 * periodKey) always resolves to the exact same active set on every platform, which is what lets
 * the claiming transaction independently re-derive "was this really one of today's missions"
 * rather than trusting the client's word for it - see
 * [com.suman.memoryarchitect.data.repository.FirestoreMissionRemoteSource].
 *
 * Reward scale is deliberately modest relative to [DailyRewardCatalog]'s single check-in day
 * (40-250 coins) - three Daily Missions plus a check-in should never dwarf it, and Weekly/Monthly
 * step up gradually rather than making the daily loop feel like the "wrong" one to focus on.
 */
object MissionCatalog {

    // Every individual mission grants coins only - no xp, no inventoryGrants. The only place
    // Lucky Spin Tickets/Redo/Hint/xp enter the mission loop is a period's category-completion
    // bonus (see MissionCategoryBonusCatalog) once all 3 active missions in that period are done.
    val definitions: List<MissionDefinition> = listOf(
        // Daily
        MissionDefinition(MissionId.CLEAR_TWO_LEVELS, MissionPeriod.DAILY, MissionRequirementType.COMPLETE_LEVELS, targetCount = 2, reward = MissionReward(coins = 40L)),
        MissionDefinition(MissionId.CLEAR_PRACTICE_ROUND, MissionPeriod.DAILY, MissionRequirementType.COMPLETE_PRACTICE_ROUNDS, targetCount = 1, reward = MissionReward(coins = 25L)),
        MissionDefinition(MissionId.WIN_DAILY_CHALLENGE, MissionPeriod.DAILY, MissionRequirementType.COMPLETE_DAILY_CHALLENGE, targetCount = 1, reward = MissionReward(coins = 60L)),
        MissionDefinition(MissionId.EARN_150_COINS, MissionPeriod.DAILY, MissionRequirementType.EARN_COINS, targetCount = 150, reward = MissionReward(coins = 35L)),
        MissionDefinition(MissionId.ZERO_HINT_CLEAR, MissionPeriod.DAILY, MissionRequirementType.ZERO_HINT_LEVEL_CLEAR, targetCount = 1, reward = MissionReward(coins = 45L)),
        MissionDefinition(MissionId.HIGH_ACCURACY_CLEAR, MissionPeriod.DAILY, MissionRequirementType.HIGH_ACCURACY_CLEAR, targetCount = 1, reward = MissionReward(coins = 50L)),
        MissionDefinition(MissionId.UNLOCK_A_COSMETIC, MissionPeriod.DAILY, MissionRequirementType.UNLOCK_COSMETIC, targetCount = 1, reward = MissionReward(coins = 30L)),
        MissionDefinition(MissionId.EQUIP_A_COSMETIC, MissionPeriod.DAILY, MissionRequirementType.EQUIP_COSMETIC, targetCount = 1, reward = MissionReward(coins = 20L)),
        // The one repeatable ad-watch mission - reward-only, never required to keep pace, and
        // can't appear more than once since it's a single pool entry (see MissionRotationRules' doc).
        MissionDefinition(MissionId.WATCH_A_REWARDED_AD, MissionPeriod.DAILY, MissionRequirementType.WATCH_REWARDED_AD, targetCount = 1, reward = MissionReward(coins = 30L)),

        // Weekly - larger scope than Daily, coins-only same as every other period.
        MissionDefinition(MissionId.CLEAR_FIFTEEN_LEVELS, MissionPeriod.WEEKLY, MissionRequirementType.COMPLETE_LEVELS, targetCount = 15, reward = MissionReward(coins = 200L)),
        MissionDefinition(MissionId.THREE_STAR_TEN_TIMES, MissionPeriod.WEEKLY, MissionRequirementType.EARN_STARS, targetCount = 30, reward = MissionReward(coins = 180L)),
        MissionDefinition(MissionId.WIN_WEEKLY_CHALLENGE, MissionPeriod.WEEKLY, MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE, targetCount = 1, reward = MissionReward(coins = 220L)),
        MissionDefinition(MissionId.EARN_800_COINS, MissionPeriod.WEEKLY, MissionRequirementType.EARN_COINS, targetCount = 800, reward = MissionReward(coins = 150L)),
        MissionDefinition(MissionId.FIVE_ZERO_HINT_CLEARS, MissionPeriod.WEEKLY, MissionRequirementType.ZERO_HINT_LEVEL_CLEAR, targetCount = 5, reward = MissionReward(coins = 170L)),

        // Monthly - the biggest single per-mission coin reward short of a Streak milestone, and
        // meaningfully tougher than Weekly, but every target here is sized so a player who plays
        // *every day* clears it within roughly 4-5 days - not stretched anywhere near the full
        // 30-day rotation window (a pool of 5, 3 active at a time - see
        // MissionRotationRules.activeMonthlyCount - purely for rotation variety, not difficulty).
        // CLEAR_FORTY_LEVELS' targetCount (40) is comfortably inside LevelCampaignRules.maxLevel
        // (100) - COMPLETE_LEVELS counts *clears*, not distinct levels reached, so replaying
        // already-cleared levels counts too, same as CLEAR_TWO_LEVELS/CLEAR_FIFTEEN_LEVELS above.
        MissionDefinition(MissionId.CLEAR_FORTY_LEVELS, MissionPeriod.MONTHLY, MissionRequirementType.COMPLETE_LEVELS, targetCount = 40, reward = MissionReward(coins = 500L)),
        MissionDefinition(MissionId.FOUR_WEEKLY_SETS, MissionPeriod.MONTHLY, MissionRequirementType.EARN_STARS, targetCount = 80, reward = MissionReward(coins = 550L)),
        MissionDefinition(MissionId.EARN_2500_COINS_MONTHLY, MissionPeriod.MONTHLY, MissionRequirementType.EARN_COINS, targetCount = 2500, reward = MissionReward(coins = 400L)),
        // Daily/Weekly Challenge wins are already capped at one per real calendar day/week (see
        // GameMode.challengeLockDurationSeconds()) - targetCount here is deliberately small enough
        // that "play every day for 4-5 days" is exactly enough to clear it, not a multi-week grind.
        MissionDefinition(MissionId.WIN_DAILY_CHALLENGE_MONTHLY, MissionPeriod.MONTHLY, MissionRequirementType.COMPLETE_DAILY_CHALLENGE, targetCount = 4, reward = MissionReward(coins = 350L)),
        MissionDefinition(MissionId.WIN_WEEKLY_CHALLENGE_MONTHLY, MissionPeriod.MONTHLY, MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE, targetCount = 1, reward = MissionReward(coins = 450L)),

        // Event - one generic pool shared by every LiveEventCatalog template, scoped between
        // Daily and Weekly (a typical event window runs a few days). See MissionRotationRules'
        // doc for why the pool size equals its own active count (no rotation).
        MissionDefinition(MissionId.EVENT_CLEAR_FIVE_LEVELS, MissionPeriod.EVENT, MissionRequirementType.COMPLETE_LEVELS, targetCount = 5, reward = MissionReward(coins = 150L)),
        MissionDefinition(MissionId.EVENT_EARN_FORTY_STARS, MissionPeriod.EVENT, MissionRequirementType.EARN_STARS, targetCount = 40, reward = MissionReward(coins = 180L)),
        MissionDefinition(MissionId.EVENT_EARN_1000_COINS, MissionPeriod.EVENT, MissionRequirementType.EARN_COINS, targetCount = 1000, reward = MissionReward(coins = 120L)),
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

    /** Murmur3's 32-bit finalizer ("fmix32") - thoroughly avalanches [periodKey] so it and
     * `periodKey + 1` land on wildly different mixed values.
     *
     * This replaces an earlier scheme that instead hashed the *string concatenation*
     * `"${missionId}_$periodKey"` directly: [hashStringToSeed] is a left-to-right polynomial hash
     * (`hash = hash * 31 + char`), which means the *last* characters of a string - here,
     * periodKey's own digits, since it was the suffix - carry far less weight than the first
     * (missionId's own prefix dominates via much larger powers of 31). Worse, a short decimal
     * string's own hash is itself nearly linear in the integer it represents (incrementing the
     * last digit by one changes the hash by exactly one, for same-length numbers), so two
     * consecutive periodKeys barely perturbed the sort order at all. The real-world result: with
     * Weekly/Monthly's small 5-item pools, the *same* 3 missions stayed active for well over a
     * thousand consecutive periodKeys in a row (verified empirically) - Weekly/Monthly rotation
     * was effectively frozen. Mixing [periodKey] as a real integer through this finalizer first,
     * then combining with each mission id's own (unmodified, still string-hashed) value via XOR,
     * fixes this: verified empirically to bring every period's median run length down to 1
     * (worst observed: 12 consecutive periodKeys, across a 20,000-sample scan). */
    private fun fmix32(hIn: Int): Int {
        var h = hIn
        h = h xor (h ushr 16)
        h *= 0x85EBCA6B.toInt()
        h = h xor (h ushr 13)
        h *= 0xC2B2AE35.toInt()
        h = h xor (h ushr 16)
        return h
    }

    /** Deterministically picks [rules]' active-count for [period] out of that period's pool,
     * keyed on [periodKey] (see [periodKeyFor]) - the same (period, periodKey) always yields the
     * same set on every platform. Ties (astronomically unlikely at this pool size, but not
     * impossible) are broken by [MissionId] name so the ordering is total and stable regardless. */
    fun activeMissionIds(
        period: MissionPeriod,
        periodKey: Long,
        rules: MissionRotationRules = MissionRotationRules.Default,
    ): List<MissionId> {
        val pool = poolFor(period)
        val activeCount = rules.activeCountFor(period).coerceAtMost(pool.size)
        val periodMixed = fmix32(periodKey.toInt())
        return pool
            .sortedWith(compareBy({ hashStringToSeed(it.id.name) xor periodMixed }, { it.id.name }))
            .take(activeCount)
            .map { it.id }
    }

    /** [todayEpochDay] itself for [MissionPeriod.DAILY]; simple integer-division buckets for the
     * coarser windows - no calendar-aligned ISO week/month edge cases to keep in sync across
     * three languages. "Roughly weekly/monthly" is all the rotation cadence needs to be.
     *
     * [MissionPeriod.EVENT] uses [activeEventStartEpochSecond] (the live
     * [com.suman.memoryarchitect.domain.model.LiveEvent]'s own start, from
     * [LiveEventCatalog.activeEvent]) instead of any [todayEpochDay]-derived bucket, so the same
     * event's Event missions stay one continuous occurrence for its whole (possibly multi-day)
     * window rather than resetting daily like every other period. Callers must never resolve
     * [MissionPeriod.EVENT] without a live event - see [MissionRepositoryImpl]'s doc. */
    fun periodKeyFor(period: MissionPeriod, todayEpochDay: Long, activeEventStartEpochSecond: Long? = null): Long = when (period) {
        MissionPeriod.DAILY -> todayEpochDay
        MissionPeriod.WEEKLY -> Math.floorDiv(todayEpochDay, 7L)
        MissionPeriod.MONTHLY -> Math.floorDiv(todayEpochDay, 30L)
        MissionPeriod.EVENT -> requireNotNull(activeEventStartEpochSecond) {
            "MissionPeriod.EVENT requires an active event's start epoch"
        }
    }

    /** [periodKeyFor] plus [forcedPeriodKey] - the "pay 1000 coins to unlock now" override (see
     * [com.suman.memoryarchitect.domain.model.MissionRefreshState]'s doc). `maxOf` is what makes
     * this self-healing: a forced key only matters while it's still ahead of the real calendar's
     * own natural key; the moment real time catches up and passes it, this silently reverts to
     * tracking the natural key again with no separate cleanup step. Never used for
     * [MissionPeriod.EVENT] (no forced-key concept there - an Event's own [LiveEvent] window
     * already has a real end time). */
    fun effectivePeriodKey(period: MissionPeriod, todayEpochDay: Long, forcedPeriodKey: Long?, activeEventStartEpochSecond: Long? = null): Long =
        maxOf(periodKeyFor(period, todayEpochDay, activeEventStartEpochSecond), forcedPeriodKey ?: Long.MIN_VALUE)

    /** The epoch-second [period]'s *next* rotation begins, given today's already-computed
     * [periodKey] - purely client-side, no server round-trip needed, since rotation boundaries are
     * a pure function of the calendar. Real `ZonedDateTime` math (not naive `epochDay * 86400`) so
     * a DST-observing zone's countdown never drifts by an hour. Never called for
     * [MissionPeriod.EVENT] - that period's "when does it end" question is
     * [com.suman.memoryarchitect.domain.model.LiveEvent.endEpochSecond], not a rotation boundary. */
    fun nextPeriodStartEpochSecond(period: MissionPeriod, periodKey: Long, zone: java.time.ZoneId): Long {
        val nextKeyStartEpochDay = when (period) {
            MissionPeriod.DAILY -> periodKey + 1
            MissionPeriod.WEEKLY -> (periodKey + 1) * 7L
            MissionPeriod.MONTHLY -> (periodKey + 1) * 30L
            MissionPeriod.EVENT -> throw IllegalArgumentException("MissionPeriod.EVENT has no rotation boundary - use LiveEvent.endEpochSecond")
        }
        return java.time.LocalDate.ofEpochDay(nextKeyStartEpochDay).atStartOfDay(zone).toEpochSecond()
    }

    /** The first candidate periodKey strictly after [fromPeriodKey] whose active set (for [period])
     * isn't identical to [justFinishedActiveIds] - the "not the same as last missions" guarantee
     * [com.suman.memoryarchitect.domain.repository.MissionRepository.unlockAllMissionsEarly] needs.
     * Bounded at 20 tries purely defensively - given [MissionRotationRules]' pool sizes, a repeat
     * full-set match on consecutive periodKeys is already astronomically unlikely, so this is
     * expected to return on its very first attempt in practice. Full disjointness (sharing *no*
     * id at all) isn't required or even always possible - every pool here has more active slots
     * than half its size (e.g. Weekly/Monthly: 3 active of 5 - two 3-subsets of a 5-pool always
     * share at least one id by pigeonhole), so "not identical as a whole set" is the achievable,
     * faithful reading of the request. */
    fun nextDifferentPeriodKey(period: MissionPeriod, fromPeriodKey: Long, justFinishedActiveIds: Set<MissionId>): Long {
        var candidate = fromPeriodKey + 1
        repeat(20) {
            if (activeMissionIds(period, candidate).toSet() != justFinishedActiveIds) return candidate
            candidate++
        }
        return candidate
    }
}
