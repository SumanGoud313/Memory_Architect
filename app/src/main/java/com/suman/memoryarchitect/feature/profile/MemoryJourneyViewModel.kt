package com.suman.memoryarchitect.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.MemoryJourneyStanding
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.GetMemoryJourneyStandingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the dedicated Memory Journey screen (Profile -> Memory Journey button) - the full tier
 * catalog (locked and unlocked) lives in [com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog]
 * itself, a static list the screen reads directly; this only needs to resolve the player's current
 * standing, same shape [AchievementsViewModel] uses for [com.suman.memoryarchitect.domain.achievements.AchievementCatalog]. */
@HiltViewModel
class MemoryJourneyViewModel @Inject constructor(
    private val getStanding: GetMemoryJourneyStandingUseCase,
) : ViewModel() {

    private val _standing = MutableStateFlow<MemoryJourneyStanding?>(null)
    val standing: StateFlow<MemoryJourneyStanding?> = _standing.asStateFlow()

    init {
        viewModelScope.launch {
            _standing.value = (getStanding() as? Outcome.Success)?.data
        }
    }
}
