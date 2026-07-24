package com.suman.memoryarchitect.feature.shop

import com.suman.memoryarchitect.core.billing.PremiumProductPrice
import com.suman.memoryarchitect.core.billing.PremiumPurchaseUiState
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
        val hasRemovedAds: Boolean = false,
        val removeAdsPrice: String? = null,
        val removeAdsPurchaseState: PurchaseUiState = PurchaseUiState.Idle,
        val premiumPrices: Map<String, PremiumProductPrice> = emptyMap(),
        val premiumPurchaseState: PremiumPurchaseUiState = PremiumPurchaseUiState.Idle,
    ) : ShopUiState
}
