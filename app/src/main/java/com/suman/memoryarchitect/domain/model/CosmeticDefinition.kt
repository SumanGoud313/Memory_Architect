package com.suman.memoryarchitect.domain.model

/** One entry in [com.suman.memoryarchitect.domain.progression.ShopCatalog]. [spinEligible] gates
 * whether [com.suman.memoryarchitect.domain.progression.LuckySpinEngine] may award this item -
 * `true` for every launch item today, but kept as a real field so a future item (e.g. a
 * purchase-only showcase piece) can opt out without a schema change. [isAnimated] marks a
 * "premium subtle animated version" (currently used by Epic/Legendary Premium Borders, see
 * [com.suman.memoryarchitect.ui.components.PremiumBorder]) - reusable by any future animated
 * cosmetic, not border-specific. */
data class CosmeticDefinition(
    val id: CosmeticId,
    val category: CosmeticCategory,
    val rarity: CosmeticRarity,
    val priceCoins: Long,
    val spinEligible: Boolean = true,
    val isAnimated: Boolean = false,
)
