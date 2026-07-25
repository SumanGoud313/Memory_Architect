package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.repository.MissionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class RecordMissionEventUseCase @Inject constructor(
    private val repository: MissionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(event: MissionEvent) = repository.recordMissionEvent(event, LocalDate.now(clock).toEpochDay())
}
