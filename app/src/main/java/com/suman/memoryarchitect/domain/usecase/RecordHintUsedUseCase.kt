package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.HintRepository
import javax.inject.Inject

class RecordHintUsedUseCase @Inject constructor(
    private val repository: HintRepository,
) {
    suspend operator fun invoke(levelNumber: Int) = repository.recordHintUsed(levelNumber)
}
