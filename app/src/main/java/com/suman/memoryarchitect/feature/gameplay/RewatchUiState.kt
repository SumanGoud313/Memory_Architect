package com.suman.memoryarchitect.feature.gameplay

/**
 * Free-tier Rewatch budget for the current level attempt. [rewatchesUsed] is loaded from the same
 * persisted per-level storage the always-on ad flow already tracked (see [RewatchRepository]) -
 * retrying the same level does not refresh the budget, mirroring [HintUiState]/[RedoUiState].
 *
 * [maxRewardedRewatches]/[rewardedRewatchesUsed] track rewarded-ad grants independently of the
 * free budget above (see [com.suman.memoryarchitect.domain.model.RewardedAssistLimits]) - once
 * [rewardedRemaining] hits zero, no further ad can replay the scene again for this level attempt.
 */
data class RewatchUiState(
    val maxRewatches: Int,
    val rewatchesUsed: Int,
    val maxRewardedRewatches: Int = 0,
    val rewardedRewatchesUsed: Int = 0,
    /** How many [com.suman.memoryarchitect.domain.model.InventoryItemKind.REWATCH_TICKET] the
     * player holds right now (see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryRewatchToken]) -
     * an alternative to [canWatchRewardedAd] that redeems a ticket instead of watching an ad. */
    val inventoryTokenCount: Int = 0,
    /** See [HintUiState.isRedeemingToken]'s doc - same in-flight redemption guard. */
    val isRedeemingToken: Boolean = false,
) {
    val remaining: Int get() = (maxRewatches - rewatchesUsed).coerceAtLeast(0)
    val canRewatchFree: Boolean get() = remaining > 0
    val rewardedRemaining: Int get() = (maxRewardedRewatches - rewardedRewatchesUsed).coerceAtLeast(0)
    val hasInventoryToken: Boolean get() = inventoryTokenCount > 0

    /** True only while the free budget is spent but a rewarded ad can still grant another
     * rewatch - the one state in which [RewatchButton] should offer
     * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.watchRewatchAd]. */
    val canWatchRewardedAd: Boolean get() = remaining <= 0 && rewardedRemaining > 0

    /** True whenever a Rewatch Ticket would actually help - see [HintUiState.canUseInventoryToken]'s
     * doc for the same reasoning. */
    val canUseInventoryToken: Boolean get() = canWatchRewardedAd && hasInventoryToken

    /** True once both the free budget AND the rewarded budget are spent - Rewatch is fully
     * disabled for the rest of this level attempt (see [com.suman.memoryarchitect.ui.screens.gameplay.RewatchButton]). */
    val isRewardExhausted: Boolean get() = remaining <= 0 && rewardedRemaining <= 0

    companion object {
        val Initial = RewatchUiState(maxRewatches = 0, rewatchesUsed = 0)
    }
}
