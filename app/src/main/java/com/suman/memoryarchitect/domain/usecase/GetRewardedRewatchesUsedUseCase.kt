package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.RewatchRepository
import javax.inject.Inject

class GetRewardedRewatchesUsedUseCase @Inject constructor(
    private val repository: RewatchRepository,
) {
    suspend operator fun invoke(levelNumber: Int): Int = repository.getRewardedRewatchesUsed(levelNumber)
}
