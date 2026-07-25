package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local cache of one [com.suman.memoryarchitect.domain.model.MissionId]'s progress - keyed by
 * the mission's own name since only one [periodKey] can ever be "current" for a given mission at
 * once (see [com.suman.memoryarchitect.domain.model.MissionProgress]'s doc). A stored row whose
 * [periodKey] no longer matches [com.suman.memoryarchitect.domain.progression.MissionCatalog.periodKeyFor]'s
 * current value is stale - the repository treats it as fresh (0, unclaimed) rather than reading
 * it, the same "don't inherit a previous cycle's count" guarantee [MissionProgress] documents. */
@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey val missionId: String,
    val periodKey: Long,
    val currentCount: Int,
    val claimed: Boolean,
)
