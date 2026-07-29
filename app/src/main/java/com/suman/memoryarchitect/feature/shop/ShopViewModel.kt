package com.suman.memoryarchitect.feature.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logProductViewed
import com.suman.memoryarchitect.core.analytics.logShopViewed
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.core.debug.DebugTestGrantor
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
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
    private val debugTestGrantor: DebugTestGrantor,
    private val billingManager: BillingManager,
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

    /** Unlike [debugUnlockAll]/[debugLockAll] (Shop cosmetics), this unlocks the Classic *level*
     * campaign - see [DebugTestGrantor.unlockAllLevelsForTesting]. Success is only visible on Level
     * Select, not this screen, so it needs the same explicit [debugMessage] confirmation
     * [debugTriggerSeasonalEvent] already documents needing. */
    fun debugUnlockAllLevels() {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            debugTestGrantor.unlockAllLevelsForTesting()
                .onSuccess { _uiState.value = current.copy(errorReason = null, debugMessage = "All 100 levels unlocked - open Level Select to see it") }
                .onFailure { _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC, debugMessage = null) }
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

    /** Debug-build testing convenience only, see [DebugTestGrantor]'s doc - a thin wrapper per
     * grant category, all following the same "fire the grant, surface only a failure" shape as
     * [debugGrantCoins] above. None of these have a dedicated display field on [ShopUiState.Content]
     * the way `coins` does, so a successful grant is verified by visiting the screen it actually
     * shows up on (Profile, Inventory, Missions, Notifications) - same as any other debug tool,
     * not a defect in this wiring. */
    fun debugGrantXp() = runDebugGrant { debugTestGrantor.grantTestXp(1_000L) }
    fun debugGrantJourneyPoints() = runDebugGrant { debugTestGrantor.grantTestJourneyPoints() }
    fun debugGrantStreakShield() = runDebugGrant { debugTestGrantor.grantTestStreakShield() }
    fun debugGrantInventoryItem(kind: InventoryItemKind) = runDebugGrant { debugTestGrantor.grantInventoryItem(kind) }
    fun debugResetDailyReward() = runDebugGrant { debugTestGrantor.debugResetDailyReward() }
    fun debugResetReturningPlayer() = runDebugGrant { debugTestGrantor.debugResetReturningPlayer() }
    fun debugTriggerNotification() = runDebugGrant { debugTestGrantor.debugTriggerNotification() }
    /** Unlike every other debug button, success here is otherwise invisible - the override this
     * sets is only checked by the Missions screen elsewhere, so without an explicit confirmation
     * this looks identical to the button doing nothing (the exact confusion that prompted
     * [com.suman.memoryarchitect.core.debug.DebugLiveEventOverride] to exist in the first place). */
    fun debugTriggerSeasonalEvent(eventId: String) {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            debugTestGrantor.debugTriggerSeasonalEvent(eventId)
                .onSuccess { _uiState.value = current.copy(errorReason = null, debugMessage = "$eventId event is now active - open Missions to see it") }
                .onFailure { _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC, debugMessage = null) }
        }
    }

    fun debugClearSeasonalEventOverride() {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            debugTestGrantor.debugClearSeasonalEventOverride()
                .onSuccess { _uiState.value = current.copy(errorReason = null, debugMessage = "Event override cleared") }
                .onFailure { _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC, debugMessage = null) }
        }
    }

    private fun runDebugGrant(grant: suspend () -> Result<*>) {
        viewModelScope.launch {
            val current = _uiState.value as? ShopUiState.Content ?: return@launch
            grant().onFailure { _uiState.value = current.copy(errorReason = ShopFailureReason.GENERIC) }
        }
    }
}
