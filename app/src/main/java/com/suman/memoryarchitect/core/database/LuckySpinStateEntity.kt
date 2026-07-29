package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row local cache mirror of [com.suman.memoryarchitect.domain.model.LuckySpinState] - the
 * server remains the source of truth, same "cache, not source of truth" role
 * [PlayerProgressCacheEntity] plays for [com.suman.memoryarchitect.domain.model.PlayerProfile].
 * Its own table (not folded into [PlayerProgressCacheEntity]) since this state is Shop-owned, not
 * Progression-owned - see [com.suman.memoryarchitect.domain.model.LuckySpinState]'s doc. */
@Entity(tableName = "lucky_spin_state")
data class LuckySpinStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastFreeSpinEpochDay: Long?,
    val lastAdSpinEpochDay: Long?,
    val hasEverSpun: Boolean,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
