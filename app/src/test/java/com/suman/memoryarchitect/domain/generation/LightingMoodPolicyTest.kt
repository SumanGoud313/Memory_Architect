package com.suman.memoryarchitect.domain.generation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightingMoodPolicyTest {

    @Test
    fun `forSeed is deterministic for a given seed`() {
        val seed = 9001L

        assertEquals(LightingMoodPolicy.forSeed(seed), LightingMoodPolicy.forSeed(seed))
    }

    @Test
    fun `forSeed produces every mood across many seeds, with DAY as the common case`() {
        val moods = (0L until 200L).map { LightingMoodPolicy.forSeed(it) }

        LightingMood.entries.forEach { mood -> assertTrue("expected at least one $mood", moods.contains(mood)) }
        val dayCount = moods.count { it == LightingMood.DAY }
        assertTrue("DAY should be the plurality outcome, was $dayCount/${moods.size}", dayCount > moods.size / 3)
    }
}
