package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EquippedCosmeticDao {

    @Query("SELECT * FROM equipped_cosmetics")
    suspend fun getAll(): List<EquippedCosmeticEntity>

    @Upsert
    suspend fun upsert(entity: EquippedCosmeticEntity)

    @Query("DELETE FROM equipped_cosmetics WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM equipped_cosmetics")
    suspend fun clearAll()
}
