package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MemoryJourneyStanding
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

/** Thin wrapper - Memory Journey introduces no repository/collection of its own (see
 * [MemoryJourneyCatalog]'s doc), so this just reads the already-fetched profile and interprets its
 * [com.suman.memoryarchitect.domain.model.PlayerProfile.journeyPoints] through the tier table. */
class GetMemoryJourneyStandingUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(): Outcome<MemoryJourneyStanding> = when (val outcome = repository.getProfile()) {
        is Outcome.Success -> Outcome.Success(MemoryJourneyCatalog.standingFor(outcome.data.journeyPoints))
        is Outcome.Error -> outcome
    }
}
