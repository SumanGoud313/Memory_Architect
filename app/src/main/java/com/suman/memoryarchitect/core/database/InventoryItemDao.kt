package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface InventoryItemDao {

    @Query("SELECT * FROM inventory_items")
    suspend fun getAll(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE kind = :kind")
    suspend fun get(kind: String): InventoryItemEntity?

    @Upsert
    suspend fun upsert(entity: InventoryItemEntity)

    @Query("DELETE FROM inventory_items")
    suspend fun clearAll()
}
