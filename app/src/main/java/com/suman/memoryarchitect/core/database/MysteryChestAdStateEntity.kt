package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row local cache mirror of
 * [com.suman.memoryarchitect.domain.model.MysteryChestAdState] - same "cache, not source of
 * truth" role [LuckySpinStateEntity] plays for
 * [com.suman.memoryarchitect.domain.model.LuckySpinState], and deliberately its own table for the
 * same reason: this is its own small server-authoritative document, not a field folded into
 * [LuckySpinStateEntity], even though both live on the same [com.suman.memoryarchitect.ui.screens.shop.LuckySpinScreen]. */
@Entity(tableName = "mystery_chest_ad_state")
data class MysteryChestAdStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastClaimEpochDay: Long?,
    val claimsUsedToday: Int,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
