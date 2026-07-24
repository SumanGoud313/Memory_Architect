package com.suman.memoryarchitect.feature.levelselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.usecase.GetLevelCampaignProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    private val getCampaignProgress: GetLevelCampaignProgressUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LevelSelectUiState>(LevelSelectUiState.Loading)
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Re-invoked from [LevelSelectFragment.onResume] so progress reflects a just-completed level. */
    fun load() {
        viewModelScope.launch {
            val progress = getCampaignProgress()
            _uiState.value = LevelSelectUiState.Loaded(progress.maxUnlockedLevel, progress.bestTimes, progress.bestStars)
        }
    }
}
