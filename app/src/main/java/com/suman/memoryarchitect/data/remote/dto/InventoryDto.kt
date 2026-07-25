package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/** [quantities] keys are [com.suman.memoryarchitect.domain.model.InventoryItemKind.name] - a flat
 * map rather than per-kind fields, the same shape `mock-backend/missions.js`'s inventory state and
 * Firestore's `inventory/{uid}` document both use, so adding a new kind later never needs a schema
 * change on any of the three. */
@JsonClass(generateAdapter = true)
data class InventoryDto(
    val quantities: Map<String, Int> = emptyMap(),
)
