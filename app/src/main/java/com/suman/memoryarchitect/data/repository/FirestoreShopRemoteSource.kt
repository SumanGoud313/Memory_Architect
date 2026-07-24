package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.ShopCatalog
import com.suman.memoryarchitect.domain.progression.SpinRules
import kotlinx.coroutines.tasks.await
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real, per-player-scoped counterpart to [MockBackendShopRemoteSource] - see
 * [ShopRemoteSource]'s doc. Two private Firestore collections, never the public `players/{uid}`
 * the Leaderboard system reads:
 *
 * - `playerCosmetics/{uid}` - owned SKU set + equipped-per-category map.
 * - `purchaseReceipts/{uid}_{nonce}` - replay guard, direct analog of
 *   [FirestoreProgressionRemoteSource]'s `submissionNonces/{uid}_{nonce}`.
 *
 * [purchase] and [spin] both run inside a [com.google.firebase.firestore.FirebaseFirestore.runTransaction]
 * that reads+writes `playerProfiles/{uid}` (the same document [FirestoreProgressionRemoteSource]
 * writes) alongside `playerCosmetics/{uid}` and the receipt doc, so a coin deduction and its
 * ownership grant can never land only one of the two. [equip] is a plain (non-transactional)
 * update - no coins involved, no atomicity requirement beyond Firestore's own single-document
 * write guarantee.
 */
@Singleton
class FirestoreShopRemoteSource @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val clock: Clock,
) : ShopRemoteSource {

    private val firestore by lazy { Firebase.firestore }

    override suspend fun getOwnedSkus(): Set<String> {
        val uid = requireUid()
        val snapshot = firestore.collection(COSMETICS_COLLECTION).document(uid).get().await()
        return snapshot.ownedSkus()
    }

    override suspend fun getEquipped(): Map<String, String> {
        val uid = requireUid()
        val snapshot = firestore.collection(COSMETICS_COLLECTION).document(uid).get().await()
        return snapshot.equippedMap()
    }

    override suspend fun purchase(id: CosmeticId, purchaseNonce: String): Pair<PlayerProfile, Set<String>> {
        val uid = requireUid()
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        val receiptRef = firestore.collection(RECEIPTS_COLLECTION).document(receiptDocId(uid, purchaseNonce))
        val definition = ShopCatalog.requireDefinition(id)
        val nowEpochMs = clock.millis()

        return firestore.runTransaction { transaction ->
            if (transaction.get(receiptRef).exists()) throw DuplicatePurchaseException()
            val cosmeticsSnapshot = transaction.get(cosmeticsRef)
            val owned = cosmeticsSnapshot.ownedSkus()
            if (id.name in owned) throw AlreadyOwnedCosmeticException()
            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            if (currentProfile.coins < definition.priceCoins) throw InsufficientCoinsException()

            val updatedProfile = currentProfile.copy(coins = currentProfile.coins - definition.priceCoins)
            val updatedOwned = owned + id.name

            transaction.set(
                receiptRef,
                mapOf("uid" to uid, "sku" to id.name, "priceCoins" to definition.priceCoins, "kind" to "PURCHASE", "createdAtEpochMs" to nowEpochMs),
            )
            transaction.set(profileRef, updatedProfile.toFirestoreMap(clock), SetOptions.merge())
            // `equipped` must be included even though this write never changes it - firestore.rules'
            // isValidCosmetics requires it present on every write, and a player's very first
            // cosmetics interaction ever (no prior equip/unlock-all) would otherwise create a
            // document missing it entirely, which the rule rejects outright (PERMISSION_DENIED,
            // surfaced to the player as a generic "something went wrong" purchase failure).
            transaction.set(
                cosmeticsRef,
                mapOf(
                    "ownedSkus" to FieldValue.arrayUnion(id.name),
                    "equipped" to cosmeticsSnapshot.equippedMap(),
                    "updatedAtEpochMs" to nowEpochMs,
                ),
                SetOptions.merge(),
            )
            updatedProfile to updatedOwned
        }.await()
    }

    override suspend fun spin(chosenId: CosmeticId, rarity: CosmeticRarity, spinNonce: String): SpinOutcome {
        val uid = requireUid()
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        val receiptRef = firestore.collection(RECEIPTS_COLLECTION).document(receiptDocId(uid, spinNonce))
        val spinCost = SpinRules.Default.spinCostCoins
        val nowEpochMs = clock.millis()

        return firestore.runTransaction { transaction ->
            if (transaction.get(receiptRef).exists()) throw DuplicatePurchaseException()
            val cosmeticsSnapshot = transaction.get(cosmeticsRef)
            val owned = cosmeticsSnapshot.ownedSkus()
            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            if (currentProfile.coins < spinCost) throw InsufficientCoinsException()

            // Re-verified here, not trusted from the caller - see ShopRepository's doc: the roll
            // itself is client-computed, but whether it's a genuinely new grant or a duplicate is
            // decided against this transaction's own freshly-read ownership set.
            val wasDuplicate = chosenId.name in owned
            val definition = ShopCatalog.requireDefinition(chosenId)
            val coinsRefunded = if (wasDuplicate) (definition.priceCoins * SpinRules.Default.duplicateRefundFraction).toLong() else 0L
            val netCoinDelta = coinsRefunded - spinCost
            val updatedProfile = currentProfile.copy(coins = currentProfile.coins + netCoinDelta)
            val updatedOwned = if (wasDuplicate) owned else owned + chosenId.name

            transaction.set(
                receiptRef,
                mapOf("uid" to uid, "sku" to chosenId.name, "priceCoins" to spinCost, "kind" to "SPIN", "createdAtEpochMs" to nowEpochMs),
            )
            transaction.set(profileRef, updatedProfile.toFirestoreMap(clock), SetOptions.merge())
            if (!wasDuplicate) {
                // Same "equipped must always be present" requirement as purchase() above.
                transaction.set(
                    cosmeticsRef,
                    mapOf(
                        "ownedSkus" to FieldValue.arrayUnion(chosenId.name),
                        "equipped" to cosmeticsSnapshot.equippedMap(),
                        "updatedAtEpochMs" to nowEpochMs,
                    ),
                    SetOptions.merge(),
                )
            }
            SpinOutcome(chosenId, wasDuplicate, coinsRefunded, updatedProfile, updatedOwned)
        }.await()
    }

    override suspend fun equip(category: CosmeticCategory, id: CosmeticId): Map<String, String> {
        val uid = requireUid()
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        // Read-modify-write the whole `equipped` map explicitly rather than relying on
        // SetOptions.merge() to deep-merge a nested map field - that behavior isn't guaranteed
        // (merge() is well-defined for top-level fields; nested-object merge semantics are the kind
        // of thing worth not gambling on), so a naive one-entry write here could silently wipe out
        // every other category's equipped item. ownedSkus is included for the same "always present"
        // reasoning as purchase()/spin() above.
        val existingSnapshot = cosmeticsRef.get().await()
        val updatedEquipped = existingSnapshot.equippedMap() + (category.name to id.name)
        cosmeticsRef.set(
            mapOf(
                "ownedSkus" to existingSnapshot.ownedSkus().toList(),
                "equipped" to updatedEquipped,
                "updatedAtEpochMs" to clock.millis(),
            ),
            SetOptions.merge(),
        ).await()
        return updatedEquipped
    }

    override suspend fun unequip(category: CosmeticCategory): Map<String, String> {
        val uid = requireUid()
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        // Same read-modify-write-the-whole-map approach as equip() - remove just this category's
        // key from a local copy of the map, then write the complete result back.
        val existingSnapshot = cosmeticsRef.get().await()
        val updatedEquipped = existingSnapshot.equippedMap() - category.name
        cosmeticsRef.set(
            mapOf(
                "ownedSkus" to existingSnapshot.ownedSkus().toList(),
                "equipped" to updatedEquipped,
                "updatedAtEpochMs" to clock.millis(),
            ),
            SetOptions.merge(),
        ).await()
        return updatedEquipped
    }

    private suspend fun requireUid(): String = playerIdentityManager.awaitUid()
        ?: throw IllegalStateException("FirestoreShopRemoteSource used with no signed-in player - callers must gate via ShopRepositoryImpl.activeRemoteSource first")

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.ownedSkus(): Set<String> =
        (get("ownedSkus") as? List<String>)?.toSet() ?: emptySet()

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.equippedMap(): Map<String, String> =
        (get("equipped") as? Map<String, String>) ?: emptyMap()

    private fun DocumentSnapshot.toProfile(): PlayerProfile? {
        if (!exists()) return null
        return PlayerProfile(
            xp = getLong("xp") ?: 0L,
            coins = getLong("coins") ?: 0L,
            currentStreak = (getLong("currentStreak") ?: 0L).toInt(),
            longestStreak = (getLong("longestStreak") ?: 0L).toInt(),
            lastPlayedEpochDay = getLong("lastPlayedEpochDay"),
            dailyChallengeWonAtEpochSecond = getLong("dailyChallengeWonAtEpochSecond"),
            weeklyChallengeWonAtEpochSecond = getLong("weeklyChallengeWonAtEpochSecond"),
        )
    }

    private fun PlayerProfile.toFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
        "xp" to xp,
        "coins" to coins,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "lastPlayedEpochDay" to lastPlayedEpochDay,
        "dailyChallengeWonAtEpochSecond" to dailyChallengeWonAtEpochSecond,
        "weeklyChallengeWonAtEpochSecond" to weeklyChallengeWonAtEpochSecond,
        "updatedAtEpochMs" to clock.millis(),
    )

    private companion object {
        const val PROFILES_COLLECTION = "playerProfiles"
        const val COSMETICS_COLLECTION = "playerCosmetics"
        const val RECEIPTS_COLLECTION = "purchaseReceipts"

        fun receiptDocId(uid: String, nonce: String): String = "${uid}_$nonce"
    }
}
