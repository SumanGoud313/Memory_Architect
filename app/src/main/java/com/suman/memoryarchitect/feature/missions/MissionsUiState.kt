package com.suman.memoryarchitect.feature.missions

import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.LiveEvent
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod

data class MissionsUiState(
    val isLoading: Boolean = true,
    val missions: List<ActiveMission> = emptyList(),
    /** Non-null only while a claim for this exact mission is in flight - guards against a
     * double-tap the same way [com.suman.memoryarchitect.feature.profile.ProfileViewModel.claimDailyReward]'s
     * `isClaimingDailyReward` does. */
    val claimingMissionId: MissionId? = null,
    /** Surfaced when a claim attempt fails - see [com.suman.memoryarchitect.feature.profile.ProfileUiState.Content.dailyRewardError]'s
     * doc for why this can't just reset silently: without it, a real failure (already claimed
     * elsewhere, a network error queued for retry) looks identical to the tap doing nothing.
     * Cleared the moment a new claim attempt starts. */
    val claimError: AppError? = null,
    /** Drives the seasonal event banner - see [com.suman.memoryarchitect.domain.usecase.GetActiveLiveEventUseCase].
     * `null` whenever no event is live, which is the common case outside a scheduled window. */
    val activeEvent: LiveEvent? = null,
    /** Epoch-second [com.suman.memoryarchitect.domain.progression.MissionCatalog.nextPeriodStartEpochSecond]
     * computed for each rotating period - what [com.suman.memoryarchitect.ui.screens.missions.MissionCountdown]
     * ticks down to. Absent for [MissionPeriod.EVENT] (no rotation boundary - see that function's doc). */
    val nextRotationEpochSecondByPeriod: Map<MissionPeriod, Long> = emptyMap(),
    /** True only while [MissionsViewModel.unlockAllMissionsNow] is in flight - guards against a
     * double-tap the same way [claimingMissionId] does for an individual claim. */
    val isUnlockingAll: Boolean = false,
    /** Surfaced when [MissionsViewModel.unlockAllMissionsNow] fails - same "don't fail silently"
     * reasoning [claimError] already documents. */
    val unlockAllError: AppError? = null,
) {
    /** True once every currently-active mission in [period]'s set is claimed - drives both the
     * per-period "bonus claimed" banner and (once true for all three of Daily/Weekly/Monthly at
     * once) [canUnlockAllNow]. Never true for an empty list (a period with no active missions
     * yet, e.g. before the first [MissionsViewModel.refresh] completes, isn't "complete"). */
    fun isCategoryComplete(period: MissionPeriod): Boolean {
        val periodMissions = missions.filter { it.definition.period == period }
        return periodMissions.isNotEmpty() && periodMissions.all { it.claimed }
    }

    /** Gates the "Unlock New Missions Now - 1,000 Coins" affordance - only ever visible once every
     * mission in all three rotating periods is already claimed, per the request this was built
     * for ("this option should be only available... if user completes all the daily, weekly,
     * monthly missions"). */
    val canUnlockAllNow: Boolean
        get() = isCategoryComplete(MissionPeriod.DAILY) && isCategoryComplete(MissionPeriod.WEEKLY) && isCategoryComplete(MissionPeriod.MONTHLY)
}
