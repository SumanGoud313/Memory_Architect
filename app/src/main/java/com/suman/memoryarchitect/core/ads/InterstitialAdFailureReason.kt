package com.suman.memoryarchitect.core.ads

/** Mirrors [RewardedAdFailureReason] exactly - same three failure classes, no reward-specific
 * concept to add here. */
enum class InterstitialAdFailureReason {
    NO_INTERNET,
    AD_UNAVAILABLE,
    SHOW_FAILED,
}
