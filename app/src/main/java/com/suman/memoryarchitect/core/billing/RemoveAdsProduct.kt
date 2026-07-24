package com.suman.memoryarchitect.core.billing

/** The lifetime "Remove Ads" product's live details, as resolved from Google Play - [formattedPrice]
 * is Play's own already-localized price string (e.g. "$4.99", "₹299.00"), never computed or
 * converted by this app - see [BillingManager]'s doc for why. */
data class RemoveAdsProduct(val formattedPrice: String)

/** Drives the Remove Ads screen's Buy Now / Restore Purchase affordances. [Idle] covers both
 * "nothing attempted yet" and "a Failed/Cancelled attempt was dismissed" - there's no persistent
 * error banner, a failed attempt just returns the screen to its normal Buy Now state. */
sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Loading : PurchaseUiState
    data object Success : PurchaseUiState
    data object AlreadyOwned : PurchaseUiState

    /** Play accepted a payment method that settles asynchronously (e.g. UPI collect, carrier
     * billing) - not yet entitled, but not failed either. Resolves on its own to [Success] via a
     * later [com.android.billingclient.api.PurchasesUpdatedListener] callback or the next
     * [BillingManager.restorePurchases] call; nothing the player needs to do. */
    data object Pending : PurchaseUiState
    data class Failed(val reason: BillingFailureReason) : PurchaseUiState

    /** [BillingManager.restorePurchases] specifically (never the automatic startup query) came
     * back with nothing to restore - distinct from [Idle] purely so the screen can show a brief
     * "nothing found" message for an action the player just explicitly took, without that message
     * lingering as if it were the screen's normal resting state. */
    data object RestoreNotFound : PurchaseUiState
}

/** Same shape as [com.suman.memoryarchitect.core.ads.RewardedAdFailureReason] - a plain reason
 * enum, never a raw message, so display text stays centralized and localizable (see
 * `toDisplayMessage`) rather than embedded in this domain-level state. User cancellation is
 * deliberately not one of these cases - [BillingManagerImpl] resolves it straight back to
 * [PurchaseUiState.Idle], since cancelling isn't a failure worth an error message. */
enum class BillingFailureReason {
    /** Play Billing hasn't finished connecting, or the product details query hasn't resolved yet -
     * most commonly hit if `remove_ads_lifetime` doesn't exist yet in Play Console (see
     * `BILLING_SETUP.md`), or the very first instant after opening this screen. */
    NOT_READY,

    /** Play reported the product itself as unavailable for this account/region/device. */
    ITEM_UNAVAILABLE,

    /** Play Billing isn't usable at all right now (outdated Play Store app, unsupported device,
     * no Play Store account). */
    BILLING_UNAVAILABLE,

    GENERIC,
}
