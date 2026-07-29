package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewatch_usage")
data class RewatchUsageEntity(
    @PrimaryKey val levelNumber: Int,
    val rewatchesUsed: Int,
    val rewardedRewatchesUsed: Int = 0,
)
