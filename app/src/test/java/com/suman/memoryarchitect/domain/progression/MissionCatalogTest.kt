package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionCatalogTest {

    @Test
    fun `activeMissionIds returns exactly the configured active count for each period`() {
        assertEquals(3, MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 100L).size)
        assertEquals(3, MissionCatalog.activeMissionIds(MissionPeriod.WEEKLY, periodKey = 14L).size)
        assertEquals(1, MissionCatalog.activeMissionIds(MissionPeriod.MONTHLY, periodKey = 3L).size)
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
            val periodKey = MissionCatalog.periodKeyFor(period, todayEpochDay = 1000L)
            val active = MissionCatalog.activeMissionIds(period, periodKey)
            active.forEach { missionId ->
                assertEquals(period, MissionCatalog.definitionFor(missionId).period)
            }
        }
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
        // [ZERO_HINT_CLEAR, EARN_150_COINS, WIN_DAILY_CHALLENGE] in that order. A regression in
        // either the Kotlin or JS hashStringToSeed/sort logic breaking this agreement is exactly
        // what would let a legitimate claim get rejected as "not part of today's rotation."
        val active = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey = 1000L)
        assertEquals(listOf(MissionId.ZERO_HINT_CLEAR, MissionId.EARN_150_COINS, MissionId.WIN_DAILY_CHALLENGE), active)
    }

    @Test
    fun `periodKeyFor buckets MONTHLY into thirty-day windows`() {
        assertEquals(0L, MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 0L))
        assertEquals(0L, MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 29L))
        assertEquals(1L, MissionCatalog.periodKeyFor(MissionPeriod.MONTHLY, todayEpochDay = 30L))
    }
}
