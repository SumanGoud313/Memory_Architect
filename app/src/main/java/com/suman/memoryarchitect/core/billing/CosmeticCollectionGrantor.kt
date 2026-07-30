package com.suman.memoryarchitect.core.billing

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticEntity
import com.suman.memoryarchitect.domain.model.BillingCatalogProduct
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place a [BillingCatalogProduct] with `entitlement = BillingEntitlementKind.COSMETIC_COLLECTION`
 * actually gets granted, called by [BillingManagerImpl] right after a real Play purchase
 * ([replayGuardToken] is the Play purchase token).
 *
 * Local Room cache first (works even offline/signed-out), then - only if Firebase is configured and
 * a uid is available - a Firestore transaction mirrors the grant for cross-device restore. The
 * transaction is the Spark-compatible replacement for what a Cloud Function used to do: a
 * `claimedPurchaseTokens/{sha256(token)}` document that can only ever be created once (see
 * `firestore.rules` - `allow update: if false` makes a second claim of the same token fail outright)
 * is the replay guard, and `playerCosmetics/{uid}.ownedSkus` gets the granted skus merged in, same
 * shape this class's debug caller already relied on before this extraction. A Firestore failure is
 * logged, never thrown - for a real purchase, Play has already been paid and the local grant already
 * landed, so a transient Firestore outage must never look like a failed purchase to the player.
 */
@Singleton
class CosmeticCollectionGrantor @Inject constructor(
    private val ownedCosmeticDao: OwnedCosmeticDao,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
    private val clock: Clock,
) {
    suspend fun grant(uid: String?, product: BillingCatalogProduct, replayGuardToken: String) {
        persistOwnedLocally(product.grantedCosmeticIds.map { it.name })
        if (!firebaseAvailabilityProvider.isConfigured || uid == null) return
        runCatching { mirrorToFirestore(uid, product, replayGuardToken) }
            .onFailure { Log.w(TAG, "Firestore mirror failed for ${product.billingProductId}", it) }
    }

    private suspend fun mirrorToFirestore(uid: String, product: BillingCatalogProduct, replayGuardToken: String) {
        val firestore = Firebase.firestore
        val tokenRef = firestore.collection("claimedPurchaseTokens").document(sha256(replayGuardToken))
        val cosmeticsRef = firestore.collection("playerCosmetics").document(uid)
        firestore.runTransaction { transaction ->
            val tokenSnapshot = transaction.get(tokenRef)
            if (!tokenSnapshot.exists()) {
                val cosmeticsSnapshot = transaction.get(cosmeticsRef)
                val existingOwned = (cosmeticsSnapshot.get("ownedSkus") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val existingEquipped = cosmeticsSnapshot.get("equipped") as? Map<*, *> ?: emptyMap<String, String>()
                val grantedSkus = product.grantedCosmeticIds.map { it.name }
                val updatedOwned = (existingOwned + grantedSkus).distinct()
                transaction.set(
                    cosmeticsRef,
                    mapOf("ownedSkus" to updatedOwned, "equipped" to existingEquipped, "updatedAtEpochMs" to clock.millis()),
                    SetOptions.merge(),
                )
                transaction.set(tokenRef, mapOf("uid" to uid, "productId" to product.billingProductId, "claimedAtEpochMs" to clock.millis()))
            }
            // Already claimed (a genuine replay - a retry/restore re-observing the same token, or a
            // repeat debug-grant tap) - nothing new to write, not an error.
        }.await()
    }

    private suspend fun persistOwnedLocally(skus: List<String>) {
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        skus.forEach { sku -> ownedCosmeticDao.upsert(OwnedCosmeticEntity(sku, todayEpochDay, "PREMIUM_PURCHASE", System.currentTimeMillis())) }
    }

    /** Never logs [value] itself (a purchase token) - only ever its hash, per this app's "never
     * expose purchase information in logs" requirement. */
    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "CosmeticCollectionGrantor"
    }
}
