package com.suman.memoryarchitect.core.ads

/** Mirrors [RewardedAdResult] minus the reward concept - an interstitial has no "earned"/"declined"
 * distinction, only whether it was actually shown (and dismissed) or not. */
sealed interface InterstitialAdResult {
    data object Shown : InterstitialAdResult
    data class Failed(val reason: InterstitialAdFailureReason) : InterstitialAdResult
}
