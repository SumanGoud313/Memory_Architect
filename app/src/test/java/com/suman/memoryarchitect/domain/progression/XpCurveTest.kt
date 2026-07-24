package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XpCurveTest {

    private val curve = XpCurve()

    @Test
    fun `level 1 requires zero xp`() {
        assertEquals(0L, curve.xpRequiredForLevel(1))
        assertEquals(1, curve.levelForXp(0L))
    }

    @Test
    fun `xp required for each level strictly increases`() {
        val requirements = (1..10).map { curve.xpRequiredForLevel(it) }
        requirements.zipWithNext().forEach { (lower, higher) ->
            assertTrue(higher > lower)
        }
    }

    @Test
    fun `levelForXp is the inverse of xpRequiredForLevel at each boundary`() {
        for (level in 1..10) {
            val xpAtBoundary = curve.xpRequiredForLevel(level)
            assertEquals(level, curve.levelForXp(xpAtBoundary))
        }
    }

    @Test
    fun `xp just below a level boundary does not yet count as that level`() {
        val boundary = curve.xpRequiredForLevel(5)
        assertEquals(4, curve.levelForXp(boundary - 1))
    }

    @Test
    fun `xpProgressWithinLevel is zero right at a level boundary`() {
        val boundary = curve.xpRequiredForLevel(4)

        val (into, forNext) = curve.xpProgressWithinLevel(boundary)

        assertEquals(0L, into)
        assertEquals(curve.xpRequiredForLevel(5) - boundary, forNext)
    }

    @Test
    fun `xpProgressWithinLevel tracks partial progress into the current level`() {
        val floor = curve.xpRequiredForLevel(3)
        val ceiling = curve.xpRequiredForLevel(4)
        val midpoint = floor + (ceiling - floor) / 2

        val (into, forNext) = curve.xpProgressWithinLevel(midpoint)

        assertEquals(midpoint - floor, into)
        assertEquals(ceiling - floor, forNext)
    }
}
