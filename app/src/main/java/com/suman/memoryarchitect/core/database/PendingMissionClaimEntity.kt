package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline queue: a mission claim that couldn't reach the server - the same "no queue, player must
 * re-tap manually" gap [PendingScoreSubmissionEntity] used to have for score submissions. Flushed
 * by [com.suman.memoryarchitect.core.sync.PendingMissionClaimSyncWorker] once connectivity returns
 * - see [com.suman.memoryarchitect.domain.repository.MissionRepository.retryPendingClaims].
 */
@Entity(tableName = "pending_mission_claims")
data class PendingMissionClaimEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    /** [com.suman.memoryarchitect.domain.model.MissionId.name] - stored as a string for the same
     * "survives a future enum rename/removal without crashing the DAO" reason every other
     * String-backed enum column in this schema already does. */
    val missionId: String,
    val periodKey: Long,
    /** The progress count at the moment the claim was attempted - resent verbatim on retry, never
     * recomputed, so a retry replays exactly the claim the player actually triggered even if their
     * local progress has since moved on. */
    val progressCount: Int,
    val createdAt: Long,
)
