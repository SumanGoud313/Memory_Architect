package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.PlayerProfile

/** The server-authoritative half of [ShopRepositoryImpl] - mirrors [ProgressionRemoteSource]'s
 * split exactly: [MockBackendShopRemoteSource] (dev-only, no per-player identity) and
 * [FirestoreShopRemoteSource] (real, per-player, transactional), picked per-call by
 * [ShopRepositoryImpl.activeRemoteSource]. */
interface ShopRemoteSource {
    suspend fun getOwnedSkus(): Set<String>
    suspend fun getEquipped(): Map<String, String>

    /** Throws [AlreadyOwnedCosmeticException]/[InsufficientCoinsException]/[DuplicatePurchaseException]
     * on the expected failure conditions - [ShopRepositoryImpl] maps each to a distinct
     * [com.suman.memoryarchitect.domain.model.AppError], same pattern
     * [ProgressionRepositoryImpl.claimDailyReward] already uses for [DailyRewardAlreadyClaimedException]. */
    suspend fun purchase(id: CosmeticId, purchaseNonce: String): Pair<PlayerProfile, Set<String>>

    /** Re-verifies the ownership transition (new grant vs. duplicate) server-side against
     * [chosenId] - see [ShopRepository]'s doc for why the roll itself stays client-trusted. */
    suspend fun spin(chosenId: CosmeticId, rarity: CosmeticRarity, spinNonce: String): SpinOutcome

    suspend fun equip(category: CosmeticCategory, id: CosmeticId): Map<String, String>

    /** Clears whatever's equipped for [category] - a no-op (returns the map unchanged) if nothing
     * was equipped there to begin with. */
    suspend fun unequip(category: CosmeticCategory): Map<String, String>
}

data class SpinOutcome(
    val awardedId: CosmeticId,
    val wasDuplicate: Boolean,
    val coinsRefunded: Long,
    val profile: PlayerProfile,
    val ownedSkus: Set<String>,
)
