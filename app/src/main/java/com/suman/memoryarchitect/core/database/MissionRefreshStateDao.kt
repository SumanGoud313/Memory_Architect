package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MissionRefreshStateDao {

    @Query("SELECT * FROM mission_refresh_state WHERE id = ${MissionRefreshStateEntity.SINGLETON_ID}")
    suspend fun get(): MissionRefreshStateEntity?

    @Upsert
    suspend fun upsert(entity: MissionRefreshStateEntity)
}
