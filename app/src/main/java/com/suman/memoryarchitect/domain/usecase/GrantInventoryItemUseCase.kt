package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.repository.InventoryRepository
import javax.inject.Inject

/** Refreshes the local Inventory cache to match a grant that already happened server-side (a
 * mission claim, a future Phase 2 chest-open) - there's no standalone "grant" network call today,
 * since every current grant path already returns its own updated [Inventory] from the same
 * response its claim/open call produces. This exists so those callers (and later ones) never have
 * to reach into [com.suman.memoryarchitect.core.database.InventoryItemDao] directly. */
class GrantInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(inventory: Inventory) = repository.cacheInventory(inventory)
}
