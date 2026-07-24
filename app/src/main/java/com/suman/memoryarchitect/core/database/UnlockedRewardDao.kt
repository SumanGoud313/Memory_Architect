package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UnlockedRewardDao {

    @Query("SELECT * FROM unlocked_rewards")
    suspend fun getAll(): List<UnlockedRewardEntity>

    @Upsert
    suspend fun upsert(entity: UnlockedRewardEntity)

    @Query("DELETE FROM unlocked_rewards")
    suspend fun clearAll()
}