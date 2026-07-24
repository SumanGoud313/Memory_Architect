package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline queue: a score submission that couldn't reach the server. A future sync worker
 * (WorkManager) is the natural place to flush these once connectivity returns — this table
 * is that seam, not itself the retry mechanism.
 */
@Entity(tableName = "pending_score_submissions")
data class PendingScoreSubmissionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val mode: String,
    val levelSeed: Long,
    val finalScore: Int,
    val sceneAccuracy: Float,
    val playedOnEpochDay: Long,
    val createdAt: Long,
    val retryCount: Int = 0,
    /** Minted once when this round first finished - a future sync worker must resend this exact
     * value, never a fresh one, so a retry of a submission that actually landed server-side (but
     * whose success response never reached the client) is rejected as a replay instead of granting
     * XP/coins a second time. See FirestoreProgressionRemoteSource.submitScore. */
    val submissionNonce: String = "",
)
