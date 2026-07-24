package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScoreSubmissionRequestDto(
    val mode: String,
    val levelSeed: Long,
    val finalScore: Int,
    val sceneAccuracy: Float,
    val comboCount: Int,
    val playedOnEpochDay: Long,
)
