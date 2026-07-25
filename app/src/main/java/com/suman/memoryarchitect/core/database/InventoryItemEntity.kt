package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local cache mirror of one [com.suman.memoryarchitect.domain.model.InventoryItemKind]'s
 * quantity - server-authoritative, same "cache, not source of truth" role
 * [PlayerProgressCacheEntity] plays for [com.suman.memoryarchitect.domain.model.PlayerProfile]. */
@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val kind: String,
    val quantity: Int,
)
