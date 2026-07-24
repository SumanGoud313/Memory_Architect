package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per [com.suman.memoryarchitect.domain.model.CosmeticCategory] slot that currently has
 * something equipped - a category with no row simply has nothing equipped. */
@Entity(tableName = "equipped_cosmetics")
data class EquippedCosmeticEntity(
    @PrimaryKey val category: String,
    val sku: String,
    val equippedAtEpochDay: Long,
)
