package com.suman.memoryarchitect.core.billing

/** Mirrors `com.android.billingclient.api.Purchase.PurchaseState.PURCHASED`'s raw value (1) as a
 * plain Int, so [isGrantedEntitlement] has zero dependency on the Billing Library SDK types and
 * can be unit-tested on the plain JVM runner this project uses - everything else in
 * [BillingManagerImpl] touches the real `BillingClient`/`Purchase` classes directly and can't be. */
const val BILLING_PURCHASE_STATE_PURCHASED = 1

/**
 * Pure decision: does this purchase record represent a currently-granted entitlement for
 * [targetProductId]? A purchase only counts once Play itself reports it as
 * [BILLING_PURCHASE_STATE_PURCHASED] - [BillingManagerImpl] never uses PENDING as the whole point
 * of that state is "not paid for yet, don't grant anything." [productIds] is a purchase record's
 * product ID list (Play Billing supports multi-product purchases in one record, though this app
 * only ever buys one) - membership, not exact-match, is the right check.
 */
fun isGrantedEntitlement(productIds: List<String>, purchaseState: Int, targetProductId: String): Boolean =
    targetProductId in productIds && purchaseState == BILLING_PURCHASE_STATE_PURCHASED
