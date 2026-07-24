package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.LiveEvent

/**
 * Data-driven Live Events framework - structure only, no real seasonal content this pass (see the
 * Points Economy plan's "out of scope"). The one entry below is a permanently-expired template:
 * copy its shape for a real event (Halloween/Christmas/New Year/Summer/Anniversary), give it a
 * real window and featured [com.suman.memoryarchitect.domain.model.CosmeticId]s already present in
 * [ShopCatalog], and it lights up automatically - zero new code. [activeEvent] silently returns
 * `null` today because the template's window (epoch 0..1) can never contain a real "now".
 */
object LiveEventCatalog {
    val events: List<LiveEvent> = listOf(
        LiveEvent(
            id = "TEMPLATE_EXAMPLE",
            startEpochSecond = 0L,
            endEpochSecond = 1L,
            featuredCosmeticIds = emptyList(),
        ),
    )

    fun activeEvent(nowEpochSecond: Long): LiveEvent? =
        events.firstOrNull { nowEpochSecond in it.startEpochSecond..it.endEpochSecond }
}
