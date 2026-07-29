package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class MissionCatalogTest {

    @Test
    fun `activeMissionIds returns exactly the configured active count for each period`() {
        assertEquals(3, MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 100L).size)
        assertEquals(3, MissionCatalog.activeMissionIds(MissionPeriod.WEEKLY, periodKey = 14L).size)
        assertEquals(3, MissionCatalog.activeMissionIds(MissionPeriod.MONTHLY, periodKey = 3L).size)
        assertEquals(3, MissionCatalog.activeMissionIds(MissionPeriod.EVENT, periodKey = 1_700_000_000L).size)
    }

    @Test
    fun `activeMissionIds for EVENT always returns the entire pool regardless of periodKey`() {
        // Pool size equals MissionRotationRules.Default.activeEventCount (3), so - unlike the
        // other periods - there's no partial rotation to observe here: every Event mission is
        // always active whenever any event is live.
        val first = MissionCatalog.activeMissionIds(MissionPeriod.EVENT, periodKey = 1L).toSet()
        val second = MissionCatalog.activeMissionIds(MissionPeriod.EVENT, periodKey = 999_999L).toSet()
        assertEquals(setOf(MissionId.EVENT_CLEAR_FIVE_LEVELS, MissionId.EVENT_EARN_FORTY_STARS, MissionId.EVENT_EARN_1000_COINS), first)
        assertEquals(first, second)
    }

    @Test
    fun `activeMissionIds never returns duplicates`() {
        val active = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 42L)
        assertEquals(active.size, active.toSet().size)
    }

    @Test
    fun `activeMissionIds is deterministic - same period and periodKey always yields the same set`() {
        val first = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 500L)
        val second = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 500L)

        assertEquals(first, second)
    }

    @Test
    fun `activeMissionIds varies across different periodKeys`() {
        // Not a strict mathematical guarantee (a hash collision across all three days is possible
        // in principle) but astronomically unlikely for a 9-item pool sampled over 30 consecutive
        // days - a real regression (e.g. periodKey silently ignored) would fail this immediately.
        val distinctSets = (0 until 30L).map { day -> MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = day) }.toSet()
        assertTrue(distinctSets.size > 1)
    }

    @Test
    fun `every mission definition belongs to exactly the period its own pool key claims`() {
        MissionPeriod.entries.forEach { period ->
            val periodKey = MissionCatalog.periodKeyFor(period, todayEpochDay = 1000L, activeEventStartEpochSecond = 1_700_000_000L)
            val active = MissionCatalog.activeMissionIds(period, periodKey)
            active.forEach { missionId ->
                assertEquals(period, MissionCatalog.definitionFor(missionId).period)
            }
        }
    }

    @Test
    fun `periodKeyFor returns the given event start epoch for EVENT`() {
        assertEquals(1_700_000_000L, MissionCatalog.periodKeyFor(MissionPeriod.EVENT, todayEpochDay = 1000L, activeEventStartEpochSecond = 1_700_000_000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `periodKeyFor throws for EVENT with no active event start epoch`() {
        MissionCatalog.periodKeyFor(MissionPeriod.EVENT, todayEpochDay = 1000L)
    }

    @Test
    fun `activeMissionIds respects a custom rotation rule count`() {
        val active = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 7L, rules = MissionRotationRules(activeDailyCount = 1))
        assertEquals(1, active.size)
    }

    @Test
    fun `periodKeyFor uses the epoch day directly for DAILY`() {
        assertEquals(12345L, MissionCatalog.periodKeyFor(MissionPeriod.DAILY, todayEpochDay = 12345L))
    }

    @Test
    fun `periodKeyFor buckets WEEKLY into seven-day windows`() {
        assertEquals(0L, MissionCatalog.periodKeyFor(MissionPeriod.WEEKLY, todayEpochDay = 0L))
        assertEquals(0L, MissionCatalog.periodKeyFor(MissionPeriod.WEEKLY, todayEpochDay = 6L))
        assertEquals(1L, MissionCatalog.periodKeyFor(MissionPeriod.WEEKLY, todayEpochDay = 7L))
    }

    @Test
    fun `activeMissionIds matches mock-backend missions js's output for a fixed periodKey`() {
        // Cross-platform parity check - hand-verified against `node -e` running
        // mock-backend/missions.js's activeMissionIds('DAILY', 1000), which returned exactly
        // [UNLOCK_A_COSMETIC, WATCH_A_REWARDED_AD, CLEAR_TWO_LEVELS] in that order (re-verified
        // against the fmix32-based rotation fix - see MissionCatalog.fmix32's own doc for why the
        // previous string-concatenation scheme needed replacing). A regression in either the
        // Kotlin or JS hashStringToSeed/fmix32/sort logic breaking this agreement is exactly what
        // would let a legitimate claim get rejected as "not part of today's rotation."
        val active = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 1000L)
        assertEquals(listOf(MissionId.UNLOCK_A_COSMETIC, MissionId.WATCH_A_REWARDED_AD, MissionId.CLEAR_TWO_LEVELS), active)
    }

    @Test
    fun `periodKeyFor buckets MONTHLY into thirty-day windows`() {
        assertEquals(0L, MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 0L))
        assertEquals(0L, MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 29L))
        assertEquals(1L, MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 30L))
    }

    @Test
    fun `effectivePeriodKey returns the natural key when no forced key is set`() {
        val natural = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, todayEpochDay = 1000L)
        assertEquals(natural, MissionCatalog.effectivePeriodKey(MissionPeriod.DAILY, todayEpochDay = 1000L, forcedPeriodKey = null))
    }

    @Test
    fun `effectivePeriodKey prefers the forced key while it's still ahead of the natural key`() {
        val natural = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, todayEpochDay = 1000L)
        val forced = natural + 5
        assertEquals(forced, MissionCatalog.effectivePeriodKey(MissionPeriod.DAILY, todayEpochDay = 1000L, forcedPeriodKey = forced))
    }

    @Test
    fun `effectivePeriodKey self-heals back to the natural key once real time catches up past it`() {
        val forced = 500L
        // "Today" has advanced far enough that the natural key now exceeds the old forced one.
        val laterNatural = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, todayEpochDay = 10_000L)
        assertTrue(laterNatural > forced)
        assertEquals(laterNatural, MissionCatalog.effectivePeriodKey(MissionPeriod.DAILY, todayEpochDay = 10_000L, forcedPeriodKey = forced))
    }

    @Test
    fun `nextPeriodStartEpochSecond for DAILY lands at the start of the following day`() {
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, todayEpochDay = 19_000L)
        val nextStart = MissionCatalog.nextPeriodStartEpochSecond(MissionPeriod.DAILY, periodKey, ZoneOffset.UTC)
        val expected = java.time.LocalDate.ofEpochDay(19_001L).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        assertEquals(expected, nextStart)
    }

    @Test
    fun `nextPeriodStartEpochSecond for WEEKLY and MONTHLY land seven and thirty days into the next bucket`() {
        val weeklyKey = MissionCatalog.periodKeyFor(MissionPeriod.WEEKLY, todayEpochDay = 700L)
        val weeklyNext = MissionCatalog.nextPeriodStartEpochSecond(MissionPeriod.WEEKLY, weeklyKey, ZoneOffset.UTC)
        assertEquals(java.time.LocalDate.ofEpochDay((weeklyKey + 1) * 7).atStartOfDay(ZoneOffset.UTC).toEpochSecond(), weeklyNext)

        val monthlyKey = MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 700L)
        val monthlyNext = MissionCatalog.nextPeriodStartEpochSecond(MissionPeriod.MONTHLY, monthlyKey, ZoneOffset.UTC)
        assertEquals(java.time.LocalDate.ofEpochDay((monthlyKey + 1) * 30).atStartOfDay(ZoneOffset.UTC).toEpochSecond(), monthlyNext)
    }

    @Test
    fun `nextDifferentPeriodKey never returns a candidate whose active set matches the one just finished`() {
        (0 until 50L).forEach { periodKey ->
            val justFinished = MissionCatalog.activeMissionIds(MissionPeriod.WEEKLY, periodKey).toSet()
            val candidate = MissionCatalog.nextDifferentPeriodKey(MissionPeriod.WEEKLY, periodKey, justFinished)
            assertTrue(candidate > periodKey)
            assertTrue(MissionCatalog.activeMissionIds(MissionPeriod.WEEKLY, candidate).toSet() != justFinished)
        }
    }

    @Test
    fun `MissionCategoryBonusCatalog has no bonus for EVENT and a non-empty reward for the other three`() {
        assertEquals(null, MissionCategoryBonusCatalog.forPeriod(MissionPeriod.EVENT))

        val daily = requireNotNull(MissionCategoryBonusCatalog.forPeriod(MissionPeriod.DAILY))
        assertTrue(daily.coinRange.first >= 100L)
        assertTrue("Daily's bonus must be coins-only", daily.inventoryGrants.isEmpty())

        val weekly = requireNotNull(MissionCategoryBonusCatalog.forPeriod(MissionPeriod.WEEKLY))
        assertTrue(weekly.coinRange.first >= 200L)
        assertTrue(weekly.xp > 0L)
        assertEquals(1, weekly.inventoryGrants[InventoryItemKind.LUCKY_SPIN_TICKET])

        val monthly = requireNotNull(MissionCategoryBonusCatalog.forPeriod(MissionPeriod.MONTHLY))
        assertTrue(monthly.coinRange.first >= 350L)
        assertEquals(1, monthly.inventoryGrants[InventoryItemKind.LUCKY_SPIN_TICKET])
        assertEquals(1, monthly.inventoryGrants[InventoryItemKind.REDO_TOKEN])
        assertEquals(1, monthly.inventoryGrants[InventoryItemKind.HINT_TOKEN])
    }

    @Test
    fun `MissionCategoryBonusCatalog roll never returns EVENT and always stays within its own coinRange`() {
        assertEquals(null, MissionCategoryBonusCatalog.roll(MissionPeriod.EVENT))

        val random = kotlin.random.Random(7)
        MissionPeriod.entries.filter { it != MissionPeriod.EVENT }.forEach { period ->
            val range = requireNotNull(MissionCategoryBonusCatalog.forPeriod(period)).coinRange
            repeat(200) {
                val reward = requireNotNull(MissionCategoryBonusCatalog.roll(period, random))
                assertTrue("$period rolled ${reward.coins}, outside $range", reward.coins in range)
            }
        }
    }
}
