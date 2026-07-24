package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row campaign frontier: the highest level number the player has unlocked. */
@Entity(tableName = "level_campaign_progress")
data class LevelCampaignProgressEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val maxUnlockedLevel: Int,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
