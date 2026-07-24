package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StatisticsDao {

    @Query("SELECT * FROM statistics_cache WHERE id = ${StatisticsCacheEntity.SINGLETON_ID}")
    suspend fun get(): StatisticsCacheEntity?

    @Upsert
    suspend fun upsert(entity: StatisticsCacheEntity)

    @Query("DELETE FROM statistics_cache")
    suspend fun clearAll()
}
