package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LevelBestTimeDao {

    @Query("SELECT * FROM level_best_times WHERE levelNumber = :levelNumber")
    suspend fun get(levelNumber: Int): LevelBestTimeEntity?

    @Query("SELECT * FROM level_best_times")
    suspend fun getAll(): List<LevelBestTimeEntity>

    @Upsert
    suspend fun upsert(entity: LevelBestTimeEntity)

    @Query("DELETE FROM level_best_times")
    suspend fun clearAll()
}
