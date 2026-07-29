package com.suman.memoryarchitect.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.InventoryRepository
import com.suman.memoryarchitect.domain.usecase.GetInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Inventory tab of [com.suman.memoryarchitect.ui.screens.shop.CosmeticsHubScreen] - the
 * permanent home for every earned consumable, see [com.suman.memoryarchitect.domain.model.InventoryItemKind]'s doc. */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventory: GetInventoryUseCase,
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableSharedFlow<InventoryActionResult>(extraBufferCapacity = 1)
    val actionResult: SharedFlow<InventoryActionResult> = _actionResult.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val outcome = getInventory()
            val current = _uiState.value
            _uiState.value = current.copy(
                isLoading = false,
                inventory = (outcome as? Outcome.Success)?.data ?: Inventory.EMPTY,
            )
        }
    }

    /** Consumes one Mystery Chest for a random coin reward - see
     * [com.suman.memoryarchitect.domain.progression.MysteryChestOdds]. [isOpeningChest] guards
     * against a double-tap the same way [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryHintToken]'s
     * `isRedeemingToken` guards a Hint Token redemption. */
    fun openMysteryChest() {
        val current = _uiState.value
        if (current.isOpeningChest) return
        _uiState.value = current.copy(isOpeningChest = true)
        viewModelScope.launch {
            try {
                when (val outcome = inventoryRepository.openMysteryChest()) {
                    is Outcome.Success -> {
                        _actionResult.tryEmit(InventoryActionResult.MysteryChestOpened(outcome.data.coinsAwarded))
                        refresh()
                    }
                    is Outcome.Error -> _actionResult.tryEmit(InventoryActionResult.Failed)
                }
            } finally {
                _uiState.value = _uiState.value.copy(isOpeningChest = false)
            }
        }
    }

    /** Consumes one XP Boost for an immediate flat XP grant - see
     * [com.suman.memoryarchitect.domain.progression.XpBoostRules]. Same re-entrancy guard as
     * [openMysteryChest]. */
    fun applyXpBoost() {
        val current = _uiState.value
        if (current.isApplyingXpBoost) return
        _uiState.value = current.copy(isApplyingXpBoost = true)
        viewModelScope.launch {
            try {
                when (val outcome = inventoryRepository.applyXpBoost()) {
                    is Outcome.Success -> {
                        _actionResult.tryEmit(InventoryActionResult.XpBoostApplied(outcome.data))
                        refresh()
                    }
                    is Outcome.Error -> _actionResult.tryEmit(InventoryActionResult.Failed)
                }
            } finally {
                _uiState.value = _uiState.value.copy(isApplyingXpBoost = false)
            }
        }
    }
}
