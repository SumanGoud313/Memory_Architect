package com.suman.memoryarchitect.feature.gameplay

/**
 * Redo budget for the current level attempt. [redosUsed] is loaded from persisted per-level
 * storage when the level loads and only ever increases for that level number - retrying the
 * same level does not refresh the budget, mirroring [HintUiState].
 *
 * [maxRewardedRedos]/[rewardedRedosUsed] track rewarded-ad grants independently of the free
 * budget above (see [com.suman.memoryarchitect.domain.model.RewardedAssistLimits]) - once
 * [rewardedRemaining] hits zero, no further ad can grant another redo for this level attempt.
 */
data class RedoUiState(
    val maxRedos: Int,
    val redosUsed: Int,
    val maxRewardedRedos: Int = 0,
    val rewardedRedosUsed: Int = 0,
    /** How many [com.suman.memoryarchitect.domain.model.InventoryItemKind.REDO_TOKEN] the player
     * holds right now (see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryRedoToken]) -
     * an alternative to [canWatchRewardedAd] that redeems a token instead of watching an ad. */
    val inventoryTokenCount: Int = 0,
    /** See [HintUiState.isRedeemingToken]'s doc - same in-flight redemption guard. */
    val isRedeemingToken: Boolean = false,
) {
    val remaining: Int get() = (maxRedos - redosUsed).coerceAtLeast(0)
    val canRedo: Boolean get() = remaining > 0
    val rewardedRemaining: Int get() = (maxRewardedRedos - rewardedRedosUsed).coerceAtLeast(0)
    val hasInventoryToken: Boolean get() = inventoryTokenCount > 0

    /** True only while the free budget is spent but a rewarded ad can still grant another redo -
     * the one state in which [RedoButton] should offer [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.watchRewardedRedoAd]. */
    val canWatchRewardedAd: Boolean get() = remaining <= 0 && rewardedRemaining > 0

    /** True whenever a Redo Token would actually help - see [HintUiState.canUseInventoryToken]'s
     * doc for the same reasoning. */
    val canUseInventoryToken: Boolean get() = canWatchRewardedAd && hasInventoryToken

    /** True once both the free budget AND the rewarded budget are spent - Redo is fully disabled
     * for the rest of this level attempt (see [com.suman.memoryarchitect.ui.screens.gameplay.RedoButton]). */
    val isRewardExhausted: Boolean get() = remaining <= 0 && rewardedRemaining <= 0

    companion object {
        val Initial = RedoUiState(maxRedos = 0, redosUsed = 0)
    }
}
