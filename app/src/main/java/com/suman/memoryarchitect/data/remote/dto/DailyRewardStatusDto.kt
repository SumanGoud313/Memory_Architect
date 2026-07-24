package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DailyRewardStatusDto(
    val cycleDay: Int,
    val canClaimToday: Boolean,
    val lastClaimedEpochDay: Long?,
)
