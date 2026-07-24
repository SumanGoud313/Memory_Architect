package com.suman.memoryarchitect.domain.model

/** A time-windowed featured-catalog overlay - see
 * [com.suman.memoryarchitect.domain.progression.LiveEventCatalog]. Adding a real seasonal drop
 * later (Halloween/Christmas/etc.) is exactly one new entry here plus the featured
 * [CosmeticDefinition]s in [com.suman.memoryarchitect.domain.progression.ShopCatalog] - no new
 * code path. */
data class LiveEvent(
    val id: String,
    val startEpochSecond: Long,
    val endEpochSecond: Long,
    val featuredCosmeticIds: List<CosmeticId>,
)
