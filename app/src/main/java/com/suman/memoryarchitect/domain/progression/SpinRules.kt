package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity

/** Data-driven tuning for [LuckySpinEngine] - mirrors [RewardRules]'s "balancing is a data
 * change" philosophy. [oddsByRarity] must sum to 1.0 (validated by [ShopCatalogTest]-style unit
 * tests, not enforced at construction, matching how [DailyRewardCatalog]'s table isn't validated
 * at construction either). */
data class SpinRules(
    val spinCostCoins: Long = 150L,
    val oddsByRarity: Map<CosmeticRarity, Double> = mapOf(
        CosmeticRarity.COMMON to 0.60,
        CosmeticRarity.RARE to 0.28,
        CosmeticRarity.EPIC to 0.10,
        CosmeticRarity.LEGENDARY to 0.02,
    ),
    /** A roll that lands on an already-owned item converts to a coin refund of `priceCoins *
     * duplicateRefundFraction` instead of a no-op - see [LuckySpinEngine.spin]'s doc for why. */
    val duplicateRefundFraction: Double = 0.5,
) {
    companion object {
        val Default = SpinRules()
    }
}
