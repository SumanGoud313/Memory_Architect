package com.suman.memoryarchitect.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import com.suman.memoryarchitect.domain.model.CosmeticId

/** One premium cosmetic bundle's live Play Billing price, resolved the same way
 * [RemoveAdsProduct] is - Play's own already-localized string, never computed by this app. */
data class PremiumProductPrice(val formattedPrice: String)

/** Same shape as [PurchaseUiState], generalized to carry which `productId` it's about - a Premium
 * Shop screen has 7 buyable products showing at once, unlike Remove Ads' single card. */
sealed interface PremiumPurchaseUiState {
    data object Idle : PremiumPurchaseUiState
    data class Loading(val productId: String) : PremiumPurchaseUiState

    /** [grantedCosmeticIds] lets the UI play a purchase-success celebration naming what was just
     * unlocked, and lets the caller merge them into the in-memory owned-ids set immediately
     * without waiting for a fresh [com.suman.memoryarchitect.domain.usecase.GetOwnedCosmeticsUseCase] read. */
    data class Success(val productId: String, val grantedCosmeticIds: List<CosmeticId>) : PremiumPurchaseUiState
    data class AlreadyOwned(val productId: String) : PremiumPurchaseUiState
    data class Pending(val productId: String) : PremiumPurchaseUiState
    data class Failed(val productId: String, val reason: BillingFailureReason) : PremiumPurchaseUiState
    data object RestoreNotFound : PremiumPurchaseUiState
}

/**
 * The real-money owner of [com.suman.memoryarchitect.domain.progression.PremiumShopCatalog.cosmeticBundleProductIds]
 * (Founder's Pack, Starter Bundle, and the 5 themed collections) - the sibling [BillingManager]
 * never grows to cover, so its one existing, already-working `remove_ads_lifetime` flow stays
 * completely untouched. Shares [SharedBillingClient]'s one [com.android.billingclient.api.BillingClient]
 * instance rather than constructing a second one - see that class's doc.
 *
 * Unlike [BillingManager.hasRemovedAds] (a simple local flag), this manager deliberately does
 * **not** expose an "owned products" flow of its own - whether a bundle is owned is answered by
 * checking `ownedCosmeticIds` (via [com.suman.memoryarchitect.domain.usecase.GetOwnedCosmeticsUseCase])
 * against [com.suman.memoryarchitect.domain.progression.PremiumShopCatalog.PremiumProduct.grantedCosmeticIds],
 * the same single source of truth Collections/equip already use - so a premium bundle and a coin
 * item are indistinguishable once owned, by construction.
 *
 * A purchase is never granted from a raw [com.android.billingclient.api.Purchase] alone (unlike
 * [BillingManager]/the coin Point Shop) - [handlePurchasesUpdated] always round-trips through the
 * `verifyPremiumPurchase` Cloud Function first, which re-verifies the purchase against the Play
 * Developer API server-side before this manager writes anything locally. See that function's doc
 * in `functions/src/index.ts` for the account-binding/replay-guard design.
 */
interface PremiumShopManager {
    val productPrices: StateFlow<Map<String, PremiumProductPrice>>
    val purchaseState: StateFlow<PremiumPurchaseUiState>

    /** Safe to call more than once; a no-op past the first attempt - same contract as
     * [BillingManager.startConnection]. */
    fun startConnection()

    fun launchPurchase(activity: Activity, productId: String)

    fun restorePurchases()
}
