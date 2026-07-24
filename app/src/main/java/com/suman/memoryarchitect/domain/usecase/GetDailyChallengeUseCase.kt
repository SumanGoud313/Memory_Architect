package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.ChallengePreview
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ChallengeRepository
import javax.inject.Inject

class GetDailyChallengeUseCase @Inject constructor(
    private val repository: ChallengeRepository,
) {
    suspend operator fun invoke(): Outcome<ChallengePreview> = repository.getDailyChallenge()
}
