package com.suman.memoryarchitect.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logInventoryItemGranted
import com.suman.memoryarchitect.core.analytics.logReturningPlayerSession
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.ReturningPlayerWelcome
import com.suman.memoryarchitect.domain.usecase.ClaimReturningPlayerGiftUseCase
import com.suman.memoryarchitect.domain.usecase.GetReturningPlayerWelcomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [com.suman.memoryarchitect.ui.screens.home.ReturningPlayerBanner] - a one-shot check per
 * Home visit (see [GetReturningPlayerWelcomeUseCase]'s doc for why this needs no polling: the
 * moment the player actually plays a level, `lastPlayedEpochDay` updates and the gap naturally
 * resets to zero, so the banner just stops reappearing on its own with no extra dismissal state
 * to track beyond [dismiss] for the current session). */
@HiltViewModel
class ReturningPlayerViewModel @Inject constructor(
    private val getReturningPlayerWelcome: GetReturningPlayerWelcomeUseCase,
    private val claimReturningPlayerGift: ClaimReturningPlayerGiftUseCase,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    private val _welcome = MutableStateFlow<ReturningPlayerWelcome?>(null)
    val welcome: StateFlow<ReturningPlayerWelcome?> = _welcome.asStateFlow()

    private val _isClaiming = MutableStateFlow(false)
    val isClaiming: StateFlow<Boolean> = _isClaiming.asStateFlow()

    init {
        viewModelScope.launch {
            val result = getReturningPlayerWelcome()
            _welcome.value = result
            if (result != null) analytics.logReturningPlayerSession(result.gapDays)
        }
    }

    /** No-op if there's nothing claimable, or a claim is already in flight - the UI only ever
     * shows the claim affordance when [ReturningPlayerWelcome.canClaimGift] is true, but this
     * guards the same invariant here so a double-tap can never double-claim. */
    fun claimGift() {
        val current = _welcome.value ?: return
        if (!current.canClaimGift || _isClaiming.value) return
        _isClaiming.value = true
        viewModelScope.launch {
            when (val outcome = claimReturningPlayerGift()) {
                is Outcome.Success -> {
                    analytics.logInventoryItemGranted(InventoryItemKind.MYSTERY_CHEST.name, 1, source = "returning_player_gift")
                    _welcome.value = current.copy(canClaimGift = false)
                }
                is Outcome.Error -> Unit
            }
            _isClaiming.value = false
        }
    }

    /** Dismisses the banner for the rest of this Home visit - reappears on the next cold ViewModel
     * (e.g. leaving and re-entering Home) exactly as before, since nothing here is persisted; the
     * player playing a level (not dismissing) is what actually stops it for good. */
    fun dismiss() {
        _welcome.value = null
    }
}
