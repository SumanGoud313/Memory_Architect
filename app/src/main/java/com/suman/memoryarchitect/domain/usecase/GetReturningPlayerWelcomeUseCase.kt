package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.ReturningPlayerTier
import com.suman.memoryarchitect.domain.model.ReturningPlayerWelcome
import com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog
import com.suman.memoryarchitect.domain.progression.ReturningPlayerRules
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Purely local - a function of the already-cached [com.suman.memoryarchitect.domain.model.PlayerProfile.lastPlayedEpochDay],
 * no network call of its own (unlike [ClaimReturningPlayerGiftUseCase], which is server-
 * authoritative). Returns `null` whenever there's nothing to say: never played yet (a new player,
 * not a *returning* one), or the gap is too short to be [ReturningPlayerTier.NONE].
 */
class GetReturningPlayerWelcomeUseCase @Inject constructor(
    private val repository: ProgressionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): ReturningPlayerWelcome? {
        val profile = (repository.getProfile() as? Outcome.Success)?.data ?: return null
        val lastPlayedEpochDay = profile.lastPlayedEpochDay ?: return null
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        val gapDays = todayEpochDay - lastPlayedEpochDay
        val tier = ReturningPlayerRules.Default.tierFor(gapDays)
        if (tier == ReturningPlayerTier.NONE) return null

        val journeyTierId = if (tier == ReturningPlayerTier.LONG) MemoryJourneyCatalog.tierFor(profile.journeyPoints)?.id else null
        val canClaimGift = tier == ReturningPlayerTier.MEDIUM || tier == ReturningPlayerTier.LONG
        return ReturningPlayerWelcome(tier, gapDays, canClaimGift, journeyTierId)
    }
}
