package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingScoreSubmissionDao {

    @Insert
    suspend fun insert(entity: PendingScoreSubmissionEntity): Long

    @Query("SELECT * FROM pending_score_submissions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingScoreSubmissionEntity>

    @Delete
    suspend fun delete(entity: PendingScoreSubmissionEntity)

    @Query("DELETE FROM pending_score_submissions")
    suspend fun clearAll()
}
