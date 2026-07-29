package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.MissionRepository
import javax.inject.Inject

/** [periodKey] is the exact periodKey the caller's already-loaded [com.suman.memoryarchitect.domain.model.ActiveMission]s
 * for [period] carry - no [java.time.Clock]/`todayEpochDay` re-derivation needed here, since a
 * category bonus is only ever claimed against a period the caller has already resolved via
 * [GetActiveMissionsUseCase]. */
class ClaimMissionCategoryBonusUseCase @Inject constructor(
    private val repository: MissionRepository,
) {
    suspend operator fun invoke(period: MissionPeriod, periodKey: Long): Outcome<MissionReward> =
        repository.claimCategoryBonus(period, periodKey)
}
