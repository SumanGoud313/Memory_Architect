package com.suman.memoryarchitect.domain.model

/**
 * Caps on rewarded-ad-granted assist uses, per level attempt - independent of [HintRules]/
 * [RedoRules]/[RewatchRules]'s free tiers, and the same at every level number (unlike those, which
 * tier by level). Exists so a rewarded ad remains a real monetization lever without letting a
 * single stubborn level be farmed for unlimited free ads - once a level's rewarded budget for a
 * given helper is spent, that helper is ad-gated shut for the rest of *this* attempt only; a retry
 * or the next level always gets a full budget again (see `HintUsageDao`/`RedoUsageDao`/
 * `RewatchUsageDao`, keyed by level number exactly like the free counters).
 */
data class RewardedAssistLimits(
    val maxRewardedHints: Int = 5,
    val maxRewardedRedos: Int = 5,
    val maxRewardedRewatches: Int = 3,
) {
    companion object {
        val Default = RewardedAssistLimits()
    }
}
