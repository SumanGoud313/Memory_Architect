package com.suman.memoryarchitect.data.repository

/** Thrown when a score/leaderboard submission's [submissionNonce][FirestoreProgressionRemoteSource.submitScore]
 * has already been recorded — the write is a replay (a retried network call after the first attempt
 * actually landed, or a forged resubmission) rather than a new round. Same "expected condition, not
 * a bug" shape as [DailyRewardAlreadyClaimedException] — callers map this to a routine error, not a
 * Crashlytics report. */
class DuplicateSubmissionException : RuntimeException("Submission already processed")
