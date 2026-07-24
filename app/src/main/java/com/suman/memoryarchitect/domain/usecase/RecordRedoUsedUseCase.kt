package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.RedoRepository
import javax.inject.Inject

class RecordRedoUsedUseCase @Inject constructor(
    private val repository: RedoRepository,
) {
    suspend operator fun invoke(levelNumber: Int) = repository.recordRedoUsed(levelNumber)
}
