package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.Outcome

interface MissionRepository {
    /** Local-only computation - the active set for [todayEpochDay] is fully deterministic (see
     * [com.suman.memoryarchitect.domain.progression.MissionCatalog]), so this never needs a
     * network round-trip to know *which* missions are active, only [claimMissionReward] does. */
    suspend fun getActiveMissions(todayEpochDay: Long): List<ActiveMission>

    /** Local-only, same "recognition, not an anti-cheat concern" trust level
     * [ProgressionRepository.getStatistics] already documents - advances every currently-active
     * mission whose [com.suman.memoryarchitect.domain.model.MissionRequirementType] matches
     * [event], across all three periods at once. A no-op for a mission already claimed this
     * period, or one whose target is already reached. */
    suspend fun recordMissionEvent(event: MissionEvent, todayEpochDay: Long)

    /** Server-authoritative, same reasoning as [ProgressionRepository.claimDailyReward] - needs a
     * source of truth to prevent double-claiming across devices/reinstalls. A claim that fails for
     * a reason other than "already claimed"/"not eligible" (a real network failure) is queued -
     * see [retryPendingClaims]. */
    suspend fun claimMissionReward(missionId: MissionId, todayEpochDay: Long): Outcome<MissionClaimResult>

    /** Flushes every queued claim (oldest first) that [claimMissionReward] couldn't reach the
     * server for - the same "queued but never retried" gap
     * [ProgressionRepository.retryPendingSubmissions] used to have for score submissions, now
     * closed the same way: a WorkManager worker calls this on connectivity regain (see
     * [com.suman.memoryarchitect.core.sync.PendingMissionClaimSyncWorker]). */
    suspend fun retryPendingClaims()

    /** Local-only read (Room cache) - see [MissionRefreshState]'s doc. Populated only as a side
     * effect of a successful [unlockAllMissionsEarly] call; [MissionRefreshState.EMPTY] otherwise. */
    suspend fun getMissionRefreshState(): MissionRefreshState

    /** Automatically granted the moment every currently-active mission in [period]'s set (at
     * [periodKey]) is claimed - see [com.suman.memoryarchitect.feature.missions.MissionsViewModel]'s
     * trigger. Re-verifies eligibility server-side against the same claim records
     * [claimMissionReward] itself writes, never trusting the caller's belief that the set is fully
     * done. A repeat call once already granted is the routine, expected
     * [com.suman.memoryarchitect.data.repository.MissionAlreadyClaimedException] case, same as any
     * other double-claim in this app. */
    suspend fun claimCategoryBonus(period: MissionPeriod, periodKey: Long): Outcome<MissionReward>

    /** Spends 1000 coins to immediately roll fresh Daily+Weekly+Monthly sets, skipping their
     * natural countdown - the UI only ever offers this once every mission in all three periods is
     * already claimed, but eligibility is re-verified server-side the same way, never trusted from
     * the client. [dailyPeriodKey]/[weeklyPeriodKey]/[monthlyPeriodKey] are each period's current
     * effective periodKey (see [com.suman.memoryarchitect.domain.progression.MissionCatalog.effectivePeriodKey]).
     * The fresh sets are guaranteed to differ from the ones just finished - see
     * [com.suman.memoryarchitect.domain.progression.MissionCatalog.nextDifferentPeriodKey]. */
    suspend fun unlockAllMissionsEarly(dailyPeriodKey: Long, weeklyPeriodKey: Long, monthlyPeriodKey: Long): Outcome<MissionRefreshState>
}
