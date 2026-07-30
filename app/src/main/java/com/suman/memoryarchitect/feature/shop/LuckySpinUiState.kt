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
        /** How many of today's rewarded-ad-gated bonus spins
         * ([com.suman.memoryarchitect.domain.progression.SpinRules.maxAdSpinsPerDay], 3) are still
         * unspent - only ever relevant once [canSpinFree] is false, matching
         * [com.suman.memoryarchitect.ui.screens.gameplay.HintButton]'s free-then-ad priority. */
        val adSpinsRemaining: Int,
        /** `null` whenever [canSpinFree] is true (nothing to count down to) - otherwise the epoch
         * second the free spin resets at (this device's local next midnight, matching whatever
         * clock zone [canSpinFree]/[lastFreeSpinEpochDay] were already compared in). Drives
         * [com.suman.memoryarchitect.ui.screens.shop.FreeSpinResetCountdown]'s "come back in Xh Ym"
         * readout, and flips [canSpinFree] back to `true` in-place the instant it reaches zero -
         * see [com.suman.memoryarchitect.feature.shop.LuckySpinViewModel.onFreeSpinResetReached]'s
         * doc for why that doesn't need a fresh server round-trip. */
        val nextFreeSpinAtEpochSecond: Long? = null,
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
        /** How many of today's ad-gated Mystery Chest claims
         * ([com.suman.memoryarchitect.domain.progression.MysteryChestAdRules.maxClaimsPerDay], 3)
         * are still unspent - independent of [canSpinFree]/[canSpinAd]/[ticketCount], since a
         * Mystery Chest claim is watch-ad-only with no free/ticket path. */
        val mysteryChestClaimsRemaining: Int = 0,
        /** True right after a Mystery Chest ad claim succeeds, until the player taps the resulting
         * "Available" button - see [com.suman.memoryarchitect.ui.screens.shop.LuckySpinScreen]'s
         * doc for that button's role. */
        val mysteryChestJustClaimed: Boolean = false,
        val mysteryChestRewardedAdState: RewardedAdUiState = RewardedAdUiState.Idle,
    ) : LuckySpinUiState {
        val canSpinAd: Boolean get() = adSpinsRemaining > 0
        val canSpin: Boolean get() = canSpinFree || canSpinAd || ticketCount > 0
    }
}
