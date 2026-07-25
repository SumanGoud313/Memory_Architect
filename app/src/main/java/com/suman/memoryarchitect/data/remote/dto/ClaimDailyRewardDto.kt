package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClaimDailyRewardRequestDto(val claimedOnEpochDay: Long)

@JsonClass(generateAdapter = true)
data class ClaimDailyRewardResponseDto(
    val cycleDay: Int,
    val coinsAwarded: Long,
    val xpAwarded: Long,
    val profile: PlayerProfileDto,
    val shieldAwarded: Boolean = false,
)
