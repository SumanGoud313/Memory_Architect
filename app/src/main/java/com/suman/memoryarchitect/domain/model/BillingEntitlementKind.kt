package com.suman.memoryarchitect.domain.model

/**
 * What a successful purchase actually grants - orthogonal to [BillingProductType] (which only
 * describes the Play Billing mechanics). `core.billing.BillingManagerImpl`'s grant path
 * `when`-dispatches on this, never on [BillingCatalogProduct.type], so "how do I pay for this" and
 * "what do I get" can vary independently: a future coin pack ([BillingProductType.CONSUMABLE] +
 * [CONSUMABLE_GRANT]) needs a different Play API call than Remove Ads but is granted by the same
 * kind of straightforward local-flag flip [REMOVE_ADS] already is.
 */
enum class BillingEntitlementKind {
    /** Flips `BillingManager.hasRemovedAds` (and its `UserPreferencesDataStore` cache) - no
     * cross-device cosmetic content to sync, so no Firestore write is needed; Play Billing itself
     * is what restores this on a new device (see `BillingManager`'s own doc). */
    REMOVE_ADS,

    /** Unlocks every [BillingCatalogProduct.grantedCosmeticIds] in one Premium Collection -
     * mirrored into Firestore (`playerCosmetics/{uid}`) for cross-device restore, and into the
     * local Room owned-cosmetics cache. */
    COSMETIC_COLLECTION,

    /** Not yet wired to a real grant - no consumable SKU exists today. Reserved for a future coin
     * pack/hint pack/Lucky Spin token/event token: the grant would credit a quantity (coins, a
     * token count) rather than unlock a permanent cosmetic. */
    CONSUMABLE_GRANT,

    /** Not yet wired to a real grant - no subscription SKU exists today. Reserved for a future VIP
     * Pass/membership/season pass: the grant would set an active-tier flag with an expiry Play
     * itself tracks, re-checked on every `startConnection`/`restorePurchases`, rather than a
     * permanent one-time unlock. */
    SUBSCRIPTION_TIER,
}
