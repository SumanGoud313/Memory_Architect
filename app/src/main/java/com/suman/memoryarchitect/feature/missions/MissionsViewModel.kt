package com.suman.memoryarchitect.feature.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.ClaimMissionRewardUseCase
import com.suman.memoryarchitect.domain.usecase.GetActiveMissionsUseCase
import com.suman.memoryarchitect.domain.usecase.GrantInventoryItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the dedicated Missions screen - loads today's active Daily/Weekly/Monthly set (a pure,
 * local, deterministic computation, see [GetActiveMissionsUseCase]'s doc) and handles claiming. */
@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val getActiveMissions: GetActiveMissionsUseCase,
    private val claimMissionReward: ClaimMissionRewardUseCase,
    private val grantInventoryItem: GrantInventoryItemUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()

    private val _claimEvents = MutableSharedFlow<MissionClaimResult>(extraBufferCapacity = 1)
    val claimEvents: SharedFlow<MissionClaimResult> = _claimEvents.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val missions = getActiveMissions()
            _uiState.value = _uiState.value.copy(isLoading = false, missions = missions)
        }
    }

    /** No-op if a claim is already in flight, or [missionId] isn't actually claimable - the UI
     * only ever shows an enabled claim affordance for [com.suman.memoryarchitect.domain.model.ActiveMission.canClaim],
     * but this guards the same invariant here so a double-tap can never double-claim. */
    fun claim(missionId: MissionId) {
        val current = _uiState.value
        if (current.claimingMissionId != null) return
        val mission = current.missions.firstOrNull { it.definition.id == missionId } ?: return
        if (!mission.canClaim) return

        _uiState.value = current.copy(claimingMissionId = missionId)
        viewModelScope.launch {
            when (val outcome = claimMissionReward(missionId)) {
                is Outcome.Success -> {
                    val result = outcome.data
                    grantInventoryItem(result.inventory)
                    val refreshed = getActiveMissions()
                    _uiState.value = _uiState.value.copy(missions = refreshed, claimingMissionId = null)
                    _claimEvents.emit(result)
                }
                is Outcome.Error -> {
                    _uiState.value = _uiState.value.copy(claimingMissionId = null)
                }
            }
        }
    }
}
