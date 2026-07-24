package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class GetUnlockedRewardsUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(): Set<RewardId> = repository.getUnlockedRewardIds()
}