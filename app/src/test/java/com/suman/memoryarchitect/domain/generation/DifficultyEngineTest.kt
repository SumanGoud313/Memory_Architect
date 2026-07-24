package com.suman.memoryarchitect.domain.generation

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyEngineTest {

    private val engine = DifficultyEngine()

    @Test
    fun `higher tier does not decrease object count`() {
        val beginner = engine.computeConstraints(DifficultyTier.BEGINNER, streak = 0, mode = GameMode.WEEKLY_CHALLENGE)
        val expert = engine.computeConstraints(DifficultyTier.EXPERT, streak = 0, mode = GameMode.WEEKLY_CHALLENGE)

        assertTrue(expert.objectCount >= beginner.objectCount)
    }

    @Test
    fun `higher tier does not increase memorize duration or time limit`() {
        val beginner = engine.computeConstraints(DifficultyTier.BEGINNER, streak = 0, mode = GameMode.WEEKLY_CHALLENGE)
        val expert = engine.computeConstraints(DifficultyTier.EXPERT, streak = 0, mode = GameMode.WEEKLY_CHALLENGE)

        assertTrue(expert.memorizeDurationMs <= beginner.memorizeDurationMs)
        assertTrue(requireNotNull(expert.timeLimitMs) <= requireNotNull(beginner.timeLimitMs))
    }

    @Test
    fun `practice mode has no time limit`() {
        val constraints = engine.computeConstraints(DifficultyTier.EXPERT, streak = 100, mode = GameMode.PRACTICE)

        assertNull(constraints.timeLimitMs)
    }

    @Test
    fun `rotation is disabled for practice mode regardless of tier or streak`() {
        val constraints = engine.computeConstraints(DifficultyTier.EXPERT, streak = 100, mode = GameMode.PRACTICE)

        assertFalse(constraints.rotationEnabled)
    }

    @Test
    fun `practice mode always gets a flat 12 second memorize window, regardless of tier or streak`() {
        val beginner = engine.computeConstraints(DifficultyTier.BEGINNER, streak = 0, mode = GameMode.PRACTICE)
        val expert = engine.computeConstraints(DifficultyTier.EXPERT, streak = 100, mode = GameMode.PRACTICE)

        assertEquals(12_000L, beginner.memorizeDurationMs)
        assertEquals(12_000L, expert.memorizeDurationMs)
    }
}
