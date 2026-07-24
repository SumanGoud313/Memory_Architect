package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UnlockedAchievementDao {

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun getAll(): List<UnlockedAchievementEntity>

    @Upsert
    suspend fun upsert(entity: UnlockedAchievementEntity)

    @Query("DELETE FROM unlocked_achievements")
    suspend fun clearAll()
}
