package com.suman.memoryarchitect.domain.model

data class SceneObjectSpec(
    val objectId: String,
    val slotIndex: Int,
    val rotationDegrees: Int,
    val scale: Float,
    val isDistractor: Boolean,
)