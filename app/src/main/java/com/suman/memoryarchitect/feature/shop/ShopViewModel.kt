package com.suman.memoryarchitect.feature.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.PremiumPurchaseUiState
import com.suman.memoryarchitect.core.billing.PremiumShopManager
import com.suman.memoryarchitect.core.debug.DebugTestGrantor
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
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
    private val purchaseCosmetic: PurchaseCosmeticUseCase,
    private val recordMissionEvent: RecordMissionEventUseCase,
    private val debugTestGrantor: DebugTestGrantor,
    private val billingManager: BillingManager,
    private val premiumShopManager: PremiumShopManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopUiState>(ShopUiState.Loading)
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        load()
        observePremiumShopState()
    }

    fun retry() = load()

    /** Merges [BillingManager]/[PremiumShopManager]'s already-live `StateFlow`s into [Content] as
     * they emit - both managers are app-wide singletons that started connecting in
     * [com.suman.memoryarchitect.MemoryArchitectApp.onCreate], so they're often already warm by the
     * time this screen opens. A no-op whenever [_uiState] isn't [Content] yet (e.g. still
     * [ShopUiState.Loading]) - the next relevant emission catches it up, same tolerant pattern the
     * rest of this class already uses for its own async updates. */
    private fun observePremiumShopState() {
        viewModelScope.launch { billingManager.hasRemovedAds.collect { has -> updateContent { it.copy(hasRemovedAds = has) } } }
        viewModelScope.launch { billingManager.productDetails.collect { product -> updateContent { it.copy(removeAdsPrice = product?.formattedPrice) } } }
        viewModelScope.launch { billingManager.purchaseState.collect { state -> updateContent { it.copy(removeAdsPurchaseState = state) } } }
        viewModelScope.launch { premiumShopManager.productPrices.collect { prices -> updateContent { it.copy(premiumPrices = prices) } } }
        viewModelScope.launch {
            premiumShopManager.purchaseState.collect { state ->
                updateContent { it.copy(premiumPurchaseState = state) }
                // Merge the grant into ownedIds immediately rather than waiting for a fresh
                // GetOwnedCosmeticsUseCase read - Collections/CosmeticGlyph everywhere else in the
                // app already treat ownedIds as the single source of truth once granted.
                if (state is PremiumPurchaseUiState.Success) {
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

    fun buyRemoveAds(activity: Activity) = billingManager.launchPurchase(activity)

    fun restoreRemoveAdsPurchase() = billingManager.restorePurchases()

    fun buyPremiumProduct(activity: Activity, productId: String) = premiumShopManager.launchPurchase(activity, productId)

    fun restorePremiumPurchases() = premiumShopManager.restorePurchases()

    /** Developer Test Mode entry point for one Premium Shop product - see [DebugTestGrantor]'s doc.
     * `BuildConfig.DEBUG`-gated at the [ui.screens.shop.ShopScreen] call site, same convention as
     * [debugGrantCoins]/[debugUnlockAll] below. */
    fun debugGrantPremiumProduct(productId: String) {
        viewModelScope.launch {
            debugTestGrantor.debugGrantPremiumProduct(productId)
                .onSuccess {
                    val grantedIds = PremiumShopCatalog.requireProduct(productId).grantedCosmeticIds
                    updateContent { it.copy(ownedIds = it.ownedIds + grantedIds, errorReason = null) }
                }
                .onFailure { updateContent { it.copy(errorReason = ShopFailureReason.GENERIC) } }
        }
    }

    private fun load() {
        _uiState.value = ShopUiState.Loading
        viewModelScope.launch {
            coroutineScope {
                val coins = when (val outcome = getProfile()) {
                    is Outcome.Success -> outcome.data.coins
                    is Outcome.Error -> 0L
                }
                val owned = getOwnedCosmetics()
                _uiState.value = ShopUiState.Content(coins = coins, catalog = getCatalog(), ownedIds = owned)
            }
        }
    }

    fun purchase(id: CosmeticId) {
        val current = _uiState.value as? ShopUiState.Content ?: return
        if (current.purchasingId != null || id in current.ownedIds) return
        _uiState.value = current.copy(purchasingId = id, errorReason = null)
        viewModelScope.launch {
            when (val outcome = purchaseCosmetic(id)) {
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

    /** Debug-build testing convenience only - see [DebugTestGrantor]'s doc. Never called from any
     * release-build UI (the call site in [ui.screens.shop.ShopScreen] is `BuildConfig.DEBUG`-gated).
     * Surfaces a failure via the same [ShopUiState.Content.errorReason] path a real purchase
     * failure uses - the first version of this silently swallowed failures, which is exactly what
     * made a broken Firestore write look like "the button does nothing." */
    fun debugGrantCoins() {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            debugTestGrantor.grantTestCoins(10_000L)
                .onSuccess { newBalance ->
                    val latest = _uiState.value as? ShopUiState.Content ?: return@onSuccess
                    _uiState.value = latest.copy(coins = newBalance, errorReason = null)
                }
                .onFailure {
                    _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC)
                }
        }
    }

    fun debugUnlockAll() {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            debugTestGrantor.unlockAllCosmeticsForTesting()
                .onSuccess {
                    val latest = _uiState.value as? ShopUiState.Content ?: return@onSuccess
                    _uiState.value = latest.copy(ownedIds = CosmeticId.entries.toSet(), errorReason = null)
                }
                .onFailure {
                    _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC)
                }
        }
    }

    /** The inverse of [debugUnlockAll] - see [DebugTestGrantor.lockAllCosmeticsForTesting]. */
    fun debugLockAll() {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            debugTestGrantor.lockAllCosmeticsForTesting()
                .onSuccess {
                    val latest = _uiState.value as? ShopUiState.Content ?: return@onSuccess
                    _uiState.value = latest.copy(ownedIds = emptySet(), errorReason = null)
                }
                .onFailure {
                    _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC)
                }
        }
    }
}
