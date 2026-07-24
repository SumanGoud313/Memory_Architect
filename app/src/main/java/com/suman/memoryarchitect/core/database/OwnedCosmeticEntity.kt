package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owned_cosmetics")
data class OwnedCosmeticEntity(
    @PrimaryKey val sku: String,
    val acquiredAtEpochDay: Long,
    val acquiredVia: String,
    val lastSyncedAt: Long,
    /** Player-set, syncs to Firestore (`playerCosmetics.favoriteSkus`) - see
     * [ShopRepositoryImpl.toggleFavorite]. Defaulted so every existing call site that constructs
     * this entity without naming every param keeps compiling unchanged. */
    val isFavorite: Boolean = false,
    /** Local-only browsing convenience, never synced - updated in [ShopRepositoryImpl.equip]
     * alongside the existing equip-store calls. `null` until this sku has ever been equipped. */
    val lastEquippedAtEpochMs: Long? = null,
)
