package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_best_times")
data class LevelBestTimeEntity(
    @PrimaryKey val levelNumber: Int,
    val bestTimeMs: Long,
    val bestStars: Int = 0,
)
