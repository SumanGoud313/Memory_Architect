package com.suman.memoryarchitect.domain.model

/** The full outcome of one [com.suman.memoryarchitect.domain.repository.MissionRepository.claimMissionReward]
 * call - the updated profile (coins/xp) and inventory together, so a caller never has to
 * separately re-fetch either to reflect the grant. */
data class MissionClaimResult(
    val missionId: MissionId,
    val reward: MissionReward,
    val profile: PlayerProfile,
    val inventory: Inventory,
)
