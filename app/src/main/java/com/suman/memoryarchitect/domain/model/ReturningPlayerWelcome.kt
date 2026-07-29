package com.suman.memoryarchitect.domain.model

/** What [com.suman.memoryarchitect.domain.usecase.GetReturningPlayerWelcomeUseCase] resolves for
 * the current Home visit - a pure function of the already-cached [PlayerProfile], no network call
 * of its own. [tier] drives the banner's tone/copy; [canClaimGift] is true only for
 * [ReturningPlayerTier.MEDIUM]/[ReturningPlayerTier.LONG] and the player hasn't already claimed
 * today's gift (see [com.suman.memoryarchitect.domain.repository.ProgressionRepository.claimReturningPlayerGift]).
 * [journeyTierId] is populated only for [ReturningPlayerTier.LONG], to reassure a long-absent
 * player exactly how much Memory Journey progress is still banked. */
data class ReturningPlayerWelcome(
    val tier: ReturningPlayerTier,
    val gapDays: Long,
    val canClaimGift: Boolean,
    val journeyTierId: MemoryJourneyTierId? = null,
)
