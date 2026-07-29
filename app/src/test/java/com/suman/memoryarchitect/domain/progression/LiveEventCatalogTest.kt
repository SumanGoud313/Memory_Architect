package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.RemoteConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveEventCatalogTest {

    private fun remoteConfigOf(vararg values: Pair<String, String>) = RemoteConfig(values.toMap(), fetchedAt = 0L)

    @Test
    fun `activeEvent returns null when event_active_id is unset`() {
        val result = LiveEventCatalog.activeEvent(remoteConfigOf(), nowEpochSecond = 1_700_000_000L)
        assertNull(result)
    }

    @Test
    fun `activeEvent returns null when event_active_id is blank`() {
        val result = LiveEventCatalog.activeEvent(remoteConfigOf("event_active_id" to ""), nowEpochSecond = 1_700_000_000L)
        assertNull(result)
    }

    @Test
    fun `activeEvent returns null when event_active_id matches no catalog template`() {
        val result = LiveEventCatalog.activeEvent(remoteConfigOf("event_active_id" to "NOT_A_REAL_EVENT"), nowEpochSecond = 1_700_000_000L)
        assertNull(result)
    }

    @Test
    fun `activeEvent resolves the named template using its own default window`() {
        val template = LiveEventCatalog.events.first { it.id == "HALLOWEEN" }
        val nowInsideWindow = template.startEpochSecond + 1

        val result = LiveEventCatalog.activeEvent(remoteConfigOf("event_active_id" to "HALLOWEEN"), nowEpochSecond = nowInsideWindow)

        assertEquals(template, result)
    }

    @Test
    fun `activeEvent returns null when now falls outside the template's default window`() {
        val template = LiveEventCatalog.events.first { it.id == "HALLOWEEN" }
        val nowBeforeWindow = template.startEpochSecond - 1

        val result = LiveEventCatalog.activeEvent(remoteConfigOf("event_active_id" to "HALLOWEEN"), nowEpochSecond = nowBeforeWindow)

        assertNull(result)
    }

    @Test
    fun `activeEvent honors explicit start and end epoch overrides`() {
        val config = remoteConfigOf(
            "event_active_id" to "HALLOWEEN",
            "event_start_epoch" to "1000",
            "event_end_epoch" to "2000",
        )

        assertNull(LiveEventCatalog.activeEvent(config, nowEpochSecond = 999L))
        val resolved = LiveEventCatalog.activeEvent(config, nowEpochSecond = 1500L)
        assertEquals(1000L, resolved?.startEpochSecond)
        assertEquals(2000L, resolved?.endEpochSecond)
        assertNull(LiveEventCatalog.activeEvent(config, nowEpochSecond = 2001L))
    }

    @Test
    fun `activeEvent falls back to the template's default window when overrides are zero`() {
        val template = LiveEventCatalog.events.first { it.id == "HALLOWEEN" }
        val config = remoteConfigOf(
            "event_active_id" to "HALLOWEEN",
            "event_start_epoch" to "0",
            "event_end_epoch" to "0",
        )

        val resolved = LiveEventCatalog.activeEvent(config, nowEpochSecond = template.startEpochSecond + 1)

        assertEquals(template.startEpochSecond, resolved?.startEpochSecond)
        assertEquals(template.endEpochSecond, resolved?.endEpochSecond)
    }

    @Test
    fun `every template has a unique id and at least one featured cosmetic`() {
        assertEquals(LiveEventCatalog.events.size, LiveEventCatalog.events.map { it.id }.toSet().size)
        LiveEventCatalog.events.forEach { event ->
            assert(event.featuredCosmeticIds.isNotEmpty()) { "${event.id} has no featured cosmetics" }
        }
    }
}
