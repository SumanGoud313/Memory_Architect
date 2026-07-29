package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import com.suman.memoryarchitect.domain.repository.SpinSource

/** The server-authoritative half of [ShopRepositoryImpl] - mirrors [ProgressionRemoteSource]'s
 * split exactly: [MockBackendShopRemoteSource] (dev-only, no per-player identity) and
 * [FirestoreShopRemoteSource] (real, per-player, transactional), picked per-call by
 * [ShopRepositoryImpl.activeRemoteSource]. */
interface ShopRemoteSource {
    suspend fun getOwnedSkus(): Set<String>
    suspend fun getEquipped(): Map<String, String>

    /** Local-only-cache-backing read - see [com.suman.memoryarchitect.domain.repository.ShopRepository.getLuckySpinState]'s doc. */
    suspend fun getLuckySpinState(): LuckySpinState

    /** Throws [AlreadyOwnedCosmeticException]/[InsufficientCoinsException]/[DuplicatePurchaseException]/
     * [com.suman.memoryarchitect.data.repository.InsufficientInventoryException] (the last only
     * when [useDiscountCoupon] is true and none is owned) on the expected failure conditions -
     * [ShopRepositoryImpl] maps each to a distinct [com.suman.memoryarchitect.domain.model.AppError],
     * same pattern [ProgressionRepositoryImpl.claimDailyReward] already uses for
     * [DailyRewardAlreadyClaimedException]. [useDiscountCoupon] atomically consumes one
     * [com.suman.memoryarchitect.domain.model.InventoryItemKind.DISCOUNT_COUPON] and applies
     * [com.suman.memoryarchitect.domain.progression.DiscountCouponRules] in the same transaction as
     * the purchase itself - never a separate write, so a purchase can't succeed at full price while
     * still consuming the coupon (or vice versa). */
    suspend fun purchase(id: CosmeticId, purchaseNonce: String, useDiscountCoupon: Boolean = false): Pair<PlayerProfile, Set<String>>

    /** [request] is the client-computed [com.suman.memoryarchitect.domain.progression.LuckySpinEngine]
     * roll, already resolved to one of its two shapes - see [SpinRequest]'s doc. Only the
     * [SpinRequest.Cosmetic] branch re-verifies an ownership transition server-side ([chosenId]'s
     * new-grant-vs-duplicate); a [SpinRequest.Coins] branch is just a bounded coin credit, no
     * cosmetics-collection write (see [com.suman.memoryarchitect.domain.progression.SpinRules]'s
     * doc for why that asymmetry is safe). [source] decides which allowance this spin spends -
     * see [SpinSource]'s doc - throwing [SpinNotAvailableException] for [SpinSource.FREE]/
     * [SpinSource.AD] if that day's allowance is already spent, or
     * [com.suman.memoryarchitect.data.repository.InsufficientInventoryException] for
     * [SpinSource.TICKET] with none owned. [spinNonce] is the same replay-guard role
     * [purchaseNonce] plays above. [todayEpochDay] is caller-computed (same convention
     * [com.suman.memoryarchitect.domain.repository.ProgressionRepository.claimDailyReward] already
     * uses) rather than each remote source deriving "today" independently - [MockBackendShopRemoteSource]'s
     * dev server has no device clock of its own to do so. */
    suspend fun spin(request: SpinRequest, spinNonce: String, source: SpinSource, todayEpochDay: Long): SpinOutcome

    suspend fun equip(category: CosmeticCategory, id: CosmeticId): Map<String, String>

    /** Clears whatever's equipped for [category] - a no-op (returns the map unchanged) if nothing
     * was equipped there to begin with. */
    suspend fun unequip(category: CosmeticCategory): Map<String, String>
}

/** The two shapes a client-computed [com.suman.memoryarchitect.domain.progression.LuckySpinEngine]
 * roll can take - mirrors [com.suman.memoryarchitect.domain.model.SpinRewardKind] one level down,
 * at the remote-source boundary. */
sealed interface SpinRequest {
    data class Coins(val amount: Long) : SpinRequest
    data class Cosmetic(val id: CosmeticId, val rarity: CosmeticRarity) : SpinRequest
}

data class SpinOutcome(
    val reward: SpinRewardKind,
    val wasDuplicate: Boolean,
    val coinsRefunded: Long,
    val profile: PlayerProfile,
    val ownedSkus: Set<String>,
    val spinState: LuckySpinState,
)
