package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.model.MysteryChestAdClaimResult
import com.suman.memoryarchitect.domain.model.MysteryChestAdState
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PurchaseResult
import com.suman.memoryarchitect.domain.model.SpinResult

/** Which of Lucky Spin's three independent allowances a given [ShopRepository.spin] call is
 * spending - see that method's doc for what each one checks/stamps. */
enum class SpinSource { FREE, AD, TICKET }

/**
 * The Point Shop's spend/ownership half - deliberately a sibling of [ProgressionRepository], not
 * an extension of it (see the Points Economy plan). Shares the same physical coin-balance cache
 * row via [com.suman.memoryarchitect.core.database.PlayerProgressDao] rather than depending on
 * [ProgressionRepository] itself, avoiding an awkward cross-repository coupling.
 */
interface ShopRepository {
    /** Local-only read (Room cache, synced from whichever remote source is active) - same
     * reasoning as [ProgressionRepository.getUnlockedRewardIds]. */
    suspend fun getOwnedCosmeticIds(): Set<CosmeticId>

    /** Local-only read, one entry per [CosmeticCategory] that has something equipped. */
    suspend fun getEquippedCosmetics(): Map<CosmeticCategory, CosmeticId>

    /** Fails cleanly (no local grant) on any error - not the optimistic-then-pending-sync pattern
     * [ProgressionRepository.submitScore] uses, since a purchase is a real economic transaction
     * that must never be double-credited by an offline retry. [purchaseNonce] identifies this
     * specific purchase attempt, reused across any retry of *that* attempt. [useDiscountCoupon]
     * atomically spends one owned Discount Coupon for a reduced price - see
     * [com.suman.memoryarchitect.domain.progression.DiscountCouponRules]. */
    suspend fun purchase(id: CosmeticId, purchaseNonce: String, useDiscountCoupon: Boolean = false): Outcome<PurchaseResult>

    /** Applies optimistically even if the background remote sync fails - equip carries no
     * economic stake, unlike [purchase]/[spin]. */
    suspend fun equip(category: CosmeticCategory, id: CosmeticId): Outcome<Unit>

    /** Clears whatever's equipped for [category] - same optimistic-apply reasoning as [equip]. */
    suspend fun unequip(category: CosmeticCategory): Outcome<Unit>

    /** Local-only read (Room cache, synced from whichever remote source is active) - Lucky Spin's
     * own daily-gate/first-spin state, kept deliberately separate from [PlayerProfile] (see
     * [LuckySpinState]'s doc). Used to compute the adaptive Spin button's Free/Watch Ad/Use Ticket
     * priority before ever calling [spin]. */
    suspend fun getLuckySpinState(): LuckySpinState

    /** Spins are free - no coin cost - gated instead by [source]: [SpinSource.FREE] and
     * [SpinSource.AD] each allow exactly one spin per day (re-verified server-side against
     * [LuckySpinState.lastFreeSpinEpochDay]/[LuckySpinState.lastAdSpinEpochDay], the same
     * "recognize, don't just trust" posture [ProgressionRepository.claimDailyReward] already has);
     * [SpinSource.TICKET] atomically consumes one owned
     * [com.suman.memoryarchitect.domain.model.InventoryItemKind.LUCKY_SPIN_TICKET] instead and is
     * never subject to the daily gate, so a held ticket is always an extra spin on top of
     * whichever of the two daily allowances remain. [spinNonce] is the same replay-guard role
     * [purchaseNonce] plays above. The very first spin this player ever completes (by any
     * [source]) always resolves to a [com.suman.memoryarchitect.domain.model.SpinRewardKind.Cosmetic] -
     * see [LuckySpinState.hasEverSpun]'s doc. */
    suspend fun spin(spinNonce: String, source: SpinSource): Outcome<SpinResult>

    /** Local-only read (Room cache, synced from whichever remote source is active) - the ad-gated
     * Mystery Chest claim's own daily-gate state, see
     * [com.suman.memoryarchitect.domain.model.MysteryChestAdState]'s doc. Used to compute how many
     * of today's claims remain before ever calling [claimAdMysteryChest]. */
    suspend fun getMysteryChestAdState(): MysteryChestAdState

    /** Grants one [com.suman.memoryarchitect.domain.model.InventoryItemKind.MYSTERY_CHEST] to
     * Inventory once a rewarded ad fully resolves - gated at
     * [com.suman.memoryarchitect.domain.progression.MysteryChestAdRules.maxClaimsPerDay] per day,
     * re-verified server-side against [MysteryChestAdState] rather than trusted from the caller
     * (same "recognize, don't just trust" posture [spin]'s [SpinSource.FREE]/[SpinSource.AD] gates
     * already have). No coin cost, no free/ticket alternative - watch-ad-only by design (see
     * [com.suman.memoryarchitect.domain.progression.MysteryChestAdRules]'s doc). [claimNonce] is
     * the same replay-guard role [spinNonce] plays above. */
    suspend fun claimAdMysteryChest(claimNonce: String): Outcome<MysteryChestAdClaimResult>

    /** Local-only preference, never synced to any remote source - deliberately simpler than
     * [equip]/[unequip] (no economic stake, no cross-device sync need strong enough to justify a
     * Firestore round-trip for a "star this item" toggle). Flips [id]'s favorite flag; a no-op if
     * [id] isn't owned. */
    suspend fun toggleFavorite(id: CosmeticId)

    /** Local-only read - every owned id currently favorited. */
    suspend fun getFavoriteCosmeticIds(): Set<CosmeticId>

    /** Local-only read, most-recently-equipped first - a browsing convenience, not a purchase
     * record, so (unlike [getOwnedCosmeticIds]) it's fine for this to reset on reinstall. */
    suspend fun getRecentlyUsedCosmeticIds(limit: Int = 10): List<CosmeticId>
}
