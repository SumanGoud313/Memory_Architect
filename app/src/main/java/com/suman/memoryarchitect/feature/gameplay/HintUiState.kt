package com.suman.memoryarchitect.feature.gameplay

import com.suman.memoryarchitect.domain.model.HintReveal

/**
 * Hint budget + selection-mode state for the current level attempt. [hintsUsed] is loaded from
 * persisted per-level storage when the level loads and only ever increases for that level number
 * - retrying the same level does not refresh the budget, so hints stay a genuinely scarce
 * resource rather than something a player can reset by replaying.
 *
 * [maxRewardedHints]/[rewardedHintsUsed] track rewarded-ad grants independently of the free
 * budget above (see [com.suman.memoryarchitect.domain.model.RewardedAssistLimits]) - once
 * [rewardedRemaining] hits zero, no further ad can grant another hint for this level attempt,
 * capping what would otherwise be unlimited ad-watching on a single stubborn level.
 *
 * [isArmed] is "the player tapped Hint and is now expected to tap a tray object" - it is not the
 * hint itself. [activeReveal] is the live 2-second reveal driving the scene-panel glow/pulse/
 * arrow/sparkle highlight; it is cleared automatically by the ViewModel once that window elapses.
 */
data class HintUiState(
    val maxHints: Int,
    val hintsUsed: Int,
    val maxRewardedHints: Int = 0,
    val rewardedHintsUsed: Int = 0,
    val isArmed: Boolean = false,
    val activeReveal: HintReveal? = null,
    /** How many [com.suman.memoryarchitect.domain.model.InventoryItemKind.HINT_TOKEN] the player
     * holds right now (see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryHintToken]) -
     * an alternative to [canWatchRewardedAd] that redeems a token instead of watching an ad.
     * Refreshed from the real Inventory balance, so it's always the true owned count, not just a
     * yes/no - the assist UI displays this count directly. */
    val inventoryTokenCount: Int = 0,
    /** True for the single in-flight window between tapping to redeem a token and that redemption
     * resolving (success or failure) - see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryHintToken].
     * Guards against a second rapid tap starting a second concurrent redemption before the first
     * one's state update has landed. */
    val isRedeemingToken: Boolean = false,
) {
    val remaining: Int get() = (maxHints - hintsUsed).coerceAtLeast(0)
    val canUseHint: Boolean get() = remaining > 0
    val rewardedRemaining: Int get() = (maxRewardedHints - rewardedHintsUsed).coerceAtLeast(0)
    val hasInventoryToken: Boolean get() = inventoryTokenCount > 0

    /** True only while the free budget is spent but a rewarded ad can still grant another hint -
     * the one state in which [HintButton] should offer [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.watchRewardedAd]. */
    val canWatchRewardedAd: Boolean get() = remaining <= 0 && rewardedRemaining > 0

    /** True whenever a Hint Token would actually help - the same "free budget spent, rewarded
     * budget still open" window [canWatchRewardedAd] offers, since a token fills the exact same
     * slot an ad would (see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryHintToken]'s doc). */
    val canUseInventoryToken: Boolean get() = canWatchRewardedAd && hasInventoryToken

    /** True once both the free budget AND the rewarded budget are spent - Hint is fully disabled
     * for the rest of this level attempt (see [com.suman.memoryarchitect.ui.screens.gameplay.HintButton]). */
    val isRewardExhausted: Boolean get() = remaining <= 0 && rewardedRemaining <= 0

    companion object {
        val Initial = HintUiState(maxHints = 0, hintsUsed = 0)
    }
}
