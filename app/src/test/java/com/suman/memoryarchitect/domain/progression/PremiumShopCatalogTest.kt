package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.BillingEntitlementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumShopCatalogTest {

    @Test
    fun `8 products total - remove_ads_lifetime plus 7 cosmetic collections`() {
        assertEquals(8, PremiumShopCatalog.products.size)
        assertEquals(7, PremiumShopCatalog.cosmeticCollectionProductIds.size)
        assertEquals(1, PremiumShopCatalog.products.count { it.entitlement == BillingEntitlementKind.REMOVE_ADS })
        assertEquals(7, PremiumShopCatalog.products.count { it.entitlement == BillingEntitlementKind.COSMETIC_COLLECTION })
    }

    @Test
    fun `cosmeticCollectionProductIds excludes remove_ads_lifetime`() {
        assertTrue(PremiumShopCatalog.REMOVE_ADS_PRODUCT_ID !in PremiumShopCatalog.cosmeticCollectionProductIds)
    }

    @Test
    fun `exactly one product is flagged Best Value`() {
        assertEquals(1, PremiumShopCatalog.products.count { it.isBestValue })
    }

    @Test
    fun `remove_ads_lifetime grants no cosmetics, every cosmetic collection grants at least one`() {
        val removeAds = PremiumShopCatalog.requireProduct(PremiumShopCatalog.REMOVE_ADS_PRODUCT_ID)
        assertTrue(removeAds.grantedCosmeticIds.isEmpty())
        PremiumShopCatalog.cosmeticCollectionProductIds.forEach { productId ->
            assertTrue(PremiumShopCatalog.requireProduct(productId).grantedCosmeticIds.isNotEmpty())
        }
    }

    @Test
    fun `Founder's Pack grants 10 items, Starter Bundle grants 7, each themed collection grants 10`() {
        // Each product's original category set (8/5/8) plus one ROOM_SKIN + one OBJECT_MATERIAL id.
        assertEquals(10, PremiumShopCatalog.requireProduct(PremiumShopCatalog.FOUNDERS_PACK_PRODUCT_ID).grantedCosmeticIds.size)
        assertEquals(7, PremiumShopCatalog.requireProduct(PremiumShopCatalog.STARTER_BUNDLE_PRODUCT_ID).grantedCosmeticIds.size)
        listOf(
            PremiumShopCatalog.ROYAL_COLLECTION_PRODUCT_ID, PremiumShopCatalog.CYBER_COLLECTION_PRODUCT_ID,
            PremiumShopCatalog.SPACE_COLLECTION_PRODUCT_ID, PremiumShopCatalog.NATURE_COLLECTION_PRODUCT_ID,
            PremiumShopCatalog.LUXURY_COLLECTION_PRODUCT_ID,
        ).forEach { productId ->
            assertEquals(10, PremiumShopCatalog.requireProduct(productId).grantedCosmeticIds.size)
        }
    }

    @Test
    fun `every cosmetic collection grants exactly one ROOM_SKIN and one OBJECT_MATERIAL id`() {
        PremiumShopCatalog.cosmeticCollectionProductIds.forEach { productId ->
            val grantedIds = PremiumShopCatalog.requireProduct(productId).grantedCosmeticIds
            val categories = grantedIds.map { AllCosmeticsCatalog.requireDefinition(it).category }
            assertEquals("$productId should grant exactly one ROOM_SKIN id", 1, categories.count { it == com.suman.memoryarchitect.domain.model.CosmeticCategory.ROOM_SKIN })
            assertEquals("$productId should grant exactly one OBJECT_MATERIAL id", 1, categories.count { it == com.suman.memoryarchitect.domain.model.CosmeticCategory.OBJECT_MATERIAL })
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

    @Test
    fun `every product is NON_CONSUMABLE - no consumable or subscription SKU exists yet`() {
        PremiumShopCatalog.products.forEach { product ->
            assertEquals("$product should be NON_CONSUMABLE", com.suman.memoryarchitect.domain.model.BillingProductType.NON_CONSUMABLE, product.type)
        }
    }
}
