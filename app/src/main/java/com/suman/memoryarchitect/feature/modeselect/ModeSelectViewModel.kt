package com.suman.memoryarchitect.feature.modeselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logModeSelected
import com.suman.memoryarchitect.core.analytics.setPreferredGameModeProperty
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.common.challengeLockDurationSeconds
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.XpCurve
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the quiet progress footer at the bottom of Mode Select — level, XP, coins, streak, no
 * more. It is deliberately not a second Profile: a failed fetch never surfaces an error or blocks
 * the screen (mode selection is why the player is here, not this), it just falls back to
 * [PlayerProfile.EMPTY] so the footer is always populated instead of ever going blank.
 */
@HiltViewModel
class ModeSelectViewModel @Inject constructor(
    private val getProfile: GetPlayerProfileUseCase,
    private val analytics: AnalyticsLogger,
    private val preferences: UserPreferencesDataStore,
    billingManager: BillingManager,
) : ViewModel() {

    private val xpCurve = XpCurve()

    private val _progress = MutableStateFlow(toProgressUiState(PlayerProfile.EMPTY))
    val progress: StateFlow<ModeSelectProgressUiState> = _progress.asStateFlow()

    /** Drives the Remove Ads promo card at the bottom of this screen - hidden entirely once
     * `true` rather than showing a "Purchased" status here (unlike the equivalent Settings row
     * this replaced), since Mode Select is a high-frequency action screen, not a status screen -
     * nagging an already-paying player every single time they pick a mode would be the opposite
     * of what this purchase is supposed to buy them. */
    val hasRemovedAds: StateFlow<Boolean> = billingManager.hasRemovedAds

    /** Called from a screen-level `LaunchedEffect(Unit)` (never from `init`) — Gameplay returns
     * here via `popBackStack()`, which reuses this same ViewModel instance rather than creating a
     * fresh one, so a one-time `init` fetch would never see a Daily/Weekly Challenge win recorded
     * after this screen's first visit. Re-invoking on every (re-)entry is what makes the post-win
     * lock (see [ModeSelectProgressUiState]) show up immediately instead of one navigation late. */
    fun refresh() {
        viewModelScope.launch {
            val outcome = getProfile()
            if (outcome is Outcome.Success) {
                _progress.value = toProgressUiState(outcome.data)
            }
        }
    }

    fun onModeSelected(mode: GameMode) {
        analytics.logModeSelected(mode)
        viewModelScope.launch {
            val preferredMode = preferences.recordModeSelectedAndGetPreferred(mode.name)
            analytics.setPreferredGameModeProperty(preferredMode)
        }
    }

    private fun toProgressUiState(profile: PlayerProfile): ModeSelectProgressUiState {
        val (xpIntoLevel, xpForNextLevel) = xpCurve.xpProgressWithinLevel(profile.xp)
        return ModeSelectProgressUiState(
            level = xpCurve.levelForXp(profile.xp),
            xpIntoLevel = xpIntoLevel,
            xpForNextLevel = xpForNextLevel,
            coins = profile.coins,
            currentStreak = profile.currentStreak,
            dailyChallengeUnlockAtEpochSecond = profile.dailyChallengeWonAtEpochSecond
                ?.plus(GameMode.DAILY_CHALLENGE.challengeLockDurationSeconds()!!),
            weeklyChallengeUnlockAtEpochSecond = profile.weeklyChallengeWonAtEpochSecond
                ?.plus(GameMode.WEEKLY_CHALLENGE.challengeLockDurationSeconds()!!),
        )
    }
}
