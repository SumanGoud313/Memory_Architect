package com.suman.memoryarchitect.feature.inventory

/** One-shot result of [InventoryViewModel.openMysteryChest]/[InventoryViewModel.applyXpBoost] -
 * collected once by [com.suman.memoryarchitect.ui.screens.shop.InventoryScreen] to show a Snackbar,
 * the same one-shot-event pattern this app's other ViewModels already use for a result that
 * shouldn't replay on rotation (e.g. `MissionsViewModel.claimEvents`). */
sealed interface InventoryActionResult {
    data class MysteryChestOpened(val coinsAwarded: Long) : InventoryActionResult
    data class XpBoostApplied(val xpGranted: Long) : InventoryActionResult
    data object Failed : InventoryActionResult
}
