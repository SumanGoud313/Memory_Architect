package com.suman.memoryarchitect.feature.inventory

import com.suman.memoryarchitect.domain.model.Inventory

data class InventoryUiState(
    val isLoading: Boolean = true,
    val inventory: Inventory = Inventory.EMPTY,
)
