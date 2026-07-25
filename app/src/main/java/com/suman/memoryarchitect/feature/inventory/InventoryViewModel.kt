package com.suman.memoryarchitect.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.GetInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Inventory tab of [com.suman.memoryarchitect.ui.screens.shop.CosmeticsHubScreen] - the
 * permanent home for every earned consumable, see [com.suman.memoryarchitect.domain.model.InventoryItemKind]'s doc. */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventory: GetInventoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val outcome = getInventory()
            _uiState.value = InventoryUiState(
                isLoading = false,
                inventory = (outcome as? Outcome.Success)?.data ?: Inventory.EMPTY,
            )
        }
    }
}
