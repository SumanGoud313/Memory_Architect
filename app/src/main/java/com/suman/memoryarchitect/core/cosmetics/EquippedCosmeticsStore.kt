package com.suman.memoryarchitect.core.cosmetics

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide, in-memory source of truth for "what's currently equipped" - the one thing every
 * button/surface that reads [com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics] is
 * ultimately backed by. A plain `@Singleton` (not a Room table itself - that's
 * [com.suman.memoryarchitect.core.database.EquippedCosmeticDao], the persisted copy this mirrors)
 * so it's reachable from both [com.suman.memoryarchitect.data.repository.ShopRepositoryImpl]
 * (which pushes into it on every successful equip/unequip) and [RootCosmeticsViewModel] (which
 * reads it to feed the `CompositionLocalProvider` at the app root) without either depending on the
 * other - Hilt guarantees both see the exact same instance.
 */
@Singleton
class EquippedCosmeticsStore @Inject constructor() {
    private val _equipped = MutableStateFlow<Map<CosmeticCategory, CosmeticId>>(emptyMap())
    val equipped: StateFlow<Map<CosmeticCategory, CosmeticId>> = _equipped.asStateFlow()

    /** Called once, at app start, with whatever's already persisted - see [RootCosmeticsViewModel]. */
    fun setAll(equipped: Map<CosmeticCategory, CosmeticId>) {
        _equipped.value = equipped
    }

    /** [id] `null` clears [category] (unequip) - anything else sets/replaces it. */
    fun setEquipped(category: CosmeticCategory, id: CosmeticId?) {
        _equipped.value = if (id != null) _equipped.value + (category to id) else _equipped.value - category
    }
}
