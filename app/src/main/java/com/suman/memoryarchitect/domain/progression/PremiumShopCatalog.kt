package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.PremiumProduct
import com.suman.memoryarchitect.domain.model.PremiumProductSource

/**
 * Static, client-known catalog of every Premium Shop product - same "the catalog itself is never
 * persisted" convention [ShopCatalog]/[RewardCatalog] already use. Includes the pre-existing
 * `remove_ads_lifetime` product (tagged [PremiumProductSource.LEGACY_REMOVE_ADS]) alongside the 7
 * new cosmetic bundles, so the Premium tab can render one card type off one list - the ViewModel
 * backing it dispatches to `core.billing.BillingManager` or `core.billing.PremiumShopManager`
 * by [PremiumProduct.source], the same `when`-dispatch shape
 * [com.suman.memoryarchitect.data.repository.ShopRepositoryImpl.activeRemoteSource] already uses
 * for mock-vs-Firestore. Every id in [PremiumProduct.grantedCosmeticIds] must exist in
 * [PremiumCatalog.definitions] - see that object's doc. Mirrored in `functions/src/premiumCatalog.ts`
 * (product id -> granted skus), the same "keep Kotlin/TypeScript in sync" convention `shopCatalog.ts`
 * already established.
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

    /** The 7 new cosmetic-bundle product ids - what `PremiumShopManager` queries/purchases/restores.
     * Deliberately excludes [REMOVE_ADS_PRODUCT_ID], which stays owned end-to-end by the untouched
     * `BillingManager`. */
    val cosmeticBundleProductIds: List<String> = listOf(
        FOUNDERS_PACK_PRODUCT_ID, STARTER_BUNDLE_PRODUCT_ID, ROYAL_COLLECTION_PRODUCT_ID,
        CYBER_COLLECTION_PRODUCT_ID, SPACE_COLLECTION_PRODUCT_ID, NATURE_COLLECTION_PRODUCT_ID,
        LUXURY_COLLECTION_PRODUCT_ID,
    )

    val products: List<PremiumProduct> = listOf(
        PremiumProduct(
            billingProductId = REMOVE_ADS_PRODUCT_ID,
            source = PremiumProductSource.LEGACY_REMOVE_ADS,
            titleRes = R.string.remove_ads_headline,
            taglineRes = R.string.remove_ads_subheadline,
        ),
        PremiumProduct(
            billingProductId = FOUNDERS_PACK_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_founders_pack_title,
            taglineRes = R.string.premium_founders_pack_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_FOUNDER_INAUGURAL, CosmeticId.FRAME_FOUNDER_PIONEER,
                CosmeticId.BACKGROUND_FOUNDER_GENESIS, CosmeticId.NAME_COLOR_FOUNDER_ORIGIN,
                CosmeticId.TIMER_FOUNDER_LEGACY, CosmeticId.VICTORY_FOUNDER_TRIUMPH,
                CosmeticId.CONFETTI_FOUNDER_JUBILEE, CosmeticId.BADGE_FOUNDER_EMBLEM,
            ),
            isBestValue = true,
        ),
        PremiumProduct(
            billingProductId = STARTER_BUNDLE_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_starter_bundle_title,
            taglineRes = R.string.premium_starter_bundle_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_STARTER_SUNRISE, CosmeticId.BACKGROUND_STARTER_HORIZON,
                CosmeticId.TIMER_STARTER_COMPASS, CosmeticId.VICTORY_STARTER_SPARK,
                CosmeticId.FRAME_STARTER_WAYFARER,
            ),
        ),
        PremiumProduct(
            billingProductId = ROYAL_COLLECTION_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_royal_collection_title,
            taglineRes = R.string.premium_royal_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_ROYAL_CROWN, CosmeticId.FRAME_ROYAL_CREST, CosmeticId.NAME_COLOR_ROYAL_VELVET,
                CosmeticId.TIMER_ROYAL_HOURGLASS, CosmeticId.VICTORY_ROYAL_FANFARE, CosmeticId.CONFETTI_ROYAL_PETALS,
                CosmeticId.BACKGROUND_ROYAL_THRONE, CosmeticId.BADGE_ROYAL_SEAL,
            ),
        ),
        PremiumProduct(
            billingProductId = CYBER_COLLECTION_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_cyber_collection_title,
            taglineRes = R.string.premium_cyber_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_CYBER_CIRCUIT, CosmeticId.FRAME_CYBER_VISOR, CosmeticId.NAME_COLOR_CYBER_GLITCH,
                CosmeticId.TIMER_CYBER_PULSE, CosmeticId.VICTORY_CYBER_OVERDRIVE, CosmeticId.CONFETTI_CYBER_SPARKS,
                CosmeticId.BACKGROUND_CYBER_GRID, CosmeticId.BADGE_CYBER_CHIP,
            ),
        ),
        PremiumProduct(
            billingProductId = SPACE_COLLECTION_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_space_collection_title,
            taglineRes = R.string.premium_space_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_SPACE_ORBIT, CosmeticId.FRAME_SPACE_NEBULA, CosmeticId.NAME_COLOR_SPACE_COSMOS,
                CosmeticId.TIMER_SPACE_PULSAR, CosmeticId.VICTORY_SPACE_SUPERNOVA, CosmeticId.CONFETTI_SPACE_STARDUST,
                CosmeticId.BACKGROUND_SPACE_GALAXY, CosmeticId.BADGE_SPACE_COMET,
            ),
        ),
        PremiumProduct(
            billingProductId = NATURE_COLLECTION_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_nature_collection_title,
            taglineRes = R.string.premium_nature_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_NATURE_VINE, CosmeticId.FRAME_NATURE_LEAF, CosmeticId.NAME_COLOR_NATURE_MOSS,
                CosmeticId.TIMER_NATURE_BLOOM, CosmeticId.VICTORY_NATURE_BLOSSOM, CosmeticId.CONFETTI_NATURE_PETALS,
                CosmeticId.BACKGROUND_NATURE_FOREST, CosmeticId.BADGE_NATURE_ACORN,
            ),
        ),
        PremiumProduct(
            billingProductId = LUXURY_COLLECTION_PRODUCT_ID,
            source = PremiumProductSource.COSMETIC_BUNDLE,
            titleRes = R.string.premium_luxury_collection_title,
            taglineRes = R.string.premium_luxury_collection_tagline,
            grantedCosmeticIds = listOf(
                CosmeticId.BORDER_LUXURY_ONYX, CosmeticId.FRAME_LUXURY_DIAMOND, CosmeticId.NAME_COLOR_LUXURY_PLATINUM,
                CosmeticId.TIMER_LUXURY_CHRONOGRAPH, CosmeticId.VICTORY_LUXURY_SPOTLIGHT, CosmeticId.CONFETTI_LUXURY_GOLDLEAF,
                CosmeticId.BACKGROUND_LUXURY_PENTHOUSE, CosmeticId.BADGE_LUXURY_CREST,
            ),
        ),
    )

    private val byProductId: Map<String, PremiumProduct> = products.associateBy { it.billingProductId }

    fun requireProduct(productId: String): PremiumProduct =
        byProductId[productId] ?: throw NoSuchElementException("No PremiumProduct for $productId")

    fun productOrNull(productId: String): PremiumProduct? = byProductId[productId]
}
