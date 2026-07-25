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
    val activeMonthlyCount: Int = 1,
) {
    fun activeCountFor(period: MissionPeriod): Int = when (period) {
        MissionPeriod.DAILY -> activeDailyCount
        MissionPeriod.WEEKLY -> activeWeeklyCount
        MissionPeriod.MONTHLY -> activeMonthlyCount
    }

    companion object {
        val Default = MissionRotationRules()
    }
}
