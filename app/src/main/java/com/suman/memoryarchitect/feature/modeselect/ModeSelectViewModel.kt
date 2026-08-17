package com.suman.memoryarchitect.feature.modeselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logModeSelected
import com.suman.memoryarchitect.core.analytics.setPreferredGameModeProperty
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.premiumStoreEnabled
import com.suman.memoryarchitect.core.common.challengeLockDurationSeconds
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Daily/Weekly Challenge post-win lock countdowns on Mode Select's mode cards. It is
 * deliberately not a second Profile: a failed fetch never surfaces an error or blocks the screen
 * (mode selection is why the player is here, not this), it just falls back to
 * [PlayerProfile.EMPTY] (both unlock timestamps null, i.e. "not locked") instead of ever blocking.
 */
@HiltViewModel
class ModeSelectViewModel @Inject constructor(
    private val getProfile: GetPlayerProfileUseCase,
    private val analytics: AnalyticsLogger,
    private val preferences: UserPreferencesDataStore,
    billingManager: BillingManager,
    remoteConfigRepository: RemoteConfigRepository,
) : ViewModel() {

    private val _progress = MutableStateFlow(toProgressUiState(PlayerProfile.EMPTY))
    val progress: StateFlow<ModeSelectProgressUiState> = _progress.asStateFlow()

    // Optimistic `true` default, same "a slow/failed fetch should never hide a real purchase
    // surface" reasoning BannerAdViewModel's own _remoteConfigBannerEnabled already documents -
    // only a console-set `false`, once actually fetched, should ever hide this.
    private val _premiumStoreEnabled = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            val remoteConfig = (remoteConfigRepository.getRemoteConfig() as? Outcome.Success)?.data
            if (remoteConfig != null) _premiumStoreEnabled.value = remoteConfig.premiumStoreEnabled()
        }
    }

    /** Drives the Remove Ads promo card at the bottom of this screen - hidden whenever the player
     * already owns it (unlike the equivalent Settings row this replaced, since Mode Select is a
     * high-frequency action screen, not a status screen - nagging an already-paying player every
     * single time they pick a mode would be the opposite of what this purchase is supposed to buy
     * them) or whenever Remote Config's `premium_store_enabled` is off (see
     * [com.suman.memoryarchitect.core.billing.premiumStoreEnabled]'s doc - the same instant,
     * no-release kill switch [com.suman.memoryarchitect.feature.ads.BannerAdViewModel.shouldShowBanner]
     * already uses this pattern for). */
    val showRemoveAdsButton: StateFlow<Boolean> =
        combine(billingManager.hasRemovedAds, _premiumStoreEnabled) { hasRemovedAds, storeEnabled ->
            !hasRemovedAds && storeEnabled
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

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

    private fun toProgressUiState(profile: PlayerProfile): ModeSelectProgressUiState =
        ModeSelectProgressUiState(
            dailyChallengeUnlockAtEpochSecond = profile.dailyChallengeWonAtEpochSecond
                ?.plus(GameMode.DAILY_CHALLENGE.challengeLockDurationSeconds()!!),
            weeklyChallengeUnlockAtEpochSecond = profile.weeklyChallengeWonAtEpochSecond
                ?.plus(GameMode.WEEKLY_CHALLENGE.challengeLockDurationSeconds()!!),
        )
}
