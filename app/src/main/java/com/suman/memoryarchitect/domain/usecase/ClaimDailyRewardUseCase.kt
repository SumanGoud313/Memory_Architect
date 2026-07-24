package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ClaimDailyRewardUseCase @Inject constructor(
    private val repository: ProgressionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Outcome<DailyRewardClaimResult> =
        repository.claimDailyReward(LocalDate.now(clock).toEpochDay())
}
