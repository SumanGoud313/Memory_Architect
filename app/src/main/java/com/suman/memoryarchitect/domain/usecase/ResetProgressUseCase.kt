package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.LocalProgressResetRepository
import javax.inject.Inject

class ResetProgressUseCase @Inject constructor(
    private val repository: LocalProgressResetRepository,
) {
    suspend operator fun invoke() = repository.resetAll()
}
