package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SceneObjectDto(
    val objectId: String,
    val slotIndex: Int,
    val rotationDegrees: Int,
    val scale: Float,
    val isDistractor: Boolean,
)