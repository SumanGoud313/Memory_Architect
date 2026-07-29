package com.suman.memoryarchitect.data.repository

/** Mirrors [MissionNotEligibleException]'s role - thrown by
 * [ProgressionRemoteSource.claimReturningPlayerGift] when the stored gap between
 * `lastPlayedEpochDay` and the claim day isn't actually long enough, independent of whatever the
 * client's own [com.suman.memoryarchitect.domain.usecase.GetReturningPlayerWelcomeUseCase] check
 * said. */
class ReturningPlayerGiftNotEligibleException(reason: String) : RuntimeException("Returning-player gift claim rejected: $reason")
