package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity

/** Price bands [ShopCatalog]'s launch catalog was priced from - data-driven the same way
 * [RewardRules]/[LevelCampaignRules] are, even though the catalog itself is hand-authored rather
 * than generated (each item's price sits inside its rarity's band, not derived from it at
 * runtime). */
data class ShopRules(
    val priceBandMin: Map<CosmeticRarity, Long> = mapOf(
        CosmeticRarity.COMMON to 150L,
        CosmeticRarity.RARE to 400L,
        CosmeticRarity.EPIC to 900L,
        CosmeticRarity.LEGENDARY to 2000L,
    ),
    val priceBandMax: Map<CosmeticRarity, Long> = mapOf(
        CosmeticRarity.COMMON to 300L,
        CosmeticRarity.RARE to 700L,
        CosmeticRarity.EPIC to 1500L,
        CosmeticRarity.LEGENDARY to 3500L,
    ),
) {
    companion object {
        val Default = ShopRules()
    }
}
