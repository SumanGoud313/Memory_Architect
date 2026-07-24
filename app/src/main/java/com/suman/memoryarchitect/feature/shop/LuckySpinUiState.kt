package com.suman.memoryarchitect.feature.shop

import com.suman.memoryarchitect.domain.model.SpinResult

sealed interface LuckySpinUiState {
    data object Loading : LuckySpinUiState
    data class Content(
        val coins: Long,
        val spinCostCoins: Long,
        val isSpinning: Boolean = false,
        val lastResult: SpinResult? = null,
        val errorReason: ShopFailureReason? = null,
    ) : LuckySpinUiState
}
