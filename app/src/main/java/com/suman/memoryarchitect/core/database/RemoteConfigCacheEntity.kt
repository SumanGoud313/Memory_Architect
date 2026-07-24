package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TTL'd read-through cache of a single remote-config key/value pair (difficulty curves,
 * scoring weights, ad cadence, event toggles). The server remains the source of truth;
 * this table only bridges brief reconnects, per the repository cache policy.
 */
@Entity(tableName = "remote_config_cache")
data class RemoteConfigCacheEntity(
    @PrimaryKey val configKey: String,
    val value: String,
    val fetchedAt: Long,
)