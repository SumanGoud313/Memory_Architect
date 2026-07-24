package com.suman.memoryarchitect.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for the lifetime "Remove Ads" one-time purchase
 * (`remove_ads_lifetime`, a non-consumable in-app product) - every place in the app that needs to
 * know "does this player see ads" reads [hasRemovedAds], never Play Billing directly. Backed by
 * Google Play Billing only: no custom payment gateway, no in-app card collection - Play itself
 * presents whichever payment methods it supports for the player's country (cards, UPI, carrier
 * billing, Google Pay, gift cards, etc.), and [productDetails] carries Play's own already-localized
 * price string, so this app never hardcodes a price or does its own currency conversion anywhere.
 *
 * [hasRemovedAds] starts from [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore]'s
 * cached value (instant, no network wait) and is corrected to the live, Play-verified value the
 * moment [startConnection] finishes its first purchase query - see that method's doc. This is also
 * what "restore purchases automatically on reinstall/new device" actually means in practice: Play
 * itself remembers a Google-account-linked non-consumable purchase forever, so simply asking Play
 * "what does this account own" on every cold start *is* the restore - there's no separate manual
 * "restore" action required for it to work, [restorePurchases] exists only as an explicit
 * player-visible retry for the rare case the automatic query missed (e.g. it ran before the device
 * ever came online).
 */
interface BillingManager {
    /** `true` once Play has confirmed this Google Play account owns `remove_ads_lifetime` -
     * Banner/Interstitial/App Open ads (whenever such ad types exist in this app - none do today,
     * see `AdsModule`'s doc) must check this before showing. Rewarded ads are deliberately never
     * gated by this - see [PurchaseUiState] callers' doc / the feature's own requirements: they're
     * always an optional player choice for a bonus, never forced, so a purchase has nothing to
     * remove there. */
    val hasRemovedAds: StateFlow<Boolean>

    /** `null` until Play's product details query resolves (no network yet, or it's still loading) -
     * the Remove Ads screen shows a loading state for its price/Buy Now affordance until this is
     * non-null. */
    val productDetails: StateFlow<RemoveAdsProduct?>

    val purchaseState: StateFlow<PurchaseUiState>

    /** Connects to Google Play, then queries product details and existing purchases - call once,
     * early (see [com.suman.memoryarchitect.MemoryArchitectApp]), same "best-effort, kicked off
     * once at startup" shape as [com.suman.memoryarchitect.core.auth.PlayerIdentityManager.ensureSignedIn].
     * Safe to call more than once (a no-op past the first attempt) - callers never need to guard
     * against creating a second connection/[com.android.billingclient.api.BillingClient] instance. */
    fun startConnection()

    /** Launches Play's own purchase UI. [purchaseState] reflects the outcome - this function itself
     * never throws or returns a result directly, matching how [startConnection] and the rest of
     * this app's Firebase-backed managers report outcomes through observable state rather than a
     * return value the caller must remember to check. */
    fun launchPurchase(activity: Activity)

    /** Explicit player-visible retry of the same "ask Play what this account owns" query
     * [startConnection] already runs automatically - see this interface's doc for why the
     * automatic path already covers reinstall/new-device restoration in the common case. */
    fun restorePurchases()
}
