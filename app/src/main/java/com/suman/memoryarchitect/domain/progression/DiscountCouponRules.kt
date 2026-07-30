package com.suman.memoryarchitect.domain.progression

/** Tuning for [com.suman.memoryarchitect.domain.model.InventoryItemKind.DISCOUNT_COUPON] - a flat
 * percentage off a single cosmetic purchase's coin price, consumed atomically in the same
 * transaction as the purchase itself (see
 * [com.suman.memoryarchitect.data.repository.FirestoreShopRemoteSource.purchase]). A discounted
 * price is always <= the catalog price, so it never needs its own bound beyond the ordinary
 * purchase path's own shape checks (`firestore.rules`' `purchaseReceipts` rule). */
data class DiscountCouponRules(val discountFraction: Double = 0.25) {
    companion object {
        val Default = DiscountCouponRules()
    }

    fun discountedPrice(catalogPriceCoins: Long): Long =
        (catalogPriceCoins * (1.0 - discountFraction)).toLong().coerceAtLeast(0L)
}
