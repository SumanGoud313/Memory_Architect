package com.suman.memoryarchitect.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.usecase.GetUnlockedRewardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the dedicated Rewards screen (Profile → Rewards button) - the full catalog (locked and
 * unlocked) lives in [com.suman.memoryarchitect.domain.progression.RewardCatalog] itself, a static
 * timeline the screen reads directly; this only needs to resolve which ids are unlocked, the same
 * [GetUnlockedRewardsUseCase] [ProfileViewModel] already uses. */
@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val getUnlockedRewards: GetUnlockedRewardsUseCase,
) : ViewModel() {

    private val _unlockedIds = MutableStateFlow<Set<RewardId>?>(null)
    val unlockedIds: StateFlow<Set<RewardId>?> = _unlockedIds.asStateFlow()

    init {
        viewModelScope.launch { _unlockedIds.value = getUnlockedRewards() }
    }
}
