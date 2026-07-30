package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllCosmeticsCatalogTest {

    @Test
    fun `merges ShopCatalog and PremiumCatalog with no overlap`() {
        assertEquals(ShopCatalog.definitions.size + PremiumCatalog.definitions.size, AllCosmeticsCatalog.definitions.size)
        val ids = AllCosmeticsCatalog.definitions.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun `every CosmeticId resolves via requireDefinition and definitionOrNull`() {
        // MATERIAL_* ids are the one deliberate exception: the OBJECT_MATERIAL category (and its
        // premium-bundle grants) was removed from the shop entirely after a device-specific
        // rendering bug (see GameplayScenePanel.kt's distractorDesaturation doc). The CosmeticId
        // enum constants themselves stay defined - not deleted - since a real player profile may
        // still reference one as already-owned/equipped; only their catalog *definitions* are gone.
        CosmeticId.entries.filterNot { it.name.startsWith("MATERIAL_") }.forEach { id ->
            assertEquals(id, AllCosmeticsCatalog.requireDefinition(id).id)
            assertTrue(AllCosmeticsCatalog.definitionOrNull(id) != null)
        }
    }

    @Test
    fun `definitionsOfCategory only ever returns items of that category`() {
        com.suman.memoryarchitect.domain.model.CosmeticCategory.entries.forEach { category ->
            AllCosmeticsCatalog.definitionsOfCategory(category).forEach { definition ->
                assertEquals(category, definition.category)
            }
        }
    }

}
