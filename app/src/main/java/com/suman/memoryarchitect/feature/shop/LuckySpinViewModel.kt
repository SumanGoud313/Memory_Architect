package com.suman.memoryarchitect.feature.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.ads.RewardedAdController
import com.suman.memoryarchitect.core.ads.RewardedAdFlow
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.SpinSource
import com.suman.memoryarchitect.domain.usecase.GetInventoryUseCase
import com.suman.memoryarchitect.domain.usecase.GetLuckySpinStateUseCase
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
    rewardedAdController: RewardedAdController,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LuckySpinUiState>(LuckySpinUiState.Loading)
    val uiState: StateFlow<LuckySpinUiState> = _uiState.asStateFlow()

    private val adFlow = RewardedAdFlow(viewModelScope, rewardedAdController, featureName = "lucky_spin")
    val rewardedAdState: StateFlow<RewardedAdUiState> = adFlow.state

    init {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val profile = (getProfile() as? Outcome.Success)?.data
        val spinState = getLuckySpinState()
        val ticketCount = (getInventory() as? Outcome.Success)?.data?.quantityOf(InventoryItemKind.LUCKY_SPIN_TICKET) ?: 0
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        _uiState.value = LuckySpinUiState.Content(
            coins = profile?.coins ?: 0L,
            canSpinFree = spinState.lastFreeSpinEpochDay != todayEpochDay,
            canSpinAd = spinState.lastAdSpinEpochDay != todayEpochDay,
            ticketCount = ticketCount,
            isFirstSpinEver = !spinState.hasEverSpun,
        )
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
                        canSpinAd = spinState.lastAdSpinEpochDay != todayEpochDay,
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
}
