package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.MissionRepository
import javax.inject.Inject

/** [dailyPeriodKey]/[weeklyPeriodKey]/[monthlyPeriodKey] are each period's exact periodKey the
 * caller's already-loaded [com.suman.memoryarchitect.domain.model.ActiveMission]s carry - same
 * "no separate re-derivation" reasoning [ClaimMissionCategoryBonusUseCase]'s doc gives. */
class UnlockAllMissionsEarlyUseCase @Inject constructor(
    private val repository: MissionRepository,
) {
    suspend operator fun invoke(dailyPeriodKey: Long, weeklyPeriodKey: Long, monthlyPeriodKey: Long): Outcome<MissionRefreshState> =
        repository.unlockAllMissionsEarly(dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey)
}
