package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row local cache mirror of [com.suman.memoryarchitect.domain.model.MissionRefreshState] -
 * the server remains the source of truth, same "cache, not source of truth" role
 * [PlayerProgressCacheEntity] plays for [com.suman.memoryarchitect.domain.model.PlayerProfile]. */
@Entity(tableName = "mission_refresh_state")
data class MissionRefreshStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val dailyForcedPeriodKey: Long?,
    val weeklyForcedPeriodKey: Long?,
    val monthlyForcedPeriodKey: Long?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
