package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.ReturningPlayerTier
import org.junit.Assert.assertEquals
import org.junit.Test

class ReturningPlayerRulesTest {

    private val rules = ReturningPlayerRules.Default

    @Test
    fun `tierFor is NONE below the short-gap threshold`() {
        assertEquals(ReturningPlayerTier.NONE, rules.tierFor(0))
        assertEquals(ReturningPlayerTier.NONE, rules.tierFor(2))
    }

    @Test
    fun `tierFor is SHORT from the short-gap threshold up to medium`() {
        assertEquals(ReturningPlayerTier.SHORT, rules.tierFor(3))
        assertEquals(ReturningPlayerTier.SHORT, rules.tierFor(6))
    }

    @Test
    fun `tierFor is MEDIUM from the medium-gap threshold up to long`() {
        assertEquals(ReturningPlayerTier.MEDIUM, rules.tierFor(7))
        assertEquals(ReturningPlayerTier.MEDIUM, rules.tierFor(29))
    }

    @Test
    fun `tierFor is LONG at or beyond the long-gap threshold`() {
        assertEquals(ReturningPlayerTier.LONG, rules.tierFor(30))
        assertEquals(ReturningPlayerTier.LONG, rules.tierFor(365))
    }
}
