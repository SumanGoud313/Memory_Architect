package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.MysteryChestReward

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
     * call ([MissionRepository.claimMissionReward], the chest-open below) already wrote the
     * authoritative state server-side, so the UI reflects the grant without a redundant fetch. Not
     * itself a network call. */
    suspend fun cacheInventory(inventory: Inventory)

    /** The "Phase 2" piece [cacheInventory]'s doc always pointed at - consumes one
     * [InventoryItemKind.MYSTERY_CHEST] for a random coin reward, see
     * [com.suman.memoryarchitect.domain.progression.MysteryChestOdds]. Fails with no grant and no
     * consumption on insufficient balance, same as [consumeItem]. */
    suspend fun openMysteryChest(): Outcome<MysteryChestReward>

    /** Consumes one [InventoryItemKind.XP_BOOST] for an immediate flat XP grant, see
     * [com.suman.memoryarchitect.domain.progression.XpBoostRules]. Returns the amount actually
     * granted. Fails with no grant and no consumption on insufficient balance, same as
     * [consumeItem]. */
    suspend fun applyXpBoost(): Outcome<Long>
}
