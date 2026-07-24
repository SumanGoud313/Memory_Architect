package com.suman.memoryarchitect.core.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single [BillingClient] instance for the whole app process. [BillingManager] (the pre-existing
 * `remove_ads_lifetime` flow) and [PremiumShopManager] (the 7 cosmetic-bundle products added
 * alongside it) both need a `BillingClient`, but Google Play Billing supports exactly one
 * [PurchasesUpdatedListener] per client, and this codebase's own established rule (see
 * [BillingManager]'s doc) is that exactly one `BillingClient` instance may ever exist. Rather than
 * either manager privately constructing its own (violating that rule) or [BillingManager]
 * special-casing the 7 new product ids itself (defeating the point of a separate manager), this
 * class owns the one real client and [purchasesUpdated] broadcasts every update to both managers,
 * each filtering for the product ids it actually owns - the same "one shared signal, every
 * observer decides what's relevant to it" shape [com.suman.memoryarchitect.core.cosmetics.EquippedCosmeticsStore]
 * already uses for equipped-cosmetic state.
 */
@Singleton
class SharedBillingClient @Inject constructor(@param:ApplicationContext context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionMutex = Mutex()
    private var connectionAttempted = false

    /** Flips to `true` once [BillingClient.startConnection] succeeds, and back to `false` on
     * disconnect - both managers collect this (filtering for `true`) to know when it's safe to
     * query product details/existing purchases, re-running that query on every reconnect too. */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _purchasesUpdated = MutableSharedFlow<PurchasesUpdate>(extraBufferCapacity = 8)
    val purchasesUpdated: SharedFlow<PurchasesUpdate> = _purchasesUpdated.asSharedFlow()

    val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(
                PurchasesUpdatedListener { billingResult, purchases ->
                    _purchasesUpdated.tryEmit(PurchasesUpdate(billingResult, purchases.orEmpty()))
                },
            )
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    }

    /** Idempotent - safe for both managers to call; only the first caller actually starts the
     * connection, matching [BillingManager.startConnection]'s original "safe to call more than
     * once" contract. */
    fun startConnection() {
        if (connectionAttempted) return
        scope.launch {
            connectionMutex.withLock {
                if (connectionAttempted) return@withLock
                connectionAttempted = true
                connect()
            }
        }
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                    return
                }
                _isReady.value = true
            }

            override fun onBillingServiceDisconnected() {
                _isReady.value = false
                // Google's own recommendation: retry with a short backoff rather than leaving the
                // client permanently unusable.
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    if (!billingClient.isReady) connect()
                }
            }
        })
    }

    data class PurchasesUpdate(val billingResult: BillingResult, val purchases: List<Purchase>)

    private companion object {
        const val TAG = "SharedBillingClient"
        const val RECONNECT_DELAY_MS = 2_000L
    }
}
