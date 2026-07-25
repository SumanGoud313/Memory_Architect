package com.suman.memoryarchitect.domain.model

/**
 * What a [MissionDefinition] counts progress against - each maps to exactly one [MissionEvent]
 * variant that [com.suman.memoryarchitect.domain.progression.MissionProgressRules] increments on.
 * Deliberately drawn from things a player would plausibly do anyway (see the plan doc) - nothing
 * here requires a mode played exclusively for the mission's sake, and [WATCH_REWARDED_AD] is the
 * one type [com.suman.memoryarchitect.domain.progression.MissionRotationRules] caps to at most one
 * active mission at a time, since it's the only requirement that's ever a pure ad-watch grind.
 */
enum class MissionRequirementType {
    COMPLETE_LEVELS,
    COMPLETE_PRACTICE_ROUNDS,
    COMPLETE_DAILY_CHALLENGE,
    COMPLETE_WEEKLY_CHALLENGE,
    EARN_COINS,
    ZERO_HINT_LEVEL_CLEAR,
    HIGH_ACCURACY_CLEAR,
    EARN_STARS,
    UNLOCK_COSMETIC,
    EQUIP_COSMETIC,
    WATCH_REWARDED_AD,
}
