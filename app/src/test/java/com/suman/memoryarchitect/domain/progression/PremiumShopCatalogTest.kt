package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.PremiumProductSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumShopCatalogTest {

    @Test
    fun `8 products total - remove_ads_lifetime plus 7 cosmetic bundles`() {
        assertEquals(8, PremiumShopCatalog.products.size)
        assertEquals(7, PremiumShopCatalog.cosmeticBundleProductIds.size)
        assertEquals(1, PremiumShopCatalog.products.count { it.source == PremiumProductSource.LEGACY_REMOVE_ADS })
        assertEquals(7, PremiumShopCatalog.products.count { it.source == PremiumProductSource.COSMETIC_BUNDLE })
    }

    @Test
    fun `cosmeticBundleProductIds excludes remove_ads_lifetime`() {
        assertTrue(PremiumShopCatalog.REMOVE_ADS_PRODUCT_ID !in PremiumShopCatalog.cosmeticBundleProductIds)
    }

    @Test
    fun `exactly one product is flagged Best Value`() {
        assertEquals(1, PremiumShopCatalog.products.count { it.isBestValue })
    }

    @Test
    fun `remove_ads_lifetime grants no cosmetics, every cosmetic bundle grants at least one`() {
        val removeAds = PremiumShopCatalog.requireProduct(PremiumShopCatalog.REMOVE_ADS_PRODUCT_ID)
        assertTrue(removeAds.grantedCosmeticIds.isEmpty())
        PremiumShopCatalog.cosmeticBundleProductIds.forEach { productId ->
            assertTrue(PremiumShopCatalog.requireProduct(productId).grantedCosmeticIds.isNotEmpty())
        }
    }

    @Test
    fun `Founder's Pack grants 8 items, Starter Bundle grants 5, each themed collection grants 8`() {
        assertEquals(8, PremiumShopCatalog.requireProduct(PremiumShopCatalog.FOUNDERS_PACK_PRODUCT_ID).grantedCosmeticIds.size)
        assertEquals(5, PremiumShopCatalog.requireProduct(PremiumShopCatalog.STARTER_BUNDLE_PRODUCT_ID).grantedCosmeticIds.size)
        listOf(
            PremiumShopCatalog.ROYAL_COLLECTION_PRODUCT_ID, PremiumShopCatalog.CYBER_COLLECTION_PRODUCT_ID,
            PremiumShopCatalog.SPACE_COLLECTION_PRODUCT_ID, PremiumShopCatalog.NATURE_COLLECTION_PRODUCT_ID,
            PremiumShopCatalog.LUXURY_COLLECTION_PRODUCT_ID,
        ).forEach { productId ->
            assertEquals(8, PremiumShopCatalog.requireProduct(productId).grantedCosmeticIds.size)
        }
    }

    @Test
    fun `every granted cosmetic id resolves in PremiumCatalog, never in ShopCatalog`() {
        val shopIds = ShopCatalog.definitions.map { it.id }.toSet()
        PremiumShopCatalog.products.flatMap { it.grantedCosmeticIds }.forEach { id ->
            assertNotNull("$id has no PremiumCatalog definition", PremiumCatalog.definitions.firstOrNull { it.id == id })
            assertTrue("$id must never be coin-purchasable", id !in shopIds)
        }
    }

    @Test
    fun `no cosmetic id is granted by more than one product`() {
        val allGranted = PremiumShopCatalog.products.flatMap { it.grantedCosmeticIds }
        assertEquals(allGranted.toSet().size, allGranted.size)
    }

    @Test
    fun `productOrNull is null-safe for an unknown id, requireProduct throws`() {
        assertNull(PremiumShopCatalog.productOrNull("not_a_real_product"))
        try {
            PremiumShopCatalog.requireProduct("not_a_real_product")
            throw AssertionError("expected NoSuchElementException")
        } catch (expected: NoSuchElementException) {
            // expected
        }
    }
}
