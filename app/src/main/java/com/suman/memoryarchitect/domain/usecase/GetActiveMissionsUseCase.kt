package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.repository.MissionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class GetActiveMissionsUseCase @Inject constructor(
    private val repository: MissionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): List<ActiveMission> = repository.getActiveMissions(LocalDate.now(clock).toEpochDay())
}
