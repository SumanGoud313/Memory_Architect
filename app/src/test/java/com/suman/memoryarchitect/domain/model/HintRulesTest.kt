package com.suman.memoryarchitect.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HintRulesTest {

    private val rules = HintRules.Default

    @Test
    fun `level 1 allows exactly one hint`() {
        assertEquals(1, rules.hintsAllowedForLevel(1))
    }

    @Test
    fun `level 24 - the last tier-one level - still allows exactly one hint`() {
        assertEquals(1, rules.hintsAllowedForLevel(24))
    }

    @Test
    fun `level 25 - the first tier-two level - allows two hints`() {
        assertEquals(2, rules.hintsAllowedForLevel(25))
    }

    @Test
    fun `level 100 allows two hints`() {
        assertEquals(2, rules.hintsAllowedForLevel(100))
    }
}
