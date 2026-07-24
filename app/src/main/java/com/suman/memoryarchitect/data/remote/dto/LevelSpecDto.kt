package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LevelSpecDto(
    val seed: Long,
    val sceneType: String,
    val objects: List<SceneObjectDto>,
    val timeLimitMs: Long?,
    val memorizeDurationMs: Long,
    val orderModeEnabled: Boolean = false,
)