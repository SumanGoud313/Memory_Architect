package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerProfileDto(
    val xp: Long,
    val coins: Long = 0L,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastPlayedEpochDay: Long?,
    val dailyChallengeWonAtEpochSecond: Long? = null,
    val weeklyChallengeWonAtEpochSecond: Long? = null,
)
