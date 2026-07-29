package com.suman.memoryarchitect.data.repository

/** Mirrors [DailyRewardAlreadyClaimedException]'s role - the double-claim guard for
 * [ProgressionRemoteSource.claimReturningPlayerGift], a routine/expected condition (a retried
 * request, a second device), never reported to Crashlytics. */
class ReturningPlayerGiftAlreadyClaimedException : RuntimeException("Returning-player gift already claimed today")
