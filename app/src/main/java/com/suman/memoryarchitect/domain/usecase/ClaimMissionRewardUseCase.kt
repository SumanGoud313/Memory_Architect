package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.MissionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ClaimMissionRewardUseCase @Inject constructor(
    private val repository: MissionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(missionId: MissionId): Outcome<MissionClaimResult> =
        repository.claimMissionReward(missionId, LocalDate.now(clock).toEpochDay())
}
