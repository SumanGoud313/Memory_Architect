package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.domain.generation.PeriodicChallengeGenerator
import com.suman.memoryarchitect.domain.model.ChallengePreview
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ChallengeRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preview-only reads of the shared daily/weekly puzzle (same seed for every player - see
 * [PeriodicChallengeGenerator]) for Home's challenge cards. Fully on-device and always succeeds -
 * unlike the old network-backed version, there's no failure mode to report, since generating a
 * preview is the same pure, offline computation [LevelRepositoryImpl] uses for the actually-played
 * level.
 */
@Singleton
class ChallengeRepositoryImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : ChallengeRepository {

    override suspend fun getDailyChallenge(): Outcome<ChallengePreview> = withContext(dispatchers.io) {
        val level = PeriodicChallengeGenerator.generateDailyChallenge()
        Outcome.Success(ChallengePreview(PeriodicChallengeGenerator.dailyExpiresAtEpochSecond(), level.sceneType))
    }

    override suspend fun getWeeklyChallenge(): Outcome<ChallengePreview> = withContext(dispatchers.io) {
        val level = PeriodicChallengeGenerator.generateWeeklyChallenge()
        Outcome.Success(ChallengePreview(PeriodicChallengeGenerator.weeklyExpiresAtEpochSecond(), level.sceneType))
    }
}
