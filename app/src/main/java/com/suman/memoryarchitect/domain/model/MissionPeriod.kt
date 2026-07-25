package com.suman.memoryarchitect.domain.model

/**
 * Which rotation cadence a [MissionDefinition] belongs to - see
 * [com.suman.memoryarchitect.domain.progression.MissionRotationRules] for how many are active per
 * period at once, and [com.suman.memoryarchitect.domain.progression.MissionCatalog] for the
 * deterministic seed (epoch day/week/month) each period rotates on.
 */
enum class MissionPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
}
