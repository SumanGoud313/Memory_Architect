package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.MissionPeriod

/** Tuning for [MissionCatalog]'s rotation - mirrors `mock-backend/missions.js`'s own copy of these
 * same numbers, the established "keep Kotlin/JS in sync" convention already used for
 * [StreakRules][com.suman.memoryarchitect.domain.model.StreakRules]. There's no separate cap for
 * the one ad-watch mission ([com.suman.memoryarchitect.domain.model.MissionRequirementType.WATCH_REWARDED_AD]) -
 * it's a single [com.suman.memoryarchitect.domain.model.MissionDefinition] in the daily pool like
 * any other, so it can never be selected twice and is never guaranteed active on a given day. */
data class MissionRotationRules(
    val activeDailyCount: Int = 3,
    val activeWeeklyCount: Int = 3,
    // 3, matching Daily/Weekly's shape - was 1 against a pool of 3 (no real rotation at all).
    // The pool is now 5 (see MissionCatalog's Monthly section), so a genuine 3-of-5 rotation
    // exists, with headroom for the "never repeats the just-finished set" pay-to-reroll guarantee
    // (see MissionRepository.unlockAllMissionsEarly's doc).
    val activeMonthlyCount: Int = 3,
    // Equal to the entire Event pool's size (see MissionCatalog.definitions) - unlike the other
    // periods, an event's whole window is short and rare enough that every Event mission should
    // just show up together rather than only a rotating subset of them.
    val activeEventCount: Int = 3,
) {
    fun activeCountFor(period: MissionPeriod): Int = when (period) {
        MissionPeriod.DAILY -> activeDailyCount
        MissionPeriod.WEEKLY -> activeWeeklyCount
        MissionPeriod.MONTHLY -> activeMonthlyCount
        MissionPeriod.EVENT -> activeEventCount
    }

    companion object {
        val Default = MissionRotationRules()
    }
}
