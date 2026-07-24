package com.suman.memoryarchitect.domain.model

data class DailyRewardClaimResult(
    val cycleDay: Int,
    val coinsAwarded: Long,
    val xpAwarded: Long,
    val profile: PlayerProfile,
)
