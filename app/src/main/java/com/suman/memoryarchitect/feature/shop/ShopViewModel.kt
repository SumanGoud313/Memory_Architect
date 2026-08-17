package com.suman.memoryarchitect.feature.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logProductViewed
import com.suman.memoryarchitect.core.analytics.logShopViewed
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.core.billing.premiumStoreEnabled
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import com.suman.memoryarchitect.domain.usecase.GetInventoryUseCase
import com.suman.memoryarchitect.domain.usecase.GetOwnedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import com.suman.memoryarchitect.domain.usecase.GetShopCatalogUseCase
import com.suman.memoryarchitect.domain.usecase.PurchaseCosmeticUseCase
import com.suman.memoryarchitect.domain.usecase.RecordMissionEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val getCatalog: GetShopCatalogUseCase,
    private val getOwnedCosmetics: GetOwnedCosmeticsUseCase,
    private val getProfile: GetPlayerProfileUseCase,
    private val getInventory: GetInventoryUseCase,
    private val purchaseCosmetic: PurchaseCosmeticUseCase,
    private val recordMissionEvent: RecordMissionEventUseCase,
    private val billingManager: BillingManager,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopUiState>(ShopUiState.Loading)
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        load()
        observeBillingState()
        analytics.logShopViewed()
    }

    fun retry() = load()

    /** Merges [BillingManager]'s already-live `StateFlow`s into [Content] as they emit - it's an
     * app-wide singleton that started connecting in
     * [com.suman.memoryarchitect.MemoryArchitectApp.onCreate], so it's often already warm by the
     * time this screen opens. A no-op whenever [_uiState] isn't [Content] yet (e.g. still
     * [ShopUiState.Loading]) - the next relevant emission catches it up, same tolerant pattern the
     * rest of this class already uses for its own async updates. */
    private fun observeBillingState() {
        viewModelScope.launch { billingManager.productStates.collect { states -> updateContent { it.copy(billingProductStates = states) } } }
        viewModelScope.launch { billingManager.productDetailsLoadFailed.collect { failed -> updateContent { it.copy(billingProductsLoadFailed = failed) } } }
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                updateContent { it.copy(purchaseState = state) }
                // Merge the grant into ownedIds immediately rather than waiting for a fresh
                // GetOwnedCosmeticsUseCase read - Collections/CosmeticGlyph everywhere else in the
                // app already treat ownedIds as the single source of truth once granted.
                if (state is PurchaseUiState.Success && state.grantedCosmeticIds.isNotEmpty()) {
                    updateContent { it.copy(ownedIds = it.ownedIds + state.grantedCosmeticIds) }
                }
            }
        }
    }

    private inline fun updateContent(transform: (ShopUiState.Content) -> ShopUiState.Content) {
        val current = _uiState.value as? ShopUiState.Content ?: return
        _uiState.value = transform(current)
    }

    fun selectTab(tab: ShopTab) = updateContent { it.copy(selectedTab = tab) }

    fun onProductViewed(productId: String) = analytics.logProductViewed(productId)

    fun buyProduct(activity: Activity, productId: String) = billingManager.launchPurchase(activity, productId)

    fun restorePurchases() = billingManager.restorePurchases()

    /** Explicit "Retry" affordance shown once [ShopUiState.Content.billingProductsLoadFailed] is
     * `true` - see [BillingManager.retryLoadProductDetails]'s doc. */
    fun retryBillingPriceLoad() = billingManager.retryLoadProductDetails()

    private fun load() {
        _uiState.value = ShopUiState.Loading
        viewModelScope.launch {
            coroutineScope {
                val coins = when (val outcome = getProfile()) {
                    is Outcome.Success -> outcome.data.coins
                    is Outcome.Error -> 0L
                }
                val owned = getOwnedCosmetics()
                // Optimistic `true` default (same reasoning as ModeSelectViewModel's Remove Ads
                // button) - a slow/failed Remote Config fetch should never hide a real purchase
                // surface, only a console-set `false`, once actually fetched, should.
                val premiumEnabled = (remoteConfigRepository.getRemoteConfig() as? Outcome.Success)
                    ?.data?.premiumStoreEnabled() ?: true
                _uiState.value = ShopUiState.Content(
                    coins = coins,
                    catalog = getCatalog(),
                    ownedIds = owned,
                    premiumStoreEnabled = premiumEnabled,
                )
            }
        }
    }

    fun purchase(id: CosmeticId) {
        val current = _uiState.value as? ShopUiState.Content ?: return
        if (current.purchasingId != null || id in current.ownedIds) return
        _uiState.value = current.copy(purchasingId = id, errorReason = null)
        viewModelScope.launch {
            // An owned Discount Coupon is auto-applied to every purchase, the same "help the
            // player spend the scarce resource rather than ask them to opt in every time" default
            // Hint/Redo/Rewatch tokens and Lucky Spin Tickets already use (see
            // LuckySpinViewModel.spin's doc).
            val useDiscountCoupon = (getInventory() as? Outcome.Success)?.data?.quantityOf(InventoryItemKind.DISCOUNT_COUPON)?.let { it > 0 } ?: false
            when (val outcome = purchaseCosmetic(id, useDiscountCoupon)) {
                is Outcome.Success -> {
                    val latest = _uiState.value as? ShopUiState.Content ?: return@launch
                    _uiState.value = latest.copy(
                        coins = outcome.data.updatedProfile.coins,
                        ownedIds = latest.ownedIds + id,
                        purchasingId = null,
                    )
                    recordMissionEvent(MissionEvent.CosmeticUnlocked)
                }
                is Outcome.Error -> {
                    val latest = _uiState.value as? ShopUiState.Content ?: return@launch
                    _uiState.value = latest.copy(purchasingId = null, errorReason = outcome.error.toShopFailureReason())
                }
            }
        }
    }

}
