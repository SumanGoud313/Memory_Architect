package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRewardCatalogTest {

    @Test
    fun `first ever claim starts the cycle at day one`() {
        val day = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay = null, currentCycleDay = 0, todayEpochDay = 100L)

        assertEquals(1, day)
    }

    @Test
    fun `claiming again the same day does not advance the cycle`() {
        val day = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay = 100L, currentCycleDay = 3, todayEpochDay = 100L)

        assertEquals(3, day)
    }

    @Test
    fun `claiming on the very next day advances the cycle`() {
        val day = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay = 100L, currentCycleDay = 3, todayEpochDay = 101L)

        assertEquals(4, day)
    }

    @Test
    fun `claiming the day after day seven wraps back to day one`() {
        val day = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay = 100L, currentCycleDay = 7, todayEpochDay = 101L)

        assertEquals(1, day)
    }

    @Test
    fun `a missed day quietly restarts the cycle rather than penalizing further`() {
        val day = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay = 90L, currentCycleDay = 5, todayEpochDay = 101L)

        assertEquals(1, day)
    }

    @Test
    fun `can claim as long as today is not already the last claimed day`() {
        assertTrue(DailyRewardCatalog.canClaim(lastClaimedEpochDay = 99L, todayEpochDay = 100L))
        assertTrue(DailyRewardCatalog.canClaim(lastClaimedEpochDay = null, todayEpochDay = 100L))
        assertFalse(DailyRewardCatalog.canClaim(lastClaimedEpochDay = 100L, todayEpochDay = 100L))
    }

    @Test
    fun `every day in the cycle has a defined, non-negative reward`() {
        assertEquals(7, DailyRewardCatalog.entries.size)
        DailyRewardCatalog.entries.forEach { entry ->
            assertTrue("day ${entry.day} should award coins", entry.coins > 0)
            assertTrue("day ${entry.day} xp should never be negative", entry.xp >= 0)
        }
    }

    @Test
    fun `the final day of the cycle is the biggest reward, never a step down`() {
        val coinsByDay = DailyRewardCatalog.entries.sortedBy { it.day }.map { it.coins }

        assertEquals(coinsByDay.max(), coinsByDay.last())
    }

    @Test
    fun `entryForDay clamps out-of-range days instead of crashing`() {
        assertEquals(DailyRewardCatalog.entries.first(), DailyRewardCatalog.entryForDay(0))
        assertEquals(DailyRewardCatalog.entries.last(), DailyRewardCatalog.entryForDay(99))
    }
}
