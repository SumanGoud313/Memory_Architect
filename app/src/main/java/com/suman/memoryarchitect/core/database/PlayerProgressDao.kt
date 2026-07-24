package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlayerProgressDao {

    @Query("SELECT * FROM player_progress_cache WHERE id = ${PlayerProgressCacheEntity.SINGLETON_ID}")
    suspend fun get(): PlayerProgressCacheEntity?

    @Upsert
    suspend fun upsert(entity: PlayerProgressCacheEntity)

    @Query("DELETE FROM player_progress_cache")
    suspend fun clearAll()
}
