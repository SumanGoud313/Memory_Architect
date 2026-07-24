package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface HintUsageDao {

    @Query("SELECT * FROM hint_usage WHERE levelNumber = :levelNumber")
    suspend fun get(levelNumber: Int): HintUsageEntity?

    @Upsert
    suspend fun upsert(entity: HintUsageEntity)

    @Query("DELETE FROM hint_usage")
    suspend fun clearAll()

    @Query("DELETE FROM hint_usage WHERE levelNumber = :levelNumber")
    suspend fun clearForLevel(levelNumber: Int)
}
