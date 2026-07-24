package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(): PlayerStatistics = repository.getStatistics()
}
