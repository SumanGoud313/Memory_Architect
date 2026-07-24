package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PurchaseResult
import com.suman.memoryarchitect.domain.model.SpinResult

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
     * specific purchase attempt, reused across any retry of *that* attempt. */
    suspend fun purchase(id: CosmeticId, purchaseNonce: String): Outcome<PurchaseResult>

    /** Applies optimistically even if the background remote sync fails - equip carries no
     * economic stake, unlike [purchase]/[spin]. */
    suspend fun equip(category: CosmeticCategory, id: CosmeticId): Outcome<Unit>

    /** Clears whatever's equipped for [category] - same optimistic-apply reasoning as [equip]. */
    suspend fun unequip(category: CosmeticCategory): Outcome<Unit>

    /** [spinNonce], same replay-guard role as [purchaseNonce] above. */
    suspend fun spin(spinNonce: String): Outcome<SpinResult>

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
