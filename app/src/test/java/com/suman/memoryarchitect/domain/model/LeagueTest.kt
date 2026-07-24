package com.suman.memoryarchitect.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LeagueTest {

    @Test
    fun `forXp returns Apprentice for zero and any xp below the first real threshold`() {
        assertEquals(League.APPRENTICE, League.forXp(0L))
        assertEquals(League.APPRENTICE, League.forXp(999L))
    }

    @Test
    fun `forXp is inclusive at each threshold's exact boundary`() {
        assertEquals(League.JOURNEYMAN, League.forXp(1_000L))
        assertEquals(League.ARCHITECT, League.forXp(5_000L))
        assertEquals(League.MASTER_ARCHITECT, League.forXp(20_000L))
        assertEquals(League.GRANDMASTER, League.forXp(50_000L))
    }

    @Test
    fun `forXp one below a threshold stays in the lower league`() {
        assertEquals(League.APPRENTICE, League.forXp(999L))
        assertEquals(League.JOURNEYMAN, League.forXp(4_999L))
        assertEquals(League.ARCHITECT, League.forXp(19_999L))
        assertEquals(League.MASTER_ARCHITECT, League.forXp(49_999L))
    }

    @Test
    fun `forXp far above the top threshold stays Grandmaster`() {
        assertEquals(League.GRANDMASTER, League.forXp(1_000_000L))
    }
}
