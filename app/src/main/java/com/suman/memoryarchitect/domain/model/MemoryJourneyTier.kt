package com.suman.memoryarchitect.domain.model

/** One entry in [com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog] - static
 * catalog data, not per-player state (see [MemoryJourneyStanding] for that). Mirrors
 * `mock-backend/journeyRewards.js`'s (nonexistent, by design - see the catalog's own doc) client-
 * only mirror: unlike missions/streak shields, tier *thresholds* need no server copy, since the
 * server only ever needs to bound how many points one write can plausibly grant, never recompute
 * which tier that total lands in. */
data class MemoryJourneyTier(
    val id: MemoryJourneyTierId,
    val thresholdPoints: Long,
    val title: String,
)
