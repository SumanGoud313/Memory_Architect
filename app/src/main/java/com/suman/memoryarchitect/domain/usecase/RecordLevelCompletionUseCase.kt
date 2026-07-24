package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import javax.inject.Inject

class RecordLevelCompletionUseCase @Inject constructor(
    private val repository: LevelCampaignRepository,
) {
    suspend operator fun invoke(levelNumber: Int, timeTakenMs: Long, passed: Boolean, stars: Int): LevelCompletionOutcome =
        repository.recordCompletion(levelNumber, timeTakenMs, passed, stars)
}
