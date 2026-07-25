package com.suman.memoryarchitect.data.repository

/** Thrown from inside a Firestore transaction (see [FirestoreMissionRemoteSource.claimMissionReward])
 * to abort it when this mission's reward for this [com.suman.memoryarchitect.domain.model.MissionProgress.periodKey]
 * was already claimed - the same routine, expected double-claim condition
 * [DailyRewardAlreadyClaimedException] already models for the check-in cycle. */
class MissionAlreadyClaimedException : RuntimeException("Mission reward already claimed for this period")
