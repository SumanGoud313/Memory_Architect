package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.HintUsageDao
import com.suman.memoryarchitect.core.database.HintUsageEntity
import com.suman.memoryarchitect.domain.repository.HintRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HintRepositoryImpl @Inject constructor(
    private val hintUsageDao: HintUsageDao,
    private val dispatchers: DispatcherProvider,
) : HintRepository {

    override suspend fun getHintsUsed(levelNumber: Int): Int = withContext(dispatchers.io) {
        hintUsageDao.get(levelNumber)?.hintsUsed ?: 0
    }

    override suspend fun recordHintUsed(levelNumber: Int) = withContext(dispatchers.io) {
        val current = hintUsageDao.get(levelNumber)?.hintsUsed ?: 0
        hintUsageDao.upsert(HintUsageEntity(levelNumber = levelNumber, hintsUsed = current + 1))
    }

    override suspend fun grantBonusHint(levelNumber: Int) = withContext(dispatchers.io) {
        val current = hintUsageDao.get(levelNumber)?.hintsUsed ?: 0
        hintUsageDao.upsert(HintUsageEntity(levelNumber = levelNumber, hintsUsed = (current - 1).coerceAtLeast(0)))
    }

    override suspend fun resetHintUsage(levelNumber: Int) = withContext(dispatchers.io) {
        hintUsageDao.clearForLevel(levelNumber)
    }
}
