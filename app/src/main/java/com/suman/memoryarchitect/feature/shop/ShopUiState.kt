package com.suman.memoryarchitect.feature.shop

import com.suman.memoryarchitect.core.billing.BillingProductUiState
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.model.CosmeticId

/** Which storefront half of [ShopScreen] is visible - see the Shop redesign plan's "two completely
 * separate sections" requirement. Purely UI state, never persisted. */
enum class ShopTab { COIN, PREMIUM }

sealed interface ShopUiState {
    data object Loading : ShopUiState
    data class Content(
        val coins: Long,
        val catalog: List<CosmeticDefinition>,
        val ownedIds: Set<CosmeticId>,
        val purchasingId: CosmeticId? = null,
        val errorReason: ShopFailureReason? = null,
        val selectedTab: ShopTab = ShopTab.COIN,
        // Every real-money product's live Play Billing state, keyed by billingProductId - one
        // unified map covering Remove Ads and every Premium Collection alike, since
        // core.billing.BillingManager is now the one manager for both (see that interface's doc).
        val billingProductStates: Map<String, BillingProductUiState> = emptyMap(),
        val billingProductsLoadFailed: Boolean = false,
        val purchaseState: PurchaseUiState = PurchaseUiState.Idle,
        // Remote Config's `premium_store_enabled` kill switch (see
        // core.billing.premiumStoreEnabled's doc) - hides the 💎 Premium tab chip entirely when
        // `false`, same instant no-release toggle Mode Select's Remove Ads button already uses.
        // Optimistic `true` default so a slow/failed fetch never hides a real purchase surface.
        val premiumStoreEnabled: Boolean = true,
    ) : ShopUiState
}
