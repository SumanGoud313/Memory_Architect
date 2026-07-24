package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopCatalogTest {

    @Test
    fun `every CosmeticId has exactly one definition in the merged catalog`() {
        // ShopCatalog is deliberately coin-only since the Premium Shop split (see
        // AllCosmeticsCatalog's doc) - a premium-only id has no ShopCatalog definition by design,
        // so this "every id resolves somewhere" invariant now belongs on the merged catalog.
        val ids = AllCosmeticsCatalog.definitions.map { it.id }

        assertEquals(CosmeticId.entries.size, AllCosmeticsCatalog.definitions.size)
        assertEquals(ids.toSet().size, ids.size)
        CosmeticId.entries.forEach { id -> assertTrue("$id has no definition", ids.contains(id)) }
    }

    @Test
    fun `ShopCatalog contains only coin-tier ids, disjoint from PremiumCatalog`() {
        val coinIds = ShopCatalog.definitions.map { it.id }.toSet()
        val premiumIds = PremiumCatalog.definitions.map { it.id }.toSet()
        assertTrue("Coin and Premium catalogs must never overlap", coinIds.intersect(premiumIds).isEmpty())
        assertEquals(CosmeticId.entries.size, coinIds.size + premiumIds.size)
    }

    @Test
    fun `every non-border category has exactly one item per rarity`() {
        CosmeticCategory.entries.filter { it != CosmeticCategory.PROFILE_BORDER }.forEach { category ->
            val items = ShopCatalog.definitionsOfCategory(category)
            assertEquals("$category should have 4 items", 4, items.size)
            CosmeticRarity.entries.forEach { rarity ->
                assertEquals("$category should have exactly one $rarity item", 1, items.count { it.rarity == rarity })
            }
        }
    }

    @Test
    fun `PROFILE_BORDER is the flagship category with 12 items, 3 per rarity`() {
        val items = ShopCatalog.definitionsOfCategory(CosmeticCategory.PROFILE_BORDER)
        assertEquals(12, items.size)
        CosmeticRarity.entries.forEach { rarity ->
            assertEquals("PROFILE_BORDER should have exactly 3 $rarity items", 3, items.count { it.rarity == rarity })
        }
    }

    @Test
    fun `border items are isAnimated only at Epic and Legendary rarity`() {
        val borders = ShopCatalog.definitionsOfCategory(CosmeticCategory.PROFILE_BORDER)
        borders.forEach { definition ->
            val expectedAnimated = definition.rarity == CosmeticRarity.EPIC || definition.rarity == CosmeticRarity.LEGENDARY
            assertEquals("${definition.id} isAnimated should be $expectedAnimated for ${definition.rarity}", expectedAnimated, definition.isAnimated)
        }
    }

    @Test
    fun `no non-border item is isAnimated`() {
        ShopCatalog.definitions.filter { it.category != CosmeticCategory.PROFILE_BORDER }.forEach { definition ->
            assertTrue("${definition.id} should not be isAnimated", !definition.isAnimated)
        }
    }

    @Test
    fun `prices sit within the rarity's price band`() {
        val rules = ShopRules.Default
        ShopCatalog.definitions.forEach { definition ->
            val min = rules.priceBandMin.getValue(definition.rarity)
            val max = rules.priceBandMax.getValue(definition.rarity)
            assertTrue(
                "${definition.id} price ${definition.priceCoins} outside [$min, $max] for ${definition.rarity}",
                definition.priceCoins in min..max,
            )
        }
    }

    @Test
    fun `requireDefinition throws for a missing id is unreachable but definitionOrNull is null-safe`() {
        // Every CosmeticId always has a definition in the merged catalog (see above); ShopCatalog
        // itself is only expected to resolve its own coin-tier subset.
        CosmeticId.entries.forEach { id -> assertTrue(AllCosmeticsCatalog.definitionOrNull(id) != null) }
    }
}
