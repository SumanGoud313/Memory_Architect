package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class ResetWinStreakUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke() = repository.resetWinStreak()
}
