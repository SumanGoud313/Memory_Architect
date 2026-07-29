package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline queue: a score submission that couldn't reach the server. Flushed by
 * [com.suman.memoryarchitect.core.sync.PendingScoreSyncWorker] once connectivity returns - see
 * [com.suman.memoryarchitect.domain.repository.ProgressionRepository.retryPendingSubmissions].
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
    /** The two other fields a real `submitScore` call needs beyond what's above - see
     * [com.suman.memoryarchitect.domain.model.ScoreResult]/[ProgressionRemoteSource.submitScore]'s
     * doc for why nothing else from [com.suman.memoryarchitect.domain.model.ScoreResult] (objectScores/
     * placementScore/timeBonus/comboBonus) needs to be persisted here: none of it is ever sent over
     * the wire or affects the server-authoritative xp/coins calculation, only finalScore/sceneAccuracy/
     * comboCount are. */
    val comboCount: Int = 0,
    val newlyUnlockedAchievementCount: Int = 0,
    /** Whether this round's completion should award XP - `false` only for a repeat clear of an
     * already-completed Classic level (see [com.suman.memoryarchitect.domain.model.LevelCompletionOutcome.isFirstCompletion]).
     * Must be resent exactly as computed when this round first finished, never recomputed at retry
     * time - by the time a queued submission actually retries, the level may have since been
     * "first-completed" by another already-synced round, which would wrongly flip this to `true`
     * for a submission that was never entitled to XP in the first place. Defaults to `true` so
     * every row queued before this field existed keeps awarding XP exactly as it always did. */
    val awardXp: Boolean = true,
)
