package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RemoteConfigDao {

    @Query("SELECT * FROM remote_config_cache WHERE configKey = :key")
    suspend fun getByKey(key: String): RemoteConfigCacheEntity?

    @Query("SELECT * FROM remote_config_cache")
    suspend fun getAll(): List<RemoteConfigCacheEntity>

    @Upsert
    suspend fun upsert(entities: List<RemoteConfigCacheEntity>)

    @Query("DELETE FROM remote_config_cache WHERE fetchedAt < :olderThan")
    suspend fun deleteFetchedBefore(olderThan: Long)
}