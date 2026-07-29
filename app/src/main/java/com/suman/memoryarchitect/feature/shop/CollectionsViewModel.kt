package com.suman.memoryarchitect.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.PermanentFreeCosmetics
import com.suman.memoryarchitect.domain.usecase.EquipCosmeticUseCase
import com.suman.memoryarchitect.domain.usecase.GetEquippedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetFavoriteCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetOwnedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetRecentlyUsedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.RecordMissionEventUseCase
import com.suman.memoryarchitect.domain.usecase.ToggleFavoriteCosmeticUseCase
import com.suman.memoryarchitect.domain.usecase.UnequipCosmeticUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Collections gallery (Profile -> Collections) - the full catalog lives in
 * [com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog] itself, read directly by the
 * screen; this only resolves owned/equipped/favorite/recently-used state, same shape
 * [RewardsViewModel] uses for [RewardCatalog]. */
@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val getOwnedCosmetics: GetOwnedCosmeticsUseCase,
    private val getEquippedCosmetics: GetEquippedCosmeticsUseCase,
    private val equipCosmetic: EquipCosmeticUseCase,
    private val unequipCosmetic: UnequipCosmeticUseCase,
    private val getFavoriteCosmetics: GetFavoriteCosmeticsUseCase,
    private val getRecentlyUsedCosmetics: GetRecentlyUsedCosmeticsUseCase,
    private val toggleFavoriteCosmetic: ToggleFavoriteCosmeticUseCase,
    private val recordMissionEvent: RecordMissionEventUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectionsUiState>(CollectionsUiState.Loading)
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            coroutineScope {
                val owned = getOwnedCosmetics()
                val equipped = getEquippedCosmetics()
                val favorites = getFavoriteCosmetics()
                val recentlyUsed = getRecentlyUsedCosmetics()
                _uiState.value = CollectionsUiState.Content(owned, equipped, favoriteIds = favorites, recentlyUsedIds = recentlyUsed)
            }
        }
    }

    fun selectTab(tab: CollectionsTab) {
        val current = _uiState.value as? CollectionsUiState.Content ?: return
        _uiState.value = current.copy(selectedTab = tab)
    }

    /** Optimistic local flip (matches [equip]/[unequip]'s own "update state, then let the async
     * call catch up" shape) - see [ShopRepository.toggleFavorite]'s doc for why this is a
     * local-only, no-Firestore-round-trip action, so there's no real failure mode worth reverting
     * the flip for. */
    fun toggleFavorite(id: CosmeticId) {
        val current = _uiState.value as? CollectionsUiState.Content ?: return
        if (id !in current.ownedIds) return
        val isFavorited = id in current.favoriteIds
        _uiState.value = current.copy(favoriteIds = if (isFavorited) current.favoriteIds - id else current.favoriteIds + id)
        viewModelScope.launch { toggleFavoriteCosmetic(id) }
    }

    fun equip(category: CosmeticCategory, id: CosmeticId) {
        val current = _uiState.value as? CollectionsUiState.Content ?: return
        if (current.equippingId != null || id !in current.ownedIds) return
        _uiState.value = current.copy(equippingId = id)
        viewModelScope.launch {
            val outcome = equipCosmetic(category, id)
            val latest = _uiState.value as? CollectionsUiState.Content ?: return@launch
            _uiState.value = when (outcome) {
                is Outcome.Success -> {
                    recordMissionEvent(MissionEvent.CosmeticEquipped)
                    latest.copy(
                        equipped = latest.equipped + (category to id),
                        equippingId = null,
                        recentlyUsedIds = runCatching { getRecentlyUsedCosmetics() }.getOrDefault(latest.recentlyUsedIds),
                    )
                }
                is Outcome.Error -> latest.copy(equippingId = null)
            }
        }
    }

    fun unequip(category: CosmeticCategory) {
        val current = _uiState.value as? CollectionsUiState.Content ?: return
        val equippedId = current.equipped[category] ?: return
        if (current.equippingId != null) return
        _uiState.value = current.copy(equippingId = equippedId)
        viewModelScope.launch {
            val outcome = unequipCosmetic(category)
            val latest = _uiState.value as? CollectionsUiState.Content ?: return@launch
            _uiState.value = when (outcome) {
                // BACKGROUND_THEME/PROFILE_BORDER fall straight back to their permanent free
                // default (see PermanentFreeCosmetics/ShopRepositoryImpl.unequip's doc) rather than
                // showing "nothing equipped" here and only correcting itself on the next cold
                // start's fresh read.
                is Outcome.Success -> {
                    val fallback = PermanentFreeCosmetics.defaultEquippedByCategory[category]
                    latest.copy(
                        equipped = if (fallback != null) latest.equipped + (category to fallback) else latest.equipped - category,
                        equippingId = null,
                    )
                }
                is Outcome.Error -> latest.copy(equippingId = null)
            }
        }
    }
}
