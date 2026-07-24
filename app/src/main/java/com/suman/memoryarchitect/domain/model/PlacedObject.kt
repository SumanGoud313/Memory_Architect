package com.suman.memoryarchitect.domain.model

/** What the player actually did during Reconstruct, as opposed to [SceneObjectSpec]'s original target. */
data class PlacedObject(
    val objectId: String,
    val slotIndex: Int,
    val rotationDegrees: Int,
)
