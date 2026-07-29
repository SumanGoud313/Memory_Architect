package com.suman.memoryarchitect.domain.model

/**
 * Point values per existing signal - mirrors `mock-backend/progression.js`/`missions.js`'s own
 * copies of these same numbers, the established "keep Kotlin/JS in sync" convention already used
 * for [StreakRules]. Deliberately modest relative to the coin economy (this is a pure recognition
 * track, never spendable) - see [com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog]'s
 * doc for why nothing here introduces a new source of truth.
 *
 * [pointsPerDailyMissionClaimed]/[pointsPerWeeklyMissionClaimed]/[pointsPerMonthlyMissionClaimed]
 * are a deliberate simplification of the plan doc's "weekly mission set completed" bonus - scaling
 * by period rather than detecting "all three of this week's missions are now claimed" gets the same
 * "missions feed the Journey too" outcome without a second piece of state to track set-completion.
 */
data class MemoryJourneyRules(
    val pointsPerLevelCompleted: Long = 10L,
    val perfectAccuracyBonus: Long = 5L,
    val pointsPerAchievementUnlocked: Long = 25L,
    val pointsPerStreakMilestone: Long = 30L,
    val pointsPerDailyMissionClaimed: Long = 5L,
    val pointsPerWeeklyMissionClaimed: Long = 15L,
    val pointsPerMonthlyMissionClaimed: Long = 50L,
    // Between Daily and Weekly - a typical event window runs a few days, longer than one Daily
    // reset but shorter than a full Weekly cycle.
    val pointsPerEventMissionClaimed: Long = 10L,
) {
    companion object {
        val Default = MemoryJourneyRules()
    }
}
