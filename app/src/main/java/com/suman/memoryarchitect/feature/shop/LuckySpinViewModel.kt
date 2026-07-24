package com.suman.memoryarchitect.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.SpinRules
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import com.suman.memoryarchitect.domain.usecase.SpinLuckySpinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LuckySpinViewModel @Inject constructor(
    private val getProfile: GetPlayerProfileUseCase,
    private val spinLuckySpin: SpinLuckySpinUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LuckySpinUiState>(LuckySpinUiState.Loading)
    val uiState: StateFlow<LuckySpinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val coins = when (val outcome = getProfile()) {
                is Outcome.Success -> outcome.data.coins
                is Outcome.Error -> 0L
            }
            _uiState.value = LuckySpinUiState.Content(coins = coins, spinCostCoins = SpinRules.Default.spinCostCoins)
        }
    }

    fun spin() {
        val current = _uiState.value as? LuckySpinUiState.Content ?: return
        if (current.isSpinning || current.coins < current.spinCostCoins) return
        _uiState.value = current.copy(isSpinning = true, errorReason = null)
        viewModelScope.launch {
            when (val outcome = spinLuckySpin()) {
                is Outcome.Success -> {
                    val latest = _uiState.value as? LuckySpinUiState.Content ?: return@launch
                    _uiState.value = latest.copy(
                        coins = outcome.data.updatedProfile.coins,
                        isSpinning = false,
                        lastResult = outcome.data,
                    )
                }
                is Outcome.Error -> {
                    val latest = _uiState.value as? LuckySpinUiState.Content ?: return@launch
                    _uiState.value = latest.copy(isSpinning = false, errorReason = outcome.error.toShopFailureReason())
                }
            }
        }
    }
}
