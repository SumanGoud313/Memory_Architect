package com.suman.memoryarchitect.feature.shop

import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.domain.model.SpinResult

sealed interface LuckySpinUiState {
    data object Loading : LuckySpinUiState
    data class Content(
        val coins: Long,
        /** Whether today's one free spin is still unspent - see
         * [com.suman.memoryarchitect.domain.model.LuckySpinState.lastFreeSpinEpochDay]'s doc. */
        val canSpinFree: Boolean,
        /** Whether today's one ad-gated bonus spin is still unspent - only ever relevant once
         * [canSpinFree] is false, matching [com.suman.memoryarchitect.ui.screens.gameplay.HintButton]'s
         * free-then-ad priority. */
        val canSpinAd: Boolean,
        /** Owned Lucky Spin Tickets - each is an extra spin independent of [canSpinFree]/[canSpinAd],
         * see [com.suman.memoryarchitect.domain.repository.ShopRepository.spin]'s doc. */
        val ticketCount: Int,
        /** True until this player's very first-ever spin resolves - drives the "First Spin Bonus"
         * callout so a guaranteed cosmetic reads as special, not arbitrary. Captured before the
         * spin call (from [com.suman.memoryarchitect.domain.model.LuckySpinState.hasEverSpun])
         * since [lastResult] alone can't distinguish "guaranteed" from "just got lucky." */
        val isFirstSpinEver: Boolean,
        val isSpinning: Boolean = false,
        val lastResult: SpinResult? = null,
        val wasFirstSpin: Boolean = false,
        val errorReason: ShopFailureReason? = null,
        val rewardedAdState: RewardedAdUiState = RewardedAdUiState.Idle,
    ) : LuckySpinUiState {
        val canSpin: Boolean get() = canSpinFree || canSpinAd || ticketCount > 0
    }
}
