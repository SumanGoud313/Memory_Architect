package com.suman.memoryarchitect.domain.model

/**
 * Which Google Play Billing mechanics a [BillingCatalogProduct] needs - purely about *how* to call
 * the Play Billing API, never about *what* a successful purchase grants (see [BillingEntitlementKind]
 * for that orthogonal axis). Every product this app sells today is [NON_CONSUMABLE]; [CONSUMABLE]
 * and [SUBSCRIPTION] exist now, with real query/purchase/grant code paths already wired to them, so
 * a future coin pack or VIP Pass needs one new catalog entry and one new `when` branch, never a
 * rewrite of `core.billing.BillingManagerImpl`.
 */
enum class BillingProductType {
    /** Bought once, owned forever, never re-purchasable - Remove Ads, every Premium Collection. */
    NON_CONSUMABLE,

    /** Bought, granted, then explicitly consumed (`consumeAsync`) so the same product can be bought
     * again - coin packs, hint packs, Lucky Spin tokens, event tokens. No consumable SKU is enabled
     * in this app yet; this exists so adding one is a catalog entry, not new plumbing. */
    CONSUMABLE,

    /** Recurring - VIP Pass, a monthly membership, a season pass. No subscription SKU is enabled in
     * this app yet; this exists so adding one is a catalog entry, not new plumbing. */
    SUBSCRIPTION,
}
