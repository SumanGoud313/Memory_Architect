package com.suman.memoryarchitect.domain.generation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelMirrorPolicyTest {

    @Test
    fun `isMirrored is deterministic for a given seed`() {
        val seed = 12345L

        val first = LevelMirrorPolicy.isMirrored(seed)
        val second = LevelMirrorPolicy.isMirrored(seed)

        assertEquals(first, second)
    }

    @Test
    fun `isMirrored produces both outcomes across many seeds`() {
        val outcomes = (0L until 50L).map { LevelMirrorPolicy.isMirrored(it) }.toSet()

        assertTrue(outcomes.contains(true))
        assertTrue(outcomes.contains(false))
    }
}
