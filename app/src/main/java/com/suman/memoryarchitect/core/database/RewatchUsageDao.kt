package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RewatchUsageDao {

    @Query("SELECT * FROM rewatch_usage WHERE levelNumber = :levelNumber")
    suspend fun get(levelNumber: Int): RewatchUsageEntity?

    @Upsert
    suspend fun upsert(entity: RewatchUsageEntity)

    @Query("DELETE FROM rewatch_usage")
    suspend fun clearAll()

    @Query("DELETE FROM rewatch_usage WHERE levelNumber = :levelNumber")
    suspend fun clearForLevel(levelNumber: Int)
}
