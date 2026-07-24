package com.suman.memoryarchitect.core.ads

sealed interface RewardedAdResult {
    data object Rewarded : RewardedAdResult
    data object Cancelled : RewardedAdResult
    data class Failed(val reason: RewardedAdFailureReason) : RewardedAdResult
}
