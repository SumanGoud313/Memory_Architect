package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.RedoUsageDao
import com.suman.memoryarchitect.core.database.RedoUsageEntity
import com.suman.memoryarchitect.domain.repository.RedoRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedoRepositoryImpl @Inject constructor(
    private val redoUsageDao: RedoUsageDao,
    private val dispatchers: DispatcherProvider,
) : RedoRepository {

    override suspend fun getRedosUsed(levelNumber: Int): Int = withContext(dispatchers.io) {
        redoUsageDao.get(levelNumber)?.redosUsed ?: 0
    }

    override suspend fun getRewardedRedosUsed(levelNumber: Int): Int = withContext(dispatchers.io) {
        redoUsageDao.get(levelNumber)?.rewardedRedosUsed ?: 0
    }

    override suspend fun recordRedoUsed(levelNumber: Int) = withContext(dispatchers.io) {
        val existing = redoUsageDao.get(levelNumber)
        redoUsageDao.upsert(
            RedoUsageEntity(
                levelNumber = levelNumber,
                redosUsed = (existing?.redosUsed ?: 0) + 1,
                rewardedRedosUsed = existing?.rewardedRedosUsed ?: 0,
            ),
        )
    }

    override suspend fun grantBonusRedo(levelNumber: Int) = withContext(dispatchers.io) {
        val existing = redoUsageDao.get(levelNumber)
        redoUsageDao.upsert(
            RedoUsageEntity(
                levelNumber = levelNumber,
                redosUsed = ((existing?.redosUsed ?: 0) - 1).coerceAtLeast(0),
                rewardedRedosUsed = (existing?.rewardedRedosUsed ?: 0) + 1,
            ),
        )
    }

    override suspend fun resetRedoUsage(levelNumber: Int) = withContext(dispatchers.io) {
        redoUsageDao.clearForLevel(levelNumber)
    }
}
