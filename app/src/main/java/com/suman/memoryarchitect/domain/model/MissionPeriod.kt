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

    /** Only ever active alongside a live [com.suman.memoryarchitect.domain.model.LiveEvent] - see
     * [com.suman.memoryarchitect.domain.progression.MissionCatalog.periodKeyFor]'s doc for why its
     * `periodKey` is the event's own start epoch rather than an epoch-day bucket. */
    EVENT,
}
