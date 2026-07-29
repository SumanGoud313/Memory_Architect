package com.suman.memoryarchitect.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * The single owner of every real-money purchase in this app - `remove_ads_lifetime` and every
 * Premium Collection today, and (by construction, not by aspiration) any future consumable or
 * subscription product added to `domain.progression.PremiumShopCatalog`. There is deliberately no
 * second billing manager anywhere in this codebase - a prior version of this app had a separate
 * `PremiumShopManager` for the cosmetic-bundle products, which this class replaces entirely; see
 * `SharedBillingClient`'s doc for why exactly one `BillingClient`/`PurchasesUpdatedListener` may
 * ever exist, and why this manager is now that listener's only subscriber.
 *
 * Google Play Billing is always the source of truth for ownership - local caches (the
 * `hasRemovedAds`/`ownedProductIds` DataStore flags, the Room owned-cosmetics table, the Firestore
 * `playerCosmetics` mirror) exist purely for fast reads and cross-device convenience, and are always
 * reconciled against a fresh [startConnection]/[restorePurchases] query, never trusted alone.
 *
 * Runs entirely on Firebase Spark - no Cloud Function is called anywhere in this class. Every grant
 * is verified locally via [PurchaseSignatureVerifier] (see that class's own doc for the resulting
 * security trade-off) instead of a Blaze-only server-side Android Publisher API check.
 */
interface BillingManager {
    /** `true` once Play has confirmed this Google Play account owns `remove_ads_lifetime` - checked
     * by `feature.ads.BannerAdViewModel`/`feature.ads.InterstitialGateViewModel` before either ad
     * format ever shows anything; both react live to this same [StateFlow], so a purchase
     * completing mid-session stops both instantly, with no app restart needed. Rewarded ads are
     * deliberately never gated by this - they're always an optional player choice for a bonus, a
     * purchase has nothing to remove there. */
    val hasRemovedAds: StateFlow<Boolean>

    /** Every catalog product's live price + ownership, keyed by `billingProductId` - a product
     * absent from this map hasn't resolved yet (ordinary loading), never a distinct state from "not
     * yet loaded". */
    val productStates: StateFlow<Map<String, BillingProductUiState>>

    val purchaseState: StateFlow<PurchaseUiState>

    /** `true` once a `queryProductDetails` attempt has genuinely failed (a thrown exception, or a
     * non-OK `BillingResult`) and no successful attempt has replaced it since - lets a purchase
     * screen show a real "price unavailable, tap to retry" state instead of an unexplained
     * perpetual loading indicator. Never `true` merely because a product's entry in [productStates]
     * is still missing early on (that's ordinary loading, not a failure). */
    val productDetailsLoadFailed: StateFlow<Boolean>

    /** Connects to Google Play, then queries every catalog product's details and existing
     * purchases - call once, early (see `MemoryArchitectApp`). Safe to call more than once (a no-op
     * past the first attempt). */
    fun startConnection()

    /** Re-attempts the same `queryProductDetails` call [startConnection] already ran once - for the
     * explicit "Retry" affordance a purchase screen shows after [productDetailsLoadFailed] becomes
     * `true`. Safe to call repeatedly. */
    fun retryLoadProductDetails()

    /** Launches Play's own purchase UI for [productId]. [purchaseState] reflects the outcome - this
     * function itself never throws or returns a result directly. */
    fun launchPurchase(activity: Activity, productId: String)

    /** Explicit player-visible retry of "ask Play what this account owns" - the automatic query
     * [startConnection] already runs covers reinstall/new-device restoration in the common case;
     * this exists only for the rare case that automatic query missed (e.g. it ran before the device
     * ever came online). */
    fun restorePurchases()
}
