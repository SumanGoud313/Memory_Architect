package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.model.CosmeticId

/**
 * The merged view of [ShopCatalog] (coin) + [PremiumCatalog] (real money) - the one thing every
 * *display/rendering* call site should resolve an owned or equipped [CosmeticId] through
 * (`CosmeticId.category()`, [com.suman.memoryarchitect.ui.components.PremiumBorder]'s equipped-id
 * lookup, Collections' "what categories/items exist at all" iteration). An id only needs to exist
 * in *one* of the two source catalogs to resolve here - this object never grants, prices, or
 * distinguishes acquisition method, it only answers "what is this id."
 *
 * Deliberately **not** used by the coin purchase/spin path
 * ([com.suman.memoryarchitect.data.repository.FirestoreShopRemoteSource]'s `purchase`/`spin`,
 * [com.suman.memoryarchitect.domain.usecase.GetShopCatalogUseCase],
 * [LuckySpinEngine]) or the Shop's Coin tab - those stay scoped to [ShopCatalog] alone, by
 * construction, so a premium-only id can never be coin-purchased or spin-awarded.
 */
object AllCosmeticsCatalog {
    val definitions: List<CosmeticDefinition> = ShopCatalog.definitions + PremiumCatalog.definitions

    private val byId: Map<CosmeticId, CosmeticDefinition> = definitions.associateBy { it.id }

    fun definitionsOfCategory(category: CosmeticCategory): List<CosmeticDefinition> =
        definitions.filter { it.category == category }

    fun requireDefinition(id: CosmeticId): CosmeticDefinition =
        byId[id] ?: throw NoSuchElementException("No CosmeticDefinition for $id")

    fun definitionOrNull(id: CosmeticId): CosmeticDefinition? = byId[id]
}
