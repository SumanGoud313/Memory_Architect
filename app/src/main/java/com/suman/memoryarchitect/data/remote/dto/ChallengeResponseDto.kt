package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * expiresAtEpochSecond isn't consumed yet — reserved for a future "time until reset" UI on
 * the daily/weekly challenge entry points.
 */
@JsonClass(generateAdapter = true)
data class ChallengeResponseDto(
    val level: LevelSpecDto,
    val expiresAtEpochSecond: Long,
)
