package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {

    private val calculator = StreakCalculator()

    @Test
    fun `first ever play starts a streak of one`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = null,
            todayEpochDay = 100L,
            previousCurrentStreak = 0,
            previousLongestStreak = 0,
            previousStreakShields = 0,
        )

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
        assertFalse(result.shieldConsumed)
        assertFalse(result.shieldGranted)
        assertNull(result.milestoneReached)
    }

    @Test
    fun `playing again the same day does not change the streak or re-fire a milestone`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 100L,
            previousCurrentStreak = 7,
            previousLongestStreak = 7,
            previousStreakShields = 1,
        )

        assertEquals(7, result.currentStreak)
        assertNull(result.milestoneReached)
        assertFalse(result.shieldGranted)
        assertEquals(1, result.streakShields)
    }

    @Test
    fun `playing on the very next day extends the streak`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 101L,
            previousCurrentStreak = 5,
            previousLongestStreak = 5,
            previousStreakShields = 0,
        )

        assertEquals(6, result.currentStreak)
        assertEquals(6, result.longestStreak)
    }

    @Test
    fun `a gap of more than one day resets the streak but keeps the longest record`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 90L,
            todayEpochDay = 101L,
            previousCurrentStreak = 5,
            previousLongestStreak = 5,
            previousStreakShields = 2,
        )

        assertEquals(1, result.currentStreak)
        assertEquals(5, result.longestStreak)
        // A gap this large (11 days) is not the one-day-with-a-shield case - shields are untouched.
        assertEquals(2, result.streakShields)
        assertFalse(result.shieldConsumed)
    }

    @Test
    fun `a one-day gap with a shield banked auto-consumes it and extends the streak instead of resetting`() {
        // previousCurrentStreak deliberately chosen so the resulting streak (11) isn't itself a
        // milestone - isolates "a shield was spent" from "a shield was also just re-granted",
        // which is covered on its own by the milestone-overlap test below.
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 102L, // missed day 101
            previousCurrentStreak = 10,
            previousLongestStreak = 10,
            previousStreakShields = 1,
        )

        assertEquals(11, result.currentStreak)
        assertTrue(result.shieldConsumed)
        assertEquals(0, result.streakShields)
    }

    @Test
    fun `a shield-covered gap that lands exactly on a shield milestone both consumes and re-grants one`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 102L, // missed day 101
            previousCurrentStreak = 6,
            previousLongestStreak = 6,
            previousStreakShields = 1,
        )

        assertEquals(7, result.currentStreak)
        assertEquals(7, result.milestoneReached)
        assertTrue(result.shieldConsumed)
        assertTrue(result.shieldGranted)
        // Consumed the one banked shield, then immediately re-granted one for crossing day 7 -
        // nets back to 1, not 0.
        assertEquals(1, result.streakShields)
    }

    @Test
    fun `a one-day gap with no shield banked resets exactly like a longer gap`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 102L,
            previousCurrentStreak = 6,
            previousLongestStreak = 6,
            previousStreakShields = 0,
        )

        assertEquals(1, result.currentStreak)
        assertFalse(result.shieldConsumed)
    }

    @Test
    fun `crossing a major milestone grants a shield`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 101L,
            previousCurrentStreak = 6,
            previousLongestStreak = 6,
            previousStreakShields = 0,
        )

        assertEquals(7, result.currentStreak)
        assertEquals(7, result.milestoneReached)
        assertTrue(result.shieldGranted)
        assertEquals(1, result.streakShields)
    }

    @Test
    fun `crossing a non-shield milestone does not grant a shield`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 101L,
            previousCurrentStreak = 2,
            previousLongestStreak = 2,
            previousStreakShields = 0,
        )

        assertEquals(3, result.currentStreak)
        assertEquals(3, result.milestoneReached)
        assertFalse(result.shieldGranted)
    }

    @Test
    fun `a milestone crossed while already holding the shield cap does not grant another`() {
        val result = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 101L,
            previousCurrentStreak = 6,
            previousLongestStreak = 6,
            previousStreakShields = 3,
        )

        assertEquals(7, result.milestoneReached)
        assertFalse(result.shieldGranted)
        assertEquals(3, result.streakShields)
    }
}
