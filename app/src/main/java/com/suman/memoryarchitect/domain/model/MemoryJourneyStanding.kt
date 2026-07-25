package com.suman.memoryarchitect.domain.model

/** A player's current standing on the Memory Journey - mirrors [com.suman.memoryarchitect.domain.progression.RankStanding]'s
 * shape exactly. [current] is null only if [totalPoints] hasn't reached even the first tier yet. */
data class MemoryJourneyStanding(
    val totalPoints: Long,
    val current: MemoryJourneyTier?,
    val next: MemoryJourneyTier?,
    val progressToNext: Float,
)
