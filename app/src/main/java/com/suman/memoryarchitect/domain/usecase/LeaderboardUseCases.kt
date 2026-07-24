package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.GlobalLeaderboardStats
import com.suman.memoryarchitect.domain.model.LeaderboardResult
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PeriodicLeaderboardSubmission
import com.suman.memoryarchitect.domain.repository.LeaderboardRepository
import javax.inject.Inject

/** Thin wrappers over [LeaderboardRepository] - one class per call, same convention as every
 * other use case in this package (see [GetStatisticsUseCase]/[SubmitScoreUseCase]). */

class SubmitGlobalLeaderboardStatsUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(stats: GlobalLeaderboardStats): Outcome<Unit> = repository.submitGlobalStats(stats)
}

class SubmitDailyLeaderboardScoreUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(submission: PeriodicLeaderboardSubmission): Outcome<Unit> = repository.submitDailyScore(submission)
}

class SubmitWeeklyLeaderboardScoreUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(submission: PeriodicLeaderboardSubmission): Outcome<Unit> = repository.submitWeeklyScore(submission)
}

class SubmitMonthlyLeaderboardScoreUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(submission: PeriodicLeaderboardSubmission): Outcome<Unit> = repository.submitMonthlyScore(submission)
}

class GetGlobalLeaderboardUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(): Outcome<LeaderboardResult> = repository.getGlobalLeaderboard()
}

class GetDailyLeaderboardUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(): Outcome<LeaderboardResult> = repository.getDailyLeaderboard()
}

class GetWeeklyLeaderboardUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(): Outcome<LeaderboardResult> = repository.getWeeklyLeaderboard()
}

class GetMonthlyLeaderboardUseCase @Inject constructor(private val repository: LeaderboardRepository) {
    suspend operator fun invoke(): Outcome<LeaderboardResult> = repository.getMonthlyLeaderboard()
}
