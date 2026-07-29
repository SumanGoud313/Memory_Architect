package com.suman.memoryarchitect.core.billing

import com.suman.memoryarchitect.domain.model.CosmeticId

/** One product's live Play Billing state, as resolved by the unified `BillingManager` - keyed by
 * `billingProductId` in [BillingManager.productStates]. [formattedPrice] is Play's own
 * already-localized price string (e.g. "$4.99", "₹299.00"), never computed or converted by this
 * app; `null` means either still loading or genuinely unavailable - see
 * [BillingManager.productDetailsLoadFailed] (one global flag, not per-product - a partial catalog
 * gap, e.g. one product not yet published in this Play Console track, was never distinguishable
 * from "still loading" in either predecessor manager, and this keeps that same proven behavior
 * rather than inventing a new distinction). [owned] is derived from whichever ownership source is
 * authoritative for that product's `BillingEntitlementKind` (the local `hasRemovedAds` flag for
 * Remove Ads, `ownedCosmeticIds` containing every one of a collection's `grantedCosmeticIds` for a
 * cosmetic collection) - never a bare "was this ever purchased" flag, since Play Billing itself, not
 * local state, is the actual source of truth (see `BillingManager`'s own doc). */
data class BillingProductUiState(
    val formattedPrice: String? = null,
    val owned: Boolean = false,
)

/** Drives every purchase-affordance screen's Buy Now/Restore Purchase states - one shared shape for
 * both a single-product screen ([com.suman.memoryarchitect.ui.screens.removeads.RemoveAdsScreen],
 * which only ever looks at the entry for `remove_ads_lifetime`) and a multi-product screen (the
 * Premium Shop tab, which has several buyable products showing at once) - every case below is
 * `productId`-keyed for exactly that reason. [Idle] covers both "nothing attempted yet" and "a
 * Failed/Cancelled attempt was dismissed" - there's no persistent error banner, a failed attempt
 * just returns the screen to its normal Buy Now state. */
sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data class Loading(val productId: String) : PurchaseUiState

    /** [grantedCosmeticIds] lets the UI play a purchase-success celebration naming what was just
     * unlocked, and lets the caller merge them into the in-memory owned-ids set immediately without
     * waiting for a fresh `GetOwnedCosmeticsUseCase` read - empty for Remove Ads (nothing cosmetic
     * to name), non-empty for a cosmetic collection. */
    data class Success(val productId: String, val grantedCosmeticIds: List<CosmeticId> = emptyList()) : PurchaseUiState
    data class AlreadyOwned(val productId: String) : PurchaseUiState

    /** Play accepted a payment method that settles asynchronously (e.g. UPI collect, carrier
     * billing) - not yet entitled, but not failed either. Resolves on its own to [Success] via a
     * later `PurchasesUpdatedListener` callback or the next [BillingManager.restorePurchases] call;
     * nothing the player needs to do. */
    data class Pending(val productId: String) : PurchaseUiState
    data class Failed(val productId: String, val reason: BillingFailureReason) : PurchaseUiState

    /** [BillingManager.restorePurchases] specifically (never the automatic startup query) came back
     * with nothing to restore - distinct from [Idle] purely so the screen can show a brief "nothing
     * found" message for an action the player just explicitly took, without that message lingering
     * as if it were the screen's normal resting state. */
    data object RestoreNotFound : PurchaseUiState
}

/** Same shape as [com.suman.memoryarchitect.core.ads.RewardedAdFailureReason] - a plain reason
 * enum, never a raw message, so display text stays centralized and localizable (see
 * `toDisplayMessage`) rather than embedded in this domain-level state. User cancellation is
 * deliberately not one of these cases - `BillingManagerImpl` resolves it straight back to
 * [PurchaseUiState.Idle], since cancelling isn't a failure worth an error message. */
enum class BillingFailureReason {
    /** Play Billing hasn't finished connecting, or the product details query hasn't resolved yet. */
    NOT_READY,

    /** Play reported the product itself as unavailable for this account/region/device. */
    ITEM_UNAVAILABLE,

    /** Play Billing isn't usable at all right now (outdated Play Store app, unsupported device,
     * no Play Store account). */
    BILLING_UNAVAILABLE,

    GENERIC,
}
