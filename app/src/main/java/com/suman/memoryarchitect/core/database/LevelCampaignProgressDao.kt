package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LevelCampaignProgressDao {

    @Query("SELECT * FROM level_campaign_progress WHERE id = :id")
    suspend fun get(id: Int = LevelCampaignProgressEntity.SINGLETON_ID): LevelCampaignProgressEntity?

    @Upsert
    suspend fun upsert(entity: LevelCampaignProgressEntity)

    @Query("DELETE FROM level_campaign_progress")
    suspend fun clearAll()
}
