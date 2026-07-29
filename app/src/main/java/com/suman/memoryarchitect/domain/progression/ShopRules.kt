package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity

/** Price bands [ShopCatalog]'s launch catalog was priced from - data-driven the same way
 * [RewardRules]/[LevelCampaignRules] are, even though the catalog itself is hand-authored rather
 * than generated (each item's price sits inside its rarity's band, not derived from it at
 * runtime). */
data class ShopRules(
    val priceBandMin: Map<CosmeticRarity, Long> = mapOf(
        CosmeticRarity.COMMON to 500L,
        CosmeticRarity.RARE to 900L,
        CosmeticRarity.EPIC to 1800L,
        CosmeticRarity.LEGENDARY to 3500L,
    ),
    val priceBandMax: Map<CosmeticRarity, Long> = mapOf(
        CosmeticRarity.COMMON to 800L,
        CosmeticRarity.RARE to 1400L,
        CosmeticRarity.EPIC to 2600L,
        CosmeticRarity.LEGENDARY to 5500L,
    ),
) {
    companion object {
        val Default = ShopRules()
    }
}
