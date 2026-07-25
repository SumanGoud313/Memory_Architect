package com.suman.memoryarchitect.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.GetDailyRewardStatusUseCase
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the two small, quiet Home signals that don't rise to a full dashboard: a badge dot on the
 * profile icon when a Daily Reward is waiting, and [currentStreak] for [HomeStreakChip] - both
 * best-effort (a failed/offline check just leaves them off, never an error state) since neither is
 * more than a nudge toward Profile, where the real detail already lives.
 */
@HiltViewModel
class DailyRewardBadgeViewModel @Inject constructor(
    private val getDailyRewardStatus: GetDailyRewardStatusUseCase,
    private val getProfile: GetPlayerProfileUseCase,
) : ViewModel() {

    private val _hasClaimableReward = MutableStateFlow(false)
    val hasClaimableReward: StateFlow<Boolean> = _hasClaimableReward.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    init {
        viewModelScope.launch {
            val outcome = getDailyRewardStatus()
            _hasClaimableReward.value = (outcome as? Outcome.Success)?.data?.canClaimToday == true
        }
        viewModelScope.launch {
            val outcome = getProfile()
            _currentStreak.value = (outcome as? Outcome.Success)?.data?.currentStreak ?: 0
        }
    }
}
