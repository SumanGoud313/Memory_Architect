package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class GetPlayerProfileUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(): Outcome<PlayerProfile> = repository.getProfile()
}
