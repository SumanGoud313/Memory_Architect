package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import kotlin.random.Random

/** Result of one client-computed roll, before any server round-trip. [ShopRepositoryImpl]
 * re-verifies only the *ownership transition* (new grant vs. duplicate) server-side - the rarity/
 * item roll itself is client-trusted, a deliberate trade-off proportionate to a cosmetic-only
 * economy with zero competitive stake (see the Points Economy plan's "out of scope" notes). */
data class SpinRoll(val id: CosmeticId, val rarity: CosmeticRarity)

/**
 * Pure, injectable-[Random] engine backing the Lucky Spin - no pity/streak system (a roll is
 * always independent), no I/O. [rollRarity] picks a rarity by cumulative weight from
 * [SpinRules.oddsByRarity], then [pickItem] picks uniformly among that rarity's
 * [com.suman.memoryarchitect.domain.model.CosmeticDefinition.spinEligible] items in [ShopCatalog].
 */
class LuckySpinEngine(
    private val rules: SpinRules = SpinRules.Default,
    private val random: Random = Random.Default,
) {
    fun rollRarity(): CosmeticRarity {
        val roll = random.nextDouble()
        var cumulative = 0.0
        for ((rarity, odds) in rules.oddsByRarity) {
            cumulative += odds
            if (roll < cumulative) return rarity
        }
        // Floating-point rounding at the tail end of the cumulative sum - fall back to the last
        // configured rarity rather than throwing, so a spin always resolves to something.
        return rules.oddsByRarity.keys.last()
    }

    fun pickItem(rarity: CosmeticRarity): CosmeticId {
        val pool = ShopCatalog.definitionsOfRarity(rarity).filter { it.spinEligible }
        check(pool.isNotEmpty()) { "No spin-eligible items for rarity $rarity" }
        return pool[random.nextInt(pool.size)].id
    }

    fun spin(): SpinRoll {
        val rarity = rollRarity()
        return SpinRoll(pickItem(rarity), rarity)
    }
}
