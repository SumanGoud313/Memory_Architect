package com.suman.memoryarchitect.data.repository

/** Thrown from inside a Firestore transaction (see [FirestoreProgressionRemoteSource.claimDailyReward])
 * to abort it when the day's reward was already claimed - the same routine, expected condition
 * `mock-backend/index.js`'s `/v1/rewards/daily/claim` reports as an HTTP 409 for
 * [MockBackendProgressionRemoteSource]. [ProgressionRepositoryImpl] catches this specifically and
 * maps it to the same [com.suman.memoryarchitect.domain.model.AppError.Server] shape that 409
 * already produces, rather than letting it fall through to [ErrorMapper]'s generic
 * "unexpected failure, report to Crashlytics" branch - a double-claim race is normal, not a bug. */
class DailyRewardAlreadyClaimedException : RuntimeException("Daily reward already claimed for this day")
