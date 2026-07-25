package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.Outcome

/** The permanent home for every earned consumable - see
 * [com.suman.memoryarchitect.domain.model.InventoryItemKind]'s doc. Server-authoritative, online-
 * first with a local cache fallback, same policy [ProgressionRepository.getProfile] already uses. */
interface InventoryRepository {
    suspend fun getInventory(): Outcome<Inventory>

    /** Spends [quantity] of [kind] - e.g. a Hint Token pre-filling one rewarded-hint slot without
     * an ad. Server-mirrored so a spend can't be forged/duplicated, same reasoning as a coin
     * purchase in [com.suman.memoryarchitect.domain.repository.ShopRepository]. */
    suspend fun consumeItem(kind: InventoryItemKind, quantity: Int = 1): Outcome<Inventory>

    /** Overwrites the local cache to match [inventory] exactly - called right after some other
     * call ([MissionRepository.claimMissionReward], a future Phase 2 chest-open) already wrote the
     * authoritative state server-side, so the UI reflects the grant without a redundant fetch. Not
     * itself a network call. */
    suspend fun cacheInventory(inventory: Inventory)
}
