package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionReward
import kotlin.random.Random

/**
 * The bonus granted once every currently-active mission in a period's set is claimed - on top of,
 * never instead of, each mission's own [MissionReward] (never for completing only one or two of
 * the three). Mirrored in `mock-backend/missions.js`, same "keep Kotlin/JS in sync" convention
 * [MissionCatalog] itself already documents. No entry for [MissionPeriod.EVENT] - a live event's
 * short, rare window doesn't get its own completion bonus.
 *
 * [coinRange] is rolled client-side (see [roll]) rather than a fixed amount, then re-verified only
 * for *plausibility* server-side (falls within this same range) - the same "client rolls a bounded
 * amount, server bounds-checks rather than re-deriving" trust level
 * [com.suman.memoryarchitect.domain.progression.MysteryChestOdds] already documents, since every
 * range here stays comfortably under `functions/src/index.ts`'s `MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE`
 * (2,000/write). [inventoryGrants] stays fixed (never randomized) and is always applied from the
 * server's own copy of this table, never trusted from the client.
 */
object MissionCategoryBonusCatalog {

    data class CategoryBonus(
        val coinRange: LongRange,
        val xp: Long = 0L,
        val inventoryGrants: Map<InventoryItemKind, Int> = emptyMap(),
    )

    private val bonusByPeriod: Map<MissionPeriod, CategoryBonus> = mapOf(
        MissionPeriod.DAILY to CategoryBonus(coinRange = 100L..150L),
        MissionPeriod.WEEKLY to CategoryBonus(
            coinRange = 200L..250L,
            xp = 40L,
            inventoryGrants = mapOf(InventoryItemKind.LUCKY_SPIN_TICKET to 1),
        ),
        MissionPeriod.MONTHLY to CategoryBonus(
            coinRange = 350L..450L,
            inventoryGrants = mapOf(
                InventoryItemKind.LUCKY_SPIN_TICKET to 1,
                InventoryItemKind.REDO_TOKEN to 1,
                InventoryItemKind.HINT_TOKEN to 1,
            ),
        ),
    )

    fun forPeriod(period: MissionPeriod): CategoryBonus? = bonusByPeriod[period]

    /** The client-side roll - a random coin amount within [CategoryBonus.coinRange], paired with
     * that period's fixed [CategoryBonus.xp]/[CategoryBonus.inventoryGrants]. `null` for
     * [MissionPeriod.EVENT]. */
    fun roll(period: MissionPeriod, random: Random = Random.Default): MissionReward? {
        val bonus = forPeriod(period) ?: return null
        val coins = random.nextLong(bonus.coinRange.first, bonus.coinRange.last + 1)
        return MissionReward(coins = coins, xp = bonus.xp, inventoryGrants = bonus.inventoryGrants)
    }
}
