package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class GetUnlockedAchievementsUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(): Set<AchievementId> = repository.getUnlockedAchievementIds()
}
