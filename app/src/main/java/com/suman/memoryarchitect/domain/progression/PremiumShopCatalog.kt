package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.BillingCatalogProduct
import com.suman.memoryarchitect.domain.model.BillingEntitlementKind
import com.suman.memoryarchitect.domain.model.BillingProductType
import com.suman.memoryarchitect.domain.model.CosmeticId

/**
 * The centralized, static catalog of every real-money Google Play Billing product in this app - the
 * single `BillingManager` (see `core.billing.BillingManager`'s own doc) is the only thing that ever
 * queries/purchases/restores these, so a new product (a future coin pack, a future VIP Pass) means
 * one new entry here plus one new grant-path `when` branch, never a new manager. Remove Ads lives in
 * this same list ([REMOVE_ADS_PRODUCT_ID], `entitlement = BillingEntitlementKind.REMOVE_ADS`) rather
 * than a separate catalog - the split that used to exist between it and the cosmetic bundles was a
 * manager-layer artifact, not a real difference in how either is catalogued. Mirrored in
 * `functions/src/premiumCatalog.ts`'s doc-comment only now (see that file for why the Cloud Function
 * itself is no longer called) - keep both in sync as a reference, the same "keep Kotlin/TypeScript
 * in sync" convention `shopCatalog.ts` already established for the coin catalog.
 */
object PremiumShopCatalog {
    const val REMOVE_ADS_PRODUCT_ID = "remove_ads_lifetime"
    const val FOUNDERS_PACK_PRODUCT_ID = "founders_pack"
    const val STARTER_BUNDLE_PRODUCT_ID = "starter_bundle"
    const val ROYAL_COLLECTION_PRODUCT_ID = "royal_collection"
    const val CYBER_COLLECTION_PRODUCT_ID = "cyber_collection"
    const val SPACE_COLLECTION_PRODUCT_ID = "space_collection"
    const val NATURE_COLLECTION_PRODUCT_ID = "nature_collection"
    const val LUXURY_COLLECTION_PRODUCT_ID = "luxury_collection"

    /** The 7 cosmetic-collection product ids, deliberately excluding [REMOVE_ADS_PRODUCT_ID] - what
     * the Premium Shop tab renders (see `ui.screens.shop.ShopScreen`'s own `.filter`); Remove Ads
     * keeps its own dedicated entry point/screen instead. */
    val cosmeticCollectionProductIds: List<String> = listOf(
        FOUNDERS_PACK_PRODUCT_ID, STARTER_BUNDLE_PRODUCT_ID, ROYAL_COLLECTION_PRODUCT_ID,
        CYBER_COLLECTION_PRODUCT_ID, SPACE_COLLECTION_PRODUCT_ID, NATURE_COLLECTION_PRODUCT_ID,
        LUXURY_COLLECTION_PRODUCT_ID,
    )

    val products: List<BillingCatalogProduct> = listOf(
        BillingCatalogProduct(
            billingProductId = REMOVE_ADS_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.REMOVE_ADS,
            titleRes = R.string.remove_ads_headline,
            taglineRes = R.string.remove_ads_subheadline,
        ),
        BillingCatalogProduct(
            billingProductId = FOUNDERS_PACK_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_founders_pack_title,
            taglineRes = R.string.premium_founders_pack_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_FOUNDER_INAUGURAL, CosmeticId.FRAME_FOUNDER_PIONEER,
                CosmeticId.BACKGROUND_FOUNDER_GENESIS, CosmeticId.NAME_COLOR_FOUNDER_ORIGIN,
                CosmeticId.TIMER_FOUNDER_LEGACY, CosmeticId.VICTORY_FOUNDER_TRIUMPH,
                CosmeticId.CONFETTI_FOUNDER_JUBILEE, CosmeticId.BADGE_FOUNDER_EMBLEM,
                CosmeticId.ROOM_FOUNDER_HERITAGE, CosmeticId.MATERIAL_FOUNDER_BRASS,
            ),
            isBestValue = true,
        ),
        BillingCatalogProduct(
            billingProductId = STARTER_BUNDLE_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_starter_bundle_title,
            taglineRes = R.string.premium_starter_bundle_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_STARTER_SUNRISE, CosmeticId.BACKGROUND_STARTER_HORIZON,
                CosmeticId.TIMER_STARTER_COMPASS, CosmeticId.VICTORY_STARTER_SPARK,
                CosmeticId.FRAME_STARTER_WAYFARER,
                CosmeticId.ROOM_STARTER_DAWN, CosmeticId.MATERIAL_STARTER_CANVAS,
            ),
        ),
        BillingCatalogProduct(
            billingProductId = ROYAL_COLLECTION_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_royal_collection_title,
            taglineRes = R.string.premium_royal_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_ROYAL_CROWN, CosmeticId.FRAME_ROYAL_CREST, CosmeticId.NAME_COLOR_ROYAL_VELVET,
                CosmeticId.TIMER_ROYAL_HOURGLASS, CosmeticId.VICTORY_ROYAL_FANFARE, CosmeticId.CONFETTI_ROYAL_PETALS,
                CosmeticId.BACKGROUND_ROYAL_THRONE, CosmeticId.BADGE_ROYAL_SEAL,
                CosmeticId.ROOM_ROYAL_PALACE, CosmeticId.MATERIAL_ROYAL_GILDED_MARBLE,
            ),
        ),
        BillingCatalogProduct(
            billingProductId = CYBER_COLLECTION_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_cyber_collection_title,
            taglineRes = R.string.premium_cyber_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_CYBER_CIRCUIT, CosmeticId.FRAME_CYBER_VISOR, CosmeticId.NAME_COLOR_CYBER_GLITCH,
                CosmeticId.TIMER_CYBER_PULSE, CosmeticId.VICTORY_CYBER_OVERDRIVE, CosmeticId.CONFETTI_CYBER_SPARKS,
                CosmeticId.BACKGROUND_CYBER_GRID, CosmeticId.BADGE_CYBER_CHIP,
                CosmeticId.ROOM_CYBER_GRIDWORKS, CosmeticId.MATERIAL_CYBER_CHROME_CIRCUIT,
            ),
        ),
        BillingCatalogProduct(
            billingProductId = SPACE_COLLECTION_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_space_collection_title,
            taglineRes = R.string.premium_space_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_SPACE_ORBIT, CosmeticId.FRAME_SPACE_NEBULA, CosmeticId.NAME_COLOR_SPACE_COSMOS,
                CosmeticId.TIMER_SPACE_PULSAR, CosmeticId.VICTORY_SPACE_SUPERNOVA, CosmeticId.CONFETTI_SPACE_STARDUST,
                CosmeticId.BACKGROUND_SPACE_GALAXY, CosmeticId.BADGE_SPACE_COMET,
                CosmeticId.ROOM_SPACE_NEBULA_DOCK, CosmeticId.MATERIAL_SPACE_GUNMETAL,
            ),
        ),
        BillingCatalogProduct(
            billingProductId = NATURE_COLLECTION_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_nature_collection_title,
            taglineRes = R.string.premium_nature_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_NATURE_VINE, CosmeticId.FRAME_NATURE_LEAF, CosmeticId.NAME_COLOR_NATURE_MOSS,
                CosmeticId.TIMER_NATURE_BLOOM, CosmeticId.VICTORY_NATURE_BLOSSOM, CosmeticId.CONFETTI_NATURE_PETALS,
                CosmeticId.BACKGROUND_NATURE_FOREST, CosmeticId.BADGE_NATURE_ACORN,
                CosmeticId.ROOM_NATURE_GROVE, CosmeticId.MATERIAL_NATURE_MOSS_WOOD,
            ),
        ),
        BillingCatalogProduct(
            billingProductId = LUXURY_COLLECTION_PRODUCT_ID,
            type = BillingProductType.NON_CONSUMABLE,
            entitlement = BillingEntitlementKind.COSMETIC_COLLECTION,
            titleRes = R.string.premium_luxury_collection_title,
            taglineRes = R.string.premium_luxury_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_LUXURY_ONYX, CosmeticId.FRAME_LUXURY_DIAMOND, CosmeticId.NAME_COLOR_LUXURY_PLATINUM,
                CosmeticId.TIMER_LUXURY_CHRONOGRAPH, CosmeticId.VICTORY_LUXURY_SPOTLIGHT, CosmeticId.CONFETTI_LUXURY_GOLDLEAF,
                CosmeticId.BACKGROUND_LUXURY_PENTHOUSE, CosmeticId.BADGE_LUXURY_CREST,
                CosmeticId.ROOM_LUXURY_SUITE, CosmeticId.MATERIAL_LUXURY_SMOKED_GLASS,
            ),
        ),
    )

    private val byProductId: Map<String, BillingCatalogProduct> = products.associateBy { it.billingProductId }

    fun requireProduct(productId: String): BillingCatalogProduct =
        byProductId[productId] ?: throw NoSuchElementException("No BillingCatalogProduct for $productId")

    fun productOrNull(productId: String): BillingCatalogProduct? = byProductId[productId]
}
