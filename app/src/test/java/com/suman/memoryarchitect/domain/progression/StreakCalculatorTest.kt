package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val calculator = StreakCalculator()

    @Test
    fun `first ever play starts a streak of one`() {
        val (current, longest) = calculator.updateStreak(
            lastPlayedEpochDay = null,
            todayEpochDay = 100L,
            previousCurrentStreak = 0,
            previousLongestStreak = 0,
        )

        assertEquals(1, current)
        assertEquals(1, longest)
    }

    @Test
    fun `playing again the same day does not change the streak`() {
        val (current, _) = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 100L,
            previousCurrentStreak = 5,
            previousLongestStreak = 5,
        )

        assertEquals(5, current)
    }

    @Test
    fun `playing on the very next day extends the streak`() {
        val (current, longest) = calculator.updateStreak(
            lastPlayedEpochDay = 100L,
            todayEpochDay = 101L,
            previousCurrentStreak = 5,
            previousLongestStreak = 5,
        )

        assertEquals(6, current)
        assertEquals(6, longest)
    }

    @Test
    fun `a gap of more than one day resets the streak but keeps the longest record`() {
        val (current, longest) = calculator.updateStreak(
            lastPlayedEpochDay = 90L,
            todayEpochDay = 101L,
            previousCurrentStreak = 5,
            previousLongestStreak = 5,
        )

        assertEquals(1, current)
        assertEquals(5, longest)
    }
}
