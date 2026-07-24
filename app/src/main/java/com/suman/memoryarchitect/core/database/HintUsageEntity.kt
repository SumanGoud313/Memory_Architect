package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hint_usage")
data class HintUsageEntity(
    @PrimaryKey val levelNumber: Int,
    val hintsUsed: Int,
)
