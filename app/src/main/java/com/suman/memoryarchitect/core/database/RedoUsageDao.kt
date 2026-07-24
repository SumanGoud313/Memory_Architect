package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RedoUsageDao {

    @Query("SELECT * FROM redo_usage WHERE levelNumber = :levelNumber")
    suspend fun get(levelNumber: Int): RedoUsageEntity?

    @Upsert
    suspend fun upsert(entity: RedoUsageEntity)

    @Query("DELETE FROM redo_usage")
    suspend fun clearAll()

    @Query("DELETE FROM redo_usage WHERE levelNumber = :levelNumber")
    suspend fun clearForLevel(levelNumber: Int)
}
