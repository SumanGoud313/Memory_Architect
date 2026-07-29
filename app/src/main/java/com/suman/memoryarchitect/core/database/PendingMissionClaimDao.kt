package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingMissionClaimDao {

    @Insert
    suspend fun insert(entity: PendingMissionClaimEntity): Long

    @Query("SELECT * FROM pending_mission_claims ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingMissionClaimEntity>

    @Delete
    suspend fun delete(entity: PendingMissionClaimEntity)

    @Query("DELETE FROM pending_mission_claims")
    suspend fun clearAll()
}
