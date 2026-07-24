package com.suman.memoryarchitect.domain.model

data class DailyRewardStatus(
    val cycleDay: Int,
    val canClaimToday: Boolean,
    val lastClaimedEpochDay: Long?,
)
