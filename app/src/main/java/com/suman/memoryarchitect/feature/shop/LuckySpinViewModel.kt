package com.suman.memoryarchitect.feature.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.ads.RewardedAdController
import com.suman.memoryarchitect.core.ads.RewardedAdFlow
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.model.MysteryChestAdState
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.MysteryChestAdRules
import com.suman.memoryarchitect.domain.progression.SpinRules
import com.suman.memoryarchitect.domain.repository.SpinSource
import com.suman.memoryarchitect.domain.usecase.ClaimAdMysteryChestUseCase
import com.suman.memoryarchitect.domain.usecase.GetInventoryUseCase
import com.suman.memoryarchitect.domain.usecase.GetLuckySpinStateUseCase
import com.suman.memoryarchitect.domain.usecase.GetMysteryChestAdStateUseCase
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import com.suman.memoryarchitect.domain.usecase.SpinLuckySpinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** How long [LuckySpinScreen]'s wheel animation always runs before a reward can reveal - see
 * [spin]'s doc for why the network call is raced against this rather than driving the animation
 * off the call's own latency. */
const val LUCKY_SPIN_ANIMATION_DURATION_MS = 5_000L

@HiltViewModel
class LuckySpinViewModel @Inject constructor(
    private val getProfile: GetPlayerProfileUseCase,
    private val getInventory: GetInventoryUseCase,
    private val getLuckySpinState: GetLuckySpinStateUseCase,
    private val spinLuckySpin: SpinLuckySpinUseCase,
    private val getMysteryChestAdState: GetMysteryChestAdStateUseCase,
    private val claimAdMysteryChest: ClaimAdMysteryChestUseCase,
    rewardedAdController: RewardedAdController,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LuckySpinUiState>(LuckySpinUiState.Loading)
    val uiState: StateFlow<LuckySpinUiState> = _uiState.asStateFlow()

    private val adFlow = RewardedAdFlow(viewModelScope, rewardedAdController, featureName = "lucky_spin")
    val rewardedAdState: StateFlow<RewardedAdUiState> = adFlow.state

    /** Its own [RewardedAdFlow] instance, entirely independent of [adFlow] above - a Mystery Chest
     * ad claim and a wheel spin are two separate daily allowances (see
     * [com.suman.memoryarchitect.domain.model.MysteryChestAdState]'s doc) that can each be
     * mid-flight without affecting the other's loading state. */
    private val mysteryChestAdFlow = RewardedAdFlow(viewModelScope, rewardedAdController, featureName = "lucky_spin_mystery_chest")
    val mysteryChestRewardedAdState: StateFlow<RewardedAdUiState> = mysteryChestAdFlow.state

    init {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val profile = (getProfile() as? Outcome.Success)?.data
        val spinState = getLuckySpinState()
        val mysteryChestState = getMysteryChestAdState()
        val ticketCount = (getInventory() as? Outcome.Success)?.data?.quantityOf(InventoryItemKind.LUCKY_SPIN_TICKET) ?: 0
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        _uiState.value = LuckySpinUiState.Content(
            coins = profile?.coins ?: 0L,
            canSpinFree = spinState.lastFreeSpinEpochDay != todayEpochDay,
            adSpinsRemaining = spinState.adSpinsRemaining(todayEpochDay),
            ticketCount = ticketCount,
            isFirstSpinEver = !spinState.hasEverSpun,
            mysteryChestClaimsRemaining = mysteryChestState.claimsRemaining(todayEpochDay),
        )
    }

    /** How many of [SpinRules.maxAdSpinsPerDay]'s rewarded-ad bonus spins are still unspent today -
     * 0 once [LuckySpinState.adSpinsUsedToday] (for today's [LuckySpinState.lastAdSpinEpochDay])
     * reaches the cap, [SpinRules.maxAdSpinsPerDay] in full on a day with no ad spins yet. */
    private fun LuckySpinState.adSpinsRemaining(todayEpochDay: Long): Int {
        val usedToday = if (lastAdSpinEpochDay == todayEpochDay) adSpinsUsedToday else 0
        return (SpinRules.Default.maxAdSpinsPerDay - usedToday).coerceAtLeast(0)
    }

    /** Same shape as [LuckySpinState.adSpinsRemaining] above, for
     * [MysteryChestAdRules.maxClaimsPerDay] instead of [SpinRules.maxAdSpinsPerDay]. */
    private fun MysteryChestAdState.claimsRemaining(todayEpochDay: Long): Int {
        val usedToday = if (lastClaimEpochDay == todayEpochDay) claimsUsedToday else 0
        return (MysteryChestAdRules.Default.maxClaimsPerDay - usedToday).coerceAtLeast(0)
    }

    /** [source] must already be available per the current [LuckySpinUiState.Content] (the caller -
     * [LuckySpinScreen]'s adaptive Spin button - only ever offers a [source] it believes is
     * available, but this re-checks anyway rather than trusting the UI alone). The actual network
     * round-trip is raced against a hardcoded [LUCKY_SPIN_ANIMATION_DURATION_MS] delay so the
     * wheel always spins for the same real-feeling duration regardless of network latency - a fast
     * response waits out the rest of the animation instead of revealing early, and a slow one is
     * capped from the UI's perspective at exactly that duration (the call itself is still awaited
     * to completion, just never blocking the animation for longer than it needs to). */
    fun spin(source: SpinSource) {
        val current = _uiState.value as? LuckySpinUiState.Content ?: return
        if (current.isSpinning) return
        val available = when (source) {
            SpinSource.FREE -> current.canSpinFree
            SpinSource.AD -> current.canSpinAd
            SpinSource.TICKET -> current.ticketCount > 0
        }
        if (!available) return

        val wasFirstSpin = current.isFirstSpinEver
        _uiState.value = current.copy(isSpinning = true, errorReason = null, lastResult = null)
        viewModelScope.launch {
            val outcome = coroutineScope {
                val resultDeferred = async { spinLuckySpin(source) }
                delay(LUCKY_SPIN_ANIMATION_DURATION_MS)
                resultDeferred.await()
            }
            val latest = _uiState.value as? LuckySpinUiState.Content ?: return@launch
            when (outcome) {
                is Outcome.Success -> {
                    val spinState = outcome.data.updatedSpinState
                    val todayEpochDay = LocalDate.now(clock).toEpochDay()
                    val ticketCount = if (source == SpinSource.TICKET) {
                        (getInventory() as? Outcome.Success)?.data?.quantityOf(InventoryItemKind.LUCKY_SPIN_TICKET) ?: 0
                    } else {
                        latest.ticketCount
                    }
                    _uiState.value = latest.copy(
                        coins = outcome.data.updatedProfile.coins,
                        canSpinFree = spinState.lastFreeSpinEpochDay != todayEpochDay,
                        adSpinsRemaining = spinState.adSpinsRemaining(todayEpochDay),
                        ticketCount = ticketCount,
                        isFirstSpinEver = false,
                        isSpinning = false,
                        lastResult = outcome.data,
                        wasFirstSpin = wasFirstSpin,
                    )
                }
                is Outcome.Error -> {
                    _uiState.value = latest.copy(isSpinning = false, errorReason = outcome.error.toShopFailureReason())
                }
            }
        }
    }

    /** Grants [SpinSource.AD] once the ad fully resolves - re-validated against the *current*
     * state inside [onRewarded] (not the snapshot captured when the ad started), same "an ad's
     * load/show round-trip can outlast a cap being hit some other way" reasoning
     * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.watchRewardedAd]'s doc gives. */
    fun watchRewardedAd(activity: Activity) {
        val current = _uiState.value as? LuckySpinUiState.Content ?: return
        if (!current.canSpinAd || current.isSpinning) return
        adFlow.watch(activity) {
            spin(SpinSource.AD)
        }
    }

    /** Grants one Mystery Chest once the ad fully resolves - re-validated against the *current*
     * state inside the [RewardedAdFlow.watch] callback, same "an ad's load/show round-trip can
     * outlast a cap being hit some other way" reasoning [watchRewardedAd]'s own doc gives. Unlike
     * [spin], this has no free/ticket alternative to fall back to - watch-ad-only by design (see
     * [MysteryChestAdRules]'s doc), so there is nothing else for this button to ever offer once
     * [LuckySpinUiState.Content.mysteryChestClaimsRemaining] hits 0 for the day. */
    fun watchRewardedAdForMysteryChest(activity: Activity) {
        val current = _uiState.value as? LuckySpinUiState.Content ?: return
        if (current.mysteryChestClaimsRemaining <= 0) return
        mysteryChestAdFlow.watch(activity) {
            val latest = _uiState.value as? LuckySpinUiState.Content ?: return@watch
            when (val outcome = claimAdMysteryChest()) {
                is Outcome.Success -> {
                    val todayEpochDay = LocalDate.now(clock).toEpochDay()
                    _uiState.value = latest.copy(
                        mysteryChestClaimsRemaining = outcome.data.claimState.claimsRemaining(todayEpochDay),
                        mysteryChestJustClaimed = true,
                    )
                }
                is Outcome.Error -> {
                    _uiState.value = latest.copy(errorReason = outcome.error.toShopFailureReason())
                }
            }
        }
    }

    /** Called right before [com.suman.memoryarchitect.ui.screens.shop.LuckySpinScreen] navigates
     * to Inventory from the "Available" button - clears the callout so the tile goes back to
     * offering another ad watch (if any of today's claims remain) instead of getting stuck
     * showing "Available" for a chest the player has already gone to look at. */
    fun onMysteryChestAvailableClicked() {
        val current = _uiState.value as? LuckySpinUiState.Content ?: return
        _uiState.value = current.copy(mysteryChestJustClaimed = false)
    }
}
