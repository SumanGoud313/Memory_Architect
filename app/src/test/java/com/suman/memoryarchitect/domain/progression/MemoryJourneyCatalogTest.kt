package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.MemoryJourneyTierId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryJourneyCatalogTest {

    @Test
    fun `zero points has no current tier and the first tier as next`() {
        val standing = MemoryJourneyCatalog.standingFor(0L)

        assertNull(standing.current)
        assertEquals(MemoryJourneyTierId.KEEPER_OF_MEMORIES, standing.next?.id)
        assertEquals(0f, standing.progressToNext, 0.001f)
    }

    @Test
    fun `points exactly at a tier threshold unlock that tier`() {
        val standing = MemoryJourneyCatalog.standingFor(300L)

        assertEquals(MemoryJourneyTierId.DILIGENT_ARCHIVIST, standing.current?.id)
    }

    @Test
    fun `points one below a threshold do not unlock it yet`() {
        val standing = MemoryJourneyCatalog.standingFor(299L)

        assertEquals(MemoryJourneyTierId.KEEPER_OF_MEMORIES, standing.current?.id)
        assertEquals(MemoryJourneyTierId.DILIGENT_ARCHIVIST, standing.next?.id)
    }

    @Test
    fun `progressToNext is the fraction through the gap between current and next thresholds`() {
        // KEEPER_OF_MEMORIES=100, DILIGENT_ARCHIVIST=300 - halfway through that 200-point gap is 200.
        val standing = MemoryJourneyCatalog.standingFor(200L)

        assertEquals(0.5f, standing.progressToNext, 0.001f)
    }

    @Test
    fun `the highest tier has no next tier and reports full progress`() {
        val topThreshold = MemoryJourneyCatalog.tiers.last().thresholdPoints
        val standing = MemoryJourneyCatalog.standingFor(topThreshold + 100_000L)

        assertEquals(MemoryJourneyTierId.ETERNAL_MEMORY_KEEPER, standing.current?.id)
        assertNull(standing.next)
        assertEquals(1f, standing.progressToNext, 0.001f)
    }

    @Test
    fun `tiers are strictly increasing in threshold`() {
        val thresholds = MemoryJourneyCatalog.tiers.map { it.thresholdPoints }
        assertTrue(thresholds == thresholds.sorted() && thresholds.distinct().size == thresholds.size)
    }

    @Test
    fun `definitionFor returns the matching tier for every id`() {
        MemoryJourneyTierId.entries.forEach { id ->
            assertEquals(id, MemoryJourneyCatalog.definitionFor(id).id)
        }
    }
}
