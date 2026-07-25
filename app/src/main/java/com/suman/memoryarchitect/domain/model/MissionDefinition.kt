package com.suman.memoryarchitect.domain.model

/** One entry in [com.suman.memoryarchitect.domain.progression.MissionCatalog]'s pool - static
 * catalog data, not per-player state (see [MissionProgress] for that). Mirrors
 * `mock-backend/missions.js`'s `MISSION_CATALOG` - keep both in sync. */
data class MissionDefinition(
    val id: MissionId,
    val period: MissionPeriod,
    val requirement: MissionRequirementType,
    val targetCount: Int,
    val reward: MissionReward,
)
