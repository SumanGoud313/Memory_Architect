package com.suman.memoryarchitect.core.analytics

import com.suman.memoryarchitect.domain.model.MissionId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory, process-lifetime dedup guard for `mission_assigned`/`mission_completed`/
 * `monthly_goal_completed` - the same "process-lifetime is fine for an analytics-only signal"
 * convention [FrustrationTracker] already uses, for the same underlying reason: a fresh
 * `MissionsViewModel` instance is created every time the Missions screen is (re-)navigated to (a
 * new `NavBackStackEntry`), starting its own before/after diff from an empty baseline - without
 * this, every mission already assigned/already complete from a *previous* visit this session would
 * look like a brand-new `mission_assigned`/`mission_completed` on every single re-visit, wildly
 * inflating both counts relative to what actually happened. Keyed by (missionId, periodKey), same
 * as `MissionsViewModel.logMissionDiff`'s own diff key - a mission rotating into a new period is
 * correctly treated as a fresh assignment again, exactly matching the "assigned = a (missionId,
 * periodKey) seen for the first time" intent already documented on [logMissionAssigned].
 */
@Singleton
class MissionAnalyticsBaseline @Inject constructor() {
    private val loggedAssigned = mutableSetOf<Pair<MissionId, Long>>()
    private val loggedCompleted = mutableSetOf<Pair<MissionId, Long>>()

    /** Returns `true` the first time this (missionId, periodKey) is seen - the caller should only
     * log `mission_assigned` when this returns `true`. */
    fun markAssignedIfNew(missionId: MissionId, periodKey: Long): Boolean = loggedAssigned.add(missionId to periodKey)

    /** Returns `true` the first time this (missionId, periodKey) is seen complete - the caller
     * should only log `mission_completed`/`monthly_goal_completed` when this returns `true`. */
    fun markCompletedIfNew(missionId: MissionId, periodKey: Long): Boolean = loggedCompleted.add(missionId to periodKey)
}
