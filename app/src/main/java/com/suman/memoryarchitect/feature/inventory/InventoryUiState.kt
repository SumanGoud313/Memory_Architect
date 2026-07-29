package com.suman.memoryarchitect.feature.inventory

import com.suman.memoryarchitect.domain.model.Inventory

data class InventoryUiState(
    val isLoading: Boolean = true,
    val inventory: Inventory = Inventory.EMPTY,
    /** Guards [InventoryViewModel.openMysteryChest] against a double-tap the same way
     * [com.suman.memoryarchitect.feature.gameplay.HintUiState.isRedeemingToken] guards a Hint
     * Token redemption - set for the whole in-flight window, not just the network call. */
    val isOpeningChest: Boolean = false,
    /** See [isOpeningChest]'s doc - same guard for [InventoryViewModel.applyXpBoost]. */
    val isApplyingXpBoost: Boolean = false,
)
