package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRankCatalogTest {

    @Test
    fun `level 1 is Novice`() {
        assertEquals("Novice", MemoryRankCatalog.rankFor(1))
    }

    @Test
    fun `level 100 is Eternal Architect`() {
        assertEquals("Eternal Architect", MemoryRankCatalog.rankFor(100))
    }

    @Test
    fun `band boundaries are inclusive on the lower edge`() {
        assertEquals("Novice", MemoryRankCatalog.rankFor(9))
        assertEquals("Apprentice", MemoryRankCatalog.rankFor(10))
    }

    @Test
    fun `rank is monotonic non-decreasing as level increases`() {
        val order = listOf(
            "Novice", "Apprentice", "Journeyman", "Adept", "Architect",
            "Senior Architect", "Master Architect", "Grandmaster Architect", "Eternal Architect",
        )
        var lastIndex = -1
        (1..100).forEach { level ->
            val rank = MemoryRankCatalog.rankFor(level)
            val index = order.indexOf(rank)
            assertTrue("rank regressed at level $level: $rank", index >= lastIndex)
            lastIndex = index
        }
    }
}
