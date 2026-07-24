package com.suman.memoryarchitect.feature.shop

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId

/** Which gallery view [CollectionsScreen] shows - purely UI state, never persisted (unlike the
 * underlying favorite flag/recency timestamp it filters by, both real per-item Room state). */
enum class CollectionsTab { ALL, FAVORITES, RECENT }

sealed interface CollectionsUiState {
    data object Loading : CollectionsUiState
    data class Content(
        val ownedIds: Set<CosmeticId>,
        val equipped: Map<CosmeticCategory, CosmeticId>,
        val equippingId: CosmeticId? = null,
        val selectedTab: CollectionsTab = CollectionsTab.ALL,
        val favoriteIds: Set<CosmeticId> = emptySet(),
        val recentlyUsedIds: List<CosmeticId> = emptyList(),
    ) : CollectionsUiState
}
