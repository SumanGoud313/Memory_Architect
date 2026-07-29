package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.ReturningPlayerGiftClaimResult
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ClaimReturningPlayerGiftUseCase @Inject constructor(
    private val repository: ProgressionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Outcome<ReturningPlayerGiftClaimResult> =
        repository.claimReturningPlayerGift(LocalDate.now(clock).toEpochDay())
}
