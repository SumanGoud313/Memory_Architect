package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MissionProgressDao {

    @Query("SELECT * FROM mission_progress")
    suspend fun getAll(): List<MissionProgressEntity>

    @Query("SELECT * FROM mission_progress WHERE missionId = :missionId")
    suspend fun get(missionId: String): MissionProgressEntity?

    @Upsert
    suspend fun upsert(entity: MissionProgressEntity)

    @Query("DELETE FROM mission_progress")
    suspend fun clearAll()
}
