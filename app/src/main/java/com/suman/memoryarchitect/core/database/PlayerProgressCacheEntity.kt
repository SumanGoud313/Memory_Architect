package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row mirror of the server profile. The server remains the source of truth. */
@Entity(tableName = "player_progress_cache")
data class PlayerProgressCacheEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val xp: Long,
    val coins: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastPlayedEpochDay: Long?,
    val streakShields: Int = 0,
    val dailyChallengeWonAtEpochSecond: Long? = null,
    val weeklyChallengeWonAtEpochSecond: Long? = null,
    val lastSyncedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
