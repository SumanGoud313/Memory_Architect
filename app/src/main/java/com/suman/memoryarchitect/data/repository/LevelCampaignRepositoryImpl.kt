package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.LevelBestTimeDao
import com.suman.memoryarchitect.core.database.LevelBestTimeEntity
import com.suman.memoryarchitect.core.database.LevelCampaignProgressDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressEntity
import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.progression.LevelCampaignEngine
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Local-only campaign progress: unlock frontier and per-level best times. Unlike
 * [ProgressionRepositoryImpl] there is no server counterpart to sync against — the mock
 * backend has no per-level-progress endpoints, so this is a straightforward Room read/write.
 */
@Singleton
class LevelCampaignRepositoryImpl @Inject constructor(
    private val progressDao: LevelCampaignProgressDao,
    private val bestTimeDao: LevelBestTimeDao,
    private val dispatchers: DispatcherProvider,
) : LevelCampaignRepository {

    private val campaignEngine = LevelCampaignEngine()

    override suspend fun getMaxUnlockedLevel(): Int = withContext(dispatchers.io) {
        progressDao.get()?.maxUnlockedLevel ?: 1
    }

    override suspend fun getAllBestTimes(): Map<Int, Long> = withContext(dispatchers.io) {
        bestTimeDao.getAll().associate { it.levelNumber to it.bestTimeMs }
    }

    override suspend fun getAllBestStars(): Map<Int, Int> = withContext(dispatchers.io) {
        bestTimeDao.getAll().associate { it.levelNumber to it.bestStars }
    }

    override suspend fun recordCompletion(
        levelNumber: Int,
        timeTakenMs: Long,
        passed: Boolean,
        stars: Int,
    ): LevelCompletionOutcome = withContext(dispatchers.io) {
        val currentMax = progressDao.get()?.maxUnlockedLevel ?: 1
        val existing = bestTimeDao.get(levelNumber)
        val existingBestMs = existing?.bestTimeMs
        val existingBestStars = existing?.bestStars ?: 0
        // Read before this call's own upsert below overwrites it - existingBestStars == 0 means no
        // prior pass was ever recorded for this level, i.e. this is genuinely the first clear.
        val isFirstCompletion = passed && existingBestStars == 0

        var bestMs = existingBestMs
        var bestStars = existingBestStars
        var isNewBest = false
        var isNewBestStars = false
        if (passed) {
            isNewBest = existingBestMs == null || timeTakenMs < existingBestMs
            isNewBestStars = stars > existingBestStars
            bestMs = if (existingBestMs == null) timeTakenMs else min(existingBestMs, timeTakenMs)
            bestStars = max(existingBestStars, stars)
            bestTimeDao.upsert(LevelBestTimeEntity(levelNumber, bestMs, bestStars))
        }

        val nextMax = campaignEngine.nextMaxUnlocked(currentMax, levelNumber, passed)
        if (nextMax != currentMax) {
            progressDao.upsert(LevelCampaignProgressEntity(maxUnlockedLevel = nextMax))
        }

        LevelCompletionOutcome(
            levelNumber = levelNumber,
            passed = passed,
            timeTakenMs = timeTakenMs,
            bestTimeMs = bestMs,
            isNewBest = isNewBest,
            stars = stars,
            bestStars = bestStars,
            isNewBestStars = isNewBestStars,
            nextLevelUnlocked = nextMax > levelNumber,
            isFinalLevel = campaignEngine.isFinalLevel(levelNumber),
            isFirstCompletion = isFirstCompletion,
        )
    }
}
