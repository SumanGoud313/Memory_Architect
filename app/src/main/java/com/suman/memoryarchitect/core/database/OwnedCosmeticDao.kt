package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OwnedCosmeticDao {

    @Query("SELECT * FROM owned_cosmetics")
    suspend fun getAll(): List<OwnedCosmeticEntity>

    @Upsert
    suspend fun upsert(entity: OwnedCosmeticEntity)

    @Query("DELETE FROM owned_cosmetics")
    suspend fun clearAll()

    @Query("UPDATE owned_cosmetics SET isFavorite = :isFavorite WHERE sku = :sku")
    suspend fun setFavorite(sku: String, isFavorite: Boolean)

    @Query("SELECT * FROM owned_cosmetics WHERE isFavorite = 1")
    suspend fun getFavorites(): List<OwnedCosmeticEntity>

    @Query("UPDATE owned_cosmetics SET lastEquippedAtEpochMs = :epochMs WHERE sku = :sku")
    suspend fun touchLastEquipped(sku: String, epochMs: Long)

    @Query("SELECT * FROM owned_cosmetics WHERE lastEquippedAtEpochMs IS NOT NULL ORDER BY lastEquippedAtEpochMs DESC LIMIT :limit")
    suspend fun getRecentlyUsed(limit: Int = 10): List<OwnedCosmeticEntity>
}
