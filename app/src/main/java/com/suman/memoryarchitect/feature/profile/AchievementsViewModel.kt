package com.suman.memoryarchitect.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.usecase.GetUnlockedAchievementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the dedicated Achievements screen (Profile → Achievements button) - the full catalog
 * (locked and unlocked) lives in [com.suman.memoryarchitect.domain.achievements.AchievementCatalog]
 * itself, a static list the screen reads directly; this only needs to resolve which ids are
 * unlocked, the same [GetUnlockedAchievementsUseCase] [ProfileViewModel] already uses. */
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val getUnlockedAchievements: GetUnlockedAchievementsUseCase,
) : ViewModel() {

    private val _unlockedIds = MutableStateFlow<Set<AchievementId>?>(null)
    val unlockedIds: StateFlow<Set<AchievementId>?> = _unlockedIds.asStateFlow()

    init {
        viewModelScope.launch { _unlockedIds.value = getUnlockedAchievements() }
    }
}
