package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.repository.MissionRepository
import javax.inject.Inject

class GetMissionRefreshStateUseCase @Inject constructor(
    private val repository: MissionRepository,
) {
    suspend operator fun invoke(): MissionRefreshState = repository.getMissionRefreshState()
}
