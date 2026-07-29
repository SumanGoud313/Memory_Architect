package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.RewatchUsageDao
import com.suman.memoryarchitect.core.database.RewatchUsageEntity
import com.suman.memoryarchitect.domain.repository.RewatchRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewatchRepositoryImpl @Inject constructor(
    private val rewatchUsageDao: RewatchUsageDao,
    private val dispatchers: DispatcherProvider,
) : RewatchRepository {

    override suspend fun getRewatchesUsed(levelNumber: Int): Int = withContext(dispatchers.io) {
        rewatchUsageDao.get(levelNumber)?.rewatchesUsed ?: 0
    }

    override suspend fun getRewardedRewatchesUsed(levelNumber: Int): Int = withContext(dispatchers.io) {
        rewatchUsageDao.get(levelNumber)?.rewardedRewatchesUsed ?: 0
    }

    override suspend fun recordRewatchUsed(levelNumber: Int) = withContext(dispatchers.io) {
        val existing = rewatchUsageDao.get(levelNumber)
        rewatchUsageDao.upsert(
            RewatchUsageEntity(
                levelNumber = levelNumber,
                rewatchesUsed = (existing?.rewatchesUsed ?: 0) + 1,
                rewardedRewatchesUsed = existing?.rewardedRewatchesUsed ?: 0,
            ),
        )
    }

    override suspend fun recordRewardedRewatchUsed(levelNumber: Int) = withContext(dispatchers.io) {
        val existing = rewatchUsageDao.get(levelNumber)
        rewatchUsageDao.upsert(
            RewatchUsageEntity(
                levelNumber = levelNumber,
                rewatchesUsed = existing?.rewatchesUsed ?: 0,
                rewardedRewatchesUsed = (existing?.rewardedRewatchesUsed ?: 0) + 1,
            ),
        )
    }

    override suspend fun resetRewatchUsage(levelNumber: Int) = withContext(dispatchers.io) {
        rewatchUsageDao.clearForLevel(levelNumber)
    }
}
