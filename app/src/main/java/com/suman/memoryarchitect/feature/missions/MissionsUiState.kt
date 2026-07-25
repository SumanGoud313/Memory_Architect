package com.suman.memoryarchitect.feature.missions

import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.MissionId

data class MissionsUiState(
    val isLoading: Boolean = true,
    val missions: List<ActiveMission> = emptyList(),
    /** Non-null only while a claim for this exact mission is in flight - guards against a
     * double-tap the same way [com.suman.memoryarchitect.feature.profile.ProfileViewModel.claimDailyReward]'s
     * `isClaimingDailyReward` does. */
    val claimingMissionId: MissionId? = null,
)
