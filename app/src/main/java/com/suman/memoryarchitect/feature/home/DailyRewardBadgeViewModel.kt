package com.suman.memoryarchitect.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.GetDailyRewardStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs a single small badge dot on Home's profile icon — deliberately the *only* piece of state
 * Home knows about, so a claimable Daily Reward stays discoverable without reintroducing the
 * dashboard this screen was simplified away from. A failed/offline check just leaves the badge
 * off; it's a nudge, not something worth an error state over.
 */
@HiltViewModel
class DailyRewardBadgeViewModel @Inject constructor(
    private val getDailyRewardStatus: GetDailyRewardStatusUseCase,
) : ViewModel() {

    private val _hasClaimableReward = MutableStateFlow(false)
    val hasClaimableReward: StateFlow<Boolean> = _hasClaimableReward.asStateFlow()

    init {
        viewModelScope.launch {
            val outcome = getDailyRewardStatus()
            _hasClaimableReward.value = (outcome as? Outcome.Success)?.data?.canClaimToday == true
        }
    }
}
