package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.progression.ShopCatalog
import javax.inject.Inject

/** Pure, no I/O - the catalog is static and client-known (see [ShopCatalog]'s doc). Kept as a use
 * case anyway (rather than every ViewModel reading [ShopCatalog] directly) for symmetry with the
 * rest of this feature's use-case layer and to give tests one seam to substitute if the catalog
 * ever needs to be swapped. */
class GetShopCatalogUseCase @Inject constructor() {
    operator fun invoke(): List<CosmeticDefinition> = ShopCatalog.definitions
}
