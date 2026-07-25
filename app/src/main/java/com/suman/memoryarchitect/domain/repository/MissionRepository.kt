package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.MissionId
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
     * source of truth to prevent double-claiming across devices/reinstalls. */
    suspend fun claimMissionReward(missionId: MissionId, todayEpochDay: Long): Outcome<MissionClaimResult>
}
