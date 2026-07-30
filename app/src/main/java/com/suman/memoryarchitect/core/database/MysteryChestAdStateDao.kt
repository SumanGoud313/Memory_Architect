package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MysteryChestAdStateDao {

    @Query("SELECT * FROM mystery_chest_ad_state WHERE id = ${MysteryChestAdStateEntity.SINGLETON_ID}")
    suspend fun get(): MysteryChestAdStateEntity?

    @Upsert
    suspend fun upsert(entity: MysteryChestAdStateEntity)
}
