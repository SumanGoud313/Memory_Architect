package com.suman.memoryarchitect.core.cosmetics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.usecase.GetEquippedCosmeticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mounted exactly once, at the app root (`ui/navigation/MemoryArchitectNavHost.kt`, the one
 * composable already proven to live for the whole app session - see `MusicPauseOnBackground`
 * there for precedent) - primes [EquippedCosmeticsStore] from persisted state on cold start, then
 * gets out of the way. Every screen reads the store's [StateFlow] via
 * [com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics], not this ViewModel directly;
 * this exists purely because a `@Composable` can't reach a plain `@Singleton` without either a
 * ViewModel or a Hilt `EntryPoint`, and every other cross-cutting concern in this app is already
 * ViewModel-backed. */
@HiltViewModel
class RootCosmeticsViewModel @Inject constructor(
    private val store: EquippedCosmeticsStore,
    private val getEquippedCosmetics: GetEquippedCosmeticsUseCase,
) : ViewModel() {

    val equipped: StateFlow<Map<CosmeticCategory, CosmeticId>> = store.equipped

    init {
        viewModelScope.launch { store.setAll(getEquippedCosmetics()) }
    }
}
