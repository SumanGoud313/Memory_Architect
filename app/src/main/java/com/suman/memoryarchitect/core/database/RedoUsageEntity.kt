package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "redo_usage")
data class RedoUsageEntity(
    @PrimaryKey val levelNumber: Int,
    val redosUsed: Int,
)
