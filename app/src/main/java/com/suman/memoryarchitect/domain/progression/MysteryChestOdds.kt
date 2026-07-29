package com.suman.memoryarchitect.domain.progression

import kotlin.random.Random

/** One roll's outcome from opening a Mystery Chest - see
 * [com.suman.memoryarchitect.domain.model.InventoryItemKind]'s doc for why chests are earned/
 * stored well before this, the "Phase 2" piece that actually opens one. Coins-only today,
 * mirroring [SpinRules]'s own "balancing is a data change" philosophy - every value here stays
 * comfortably under `functions/src/index.ts`'s `MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE` (2,000/write),
 * so a real roll is never mistaken for a forged one server-side. */
data class MysteryChestReward(val coinsAwarded: Long)

object MysteryChestOdds {
    /** Mirrors [SpinRules.oddsByRarity]'s shape - a weighted table that must sum to 1.0 (verified
     * by `MysteryChestOddsTest`, same convention [SpinRules]'s own doc describes). */
    val rewardTable: List<Pair<MysteryChestReward, Double>> = listOf(
        MysteryChestReward(coinsAwarded = 50L) to 0.55,
        MysteryChestReward(coinsAwarded = 150L) to 0.30,
        MysteryChestReward(coinsAwarded = 400L) to 0.12,
        MysteryChestReward(coinsAwarded = 800L) to 0.03,
    )

    /** Client-rolled - unlike [com.suman.memoryarchitect.domain.progression.LuckySpinEngine]'s
     * roll (which needs server re-verification since it decides real cosmetic *ownership*), every
     * possible outcome here is just a small, bounded coin amount that
     * `functions/src/index.ts`'s existing `validateProfileWrite` per-write coin-gain cap already
     * defends against regardless of which table entry a forged client claims - so no dedicated
     * Cloud Function re-derivation is needed the way Lucky Spin has one. */
    fun roll(random: Random = Random.Default): MysteryChestReward {
        val target = random.nextDouble()
        var cumulative = 0.0
        for ((reward, odds) in rewardTable) {
            cumulative += odds
            if (target < cumulative) return reward
        }
        return rewardTable.last().first
    }
}
