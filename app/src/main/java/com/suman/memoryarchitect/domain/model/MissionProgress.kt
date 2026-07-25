package com.suman.memoryarchitect.domain.model

/**
 * Per-player progress toward one [MissionDefinition] within one rotation window - [periodKey]
 * ties this to a specific occurrence of that window (see
 * [com.suman.memoryarchitect.domain.progression.MissionCatalog.periodKeyFor]), so a new day/week/
 * month's rotation never inherits a stale count from the previous one even if the same
 * [MissionId] happens to roll around again later.
 *
 * Progress itself is tracked purely locally (see
 * [com.suman.memoryarchitect.domain.repository.MissionRepository.recordMissionEvent]) - the same
 * "recognition, not an anti-cheat concern" trust level [PlayerStatistics]/achievements already
 * use. Only [claimMissionReward][com.suman.memoryarchitect.domain.repository.MissionRepository.claimMissionReward]
 * touches the server, and only that call is independently re-verifiable, since the active mission
 * set and its reward are both fully recomputable server-side from [periodKey] alone.
 */
data class MissionProgress(
    val missionId: MissionId,
    val periodKey: Long,
    val currentCount: Int,
    val claimed: Boolean,
)
