package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import com.suman.memoryarchitect.domain.progression.DiscountCouponRules
import com.suman.memoryarchitect.domain.progression.ShopCatalog
import com.suman.memoryarchitect.domain.progression.SpinRules
import com.suman.memoryarchitect.domain.repository.SpinSource
import kotlinx.coroutines.tasks.await
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real, per-player-scoped counterpart to [MockBackendShopRemoteSource] - see
 * [ShopRemoteSource]'s doc. Private Firestore collections, never the public `players/{uid}`
 * the Leaderboard system reads:
 *
 * - `playerCosmetics/{uid}` - owned SKU set + equipped-per-category map.
 * - `purchaseReceipts/{uid}_{nonce}` - replay guard, direct analog of
 *   [FirestoreProgressionRemoteSource]'s `submissionNonces/{uid}_{nonce}`.
 * - `luckySpinState/{uid}` - Lucky Spin's own daily-gate/first-spin state, kept out of
 *   `playerProfiles/{uid}` on purpose - see [LuckySpinState]'s doc.
 *
 * [purchase] and [spin] both run inside a [com.google.firebase.firestore.FirebaseFirestore.runTransaction]
 * that reads+writes `playerProfiles/{uid}` (the same document [FirestoreProgressionRemoteSource]
 * writes) alongside `playerCosmetics/{uid}` and the receipt doc ([spin] additionally reads+writes
 * `luckySpinState/{uid}`), so a coin deduction and its ownership grant can never land only one of
 * the two. [equip] is a plain (non-transactional) update - no coins involved, no atomicity
 * requirement beyond Firestore's own single-document write guarantee.
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

    override suspend fun purchase(id: CosmeticId, purchaseNonce: String, useDiscountCoupon: Boolean): Pair<PlayerProfile, Set<String>> {
        val uid = requireUid()
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        val receiptRef = firestore.collection(RECEIPTS_COLLECTION).document(receiptDocId(uid, purchaseNonce))
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)
        val definition = ShopCatalog.requireDefinition(id)
        val nowEpochMs = clock.millis()

        return firestore.runTransaction { transaction ->
            if (transaction.get(receiptRef).exists()) throw DuplicatePurchaseException()
            val cosmeticsSnapshot = transaction.get(cosmeticsRef)
            val owned = cosmeticsSnapshot.ownedSkus()
            if (id.name in owned) throw AlreadyOwnedCosmeticException()

            // Consumed atomically with the purchase itself - see ShopRemoteSource.purchase's own
            // useDiscountCoupon doc for why this can't be a separate write.
            var updatedInventoryQuantities: Map<InventoryItemKind, Int>? = null
            val priceCoins = if (useDiscountCoupon) {
                val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
                val ownedCoupons = currentQuantities[InventoryItemKind.DISCOUNT_COUPON] ?: 0
                if (ownedCoupons < 1) throw InsufficientInventoryException()
                currentQuantities[InventoryItemKind.DISCOUNT_COUPON] = ownedCoupons - 1
                updatedInventoryQuantities = currentQuantities
                DiscountCouponRules.Default.discountedPrice(definition.priceCoins)
            } else {
                definition.priceCoins
            }

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            if (currentProfile.coins < priceCoins) throw InsufficientCoinsException()

            val updatedProfile = currentProfile.copy(coins = currentProfile.coins - priceCoins)
            val updatedOwned = owned + id.name

            transaction.set(
                receiptRef,
                mapOf("uid" to uid, "sku" to id.name, "priceCoins" to priceCoins, "kind" to "PURCHASE", "createdAtEpochMs" to nowEpochMs),
            )
            // lastWriteSource: see PROFILE_WRITE_SOURCES' doc in functions/src/index.ts - lets
            // validateProfileWrite rate-limit this write independently of an unrelated action
            // (a score submission, a mission claim) landing within the same few seconds.
            transaction.set(profileRef, updatedProfile.toFirestoreMap(clock) + mapOf("lastWriteSource" to "shop_purchase"), SetOptions.merge())
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
            if (updatedInventoryQuantities != null) transaction.set(inventoryRef, updatedInventoryQuantities.toFirestoreMap())
            updatedProfile to updatedOwned
        }.await()
    }

    override suspend fun getLuckySpinState(): LuckySpinState {
        val uid = requireUid()
        val snapshot = firestore.collection(LUCKY_SPIN_STATE_COLLECTION).document(uid).get().await()
        return snapshot.toLuckySpinState()
    }

    override suspend fun spin(request: SpinRequest, spinNonce: String, source: SpinSource, todayEpochDay: Long): SpinOutcome {
        val uid = requireUid()
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        val receiptRef = firestore.collection(RECEIPTS_COLLECTION).document(receiptDocId(uid, spinNonce))
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)
        val spinStateRef = firestore.collection(LUCKY_SPIN_STATE_COLLECTION).document(uid)
        val nowEpochMs = clock.millis()

        return firestore.runTransaction { transaction ->
            if (transaction.get(receiptRef).exists()) throw DuplicatePurchaseException()
            val cosmeticsSnapshot = transaction.get(cosmeticsRef)
            val owned = cosmeticsSnapshot.ownedSkus()
            val currentSpinState = transaction.get(spinStateRef).toLuckySpinState()

            // Re-verified here, not trusted from the caller's own button-enabled state - see
            // ShopRepository.spin's doc. A ticket spend (below) is never subject to this gate.
            var updatedInventoryQuantities: Map<InventoryItemKind, Int>? = null
            when (source) {
                SpinSource.FREE -> if (currentSpinState.lastFreeSpinEpochDay == todayEpochDay) throw SpinNotAvailableException()
                SpinSource.AD -> if (currentSpinState.lastAdSpinEpochDay == todayEpochDay) throw SpinNotAvailableException()
                SpinSource.TICKET -> {
                    // Consumed atomically with the spin itself - see ShopRemoteSource.spin's own
                    // SpinSource.TICKET doc for why this can't be a separate write.
                    val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
                    val ownedTickets = currentQuantities[InventoryItemKind.LUCKY_SPIN_TICKET] ?: 0
                    if (ownedTickets < 1) throw InsufficientInventoryException()
                    currentQuantities[InventoryItemKind.LUCKY_SPIN_TICKET] = ownedTickets - 1
                    updatedInventoryQuantities = currentQuantities
                }
            }

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY

            val wasDuplicate: Boolean
            val coinsRefunded: Long
            val netCoinDelta: Long
            val updatedOwned: Set<String>
            val reward: SpinRewardKind
            when (request) {
                is SpinRequest.Coins -> {
                    wasDuplicate = false
                    coinsRefunded = 0L
                    netCoinDelta = request.amount
                    updatedOwned = owned
                    reward = SpinRewardKind.Coins(request.amount)
                }
                is SpinRequest.Cosmetic -> {
                    // Re-verified here, not trusted from the caller - see ShopRepository's doc:
                    // the roll itself is client-computed, but whether it's a genuinely new grant
                    // or a duplicate is decided against this transaction's own freshly-read
                    // ownership set.
                    wasDuplicate = request.id.name in owned
                    val definition = ShopCatalog.requireDefinition(request.id)
                    coinsRefunded = if (wasDuplicate) (definition.priceCoins * SpinRules.Default.duplicateRefundFraction).toLong() else 0L
                    netCoinDelta = coinsRefunded
                    updatedOwned = if (wasDuplicate) owned else owned + request.id.name
                    reward = SpinRewardKind.Cosmetic(request.id, request.rarity)
                }
            }
            val updatedProfile = currentProfile.copy(coins = currentProfile.coins + netCoinDelta)
            val updatedSpinState = currentSpinState.copy(
                lastFreeSpinEpochDay = if (source == SpinSource.FREE) todayEpochDay else currentSpinState.lastFreeSpinEpochDay,
                lastAdSpinEpochDay = if (source == SpinSource.AD) todayEpochDay else currentSpinState.lastAdSpinEpochDay,
                hasEverSpun = true,
            )

            // "sku" must stay a non-null string either way - firestore.rules' purchaseReceipts
            // validator requires it (see that match block's doc) - so a Coins-kind spin (no real
            // sku) writes a fixed sentinel instead of null; the receipt is only ever read back via
            // .exists() as a replay guard, never inspected for meaning.
            transaction.set(
                receiptRef,
                mapOf(
                    "uid" to uid,
                    "sku" to ((request as? SpinRequest.Cosmetic)?.id?.name ?: "COINS"),
                    "priceCoins" to netCoinDelta,
                    "kind" to "SPIN",
                    "createdAtEpochMs" to nowEpochMs,
                ),
            )
            // See purchase()'s own lastWriteSource comment above.
            transaction.set(profileRef, updatedProfile.toFirestoreMap(clock) + mapOf("lastWriteSource" to "shop_spin"), SetOptions.merge())
            transaction.set(spinStateRef, updatedSpinState.toFirestoreMap(clock), SetOptions.merge())
            if (request is SpinRequest.Cosmetic && !wasDuplicate) {
                // Same "equipped must always be present" requirement as purchase() above.
                transaction.set(
                    cosmeticsRef,
                    mapOf(
                        "ownedSkus" to FieldValue.arrayUnion(request.id.name),
                        "equipped" to cosmeticsSnapshot.equippedMap(),
                        "updatedAtEpochMs" to nowEpochMs,
                    ),
                    SetOptions.merge(),
                )
            }
            if (updatedInventoryQuantities != null) transaction.set(inventoryRef, updatedInventoryQuantities.toFirestoreMap())
            SpinOutcome(reward, wasDuplicate, coinsRefunded, updatedProfile, updatedOwned, updatedSpinState)
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
        //
        // Wrapped in a transaction (unlike the original read-then-write) - two devices equipping
        // different categories within the same round-trip window used to be a genuine lost-update
        // race: both read the same `equipped` map, both compute a different one-entry addition, and
        // whichever write lands second silently discarded the first device's change. A transaction
        // re-reads atomically and Firestore retries it on contention, closing that window the same
        // way every other multi-step write in this class already does.
        return firestore.runTransaction { transaction ->
            val existingSnapshot = transaction.get(cosmeticsRef)
            val updatedEquipped = existingSnapshot.equippedMap() + (category.name to id.name)
            transaction.set(
                cosmeticsRef,
                mapOf(
                    "ownedSkus" to existingSnapshot.ownedSkus().toList(),
                    "equipped" to updatedEquipped,
                    "updatedAtEpochMs" to clock.millis(),
                ),
                SetOptions.merge(),
            )
            updatedEquipped
        }.await()
    }

    override suspend fun unequip(category: CosmeticCategory): Map<String, String> {
        val uid = requireUid()
        val cosmeticsRef = firestore.collection(COSMETICS_COLLECTION).document(uid)
        // Same read-modify-write-the-whole-map approach as equip() above, and the same
        // transactional fix for the same concurrent-device lost-update race.
        return firestore.runTransaction { transaction ->
            val existingSnapshot = transaction.get(cosmeticsRef)
            val updatedEquipped = existingSnapshot.equippedMap() - category.name
            transaction.set(
                cosmeticsRef,
                mapOf(
                    "ownedSkus" to existingSnapshot.ownedSkus().toList(),
                    "equipped" to updatedEquipped,
                    "updatedAtEpochMs" to clock.millis(),
                ),
                SetOptions.merge(),
            )
            updatedEquipped
        }.await()
    }

    private suspend fun requireUid(): String = playerIdentityManager.awaitUid()
        ?: throw IllegalStateException("FirestoreShopRemoteSource used with no signed-in player - callers must gate via ShopRepositoryImpl.activeRemoteSource first")

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.ownedSkus(): Set<String> =
        (get("ownedSkus") as? List<String>)?.toSet() ?: emptySet()

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.equippedMap(): Map<String, String> =
        (get("equipped") as? Map<String, String>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toQuantities(): Map<InventoryItemKind, Int> {
        if (!exists()) return emptyMap()
        val raw = get("quantities") as? Map<*, *> ?: return emptyMap()
        return raw.mapNotNull { (key, value) ->
            val kind = runCatching { InventoryItemKind.valueOf(key as String) }.getOrNull() ?: return@mapNotNull null
            kind to ((value as? Number)?.toInt() ?: 0)
        }.toMap()
    }

    private fun Map<InventoryItemKind, Int>.toFirestoreMap(): Map<String, Any?> = mapOf(
        "quantities" to mapKeys { it.key.name },
        "updatedAtEpochMs" to clock.millis(),
    )

    private fun DocumentSnapshot.toProfile(): PlayerProfile? {
        if (!exists()) return null
        return PlayerProfile(
            xp = getLong("xp") ?: 0L,
            coins = getLong("coins") ?: 0L,
            currentStreak = (getLong("currentStreak") ?: 0L).toInt(),
            longestStreak = (getLong("longestStreak") ?: 0L).toInt(),
            lastPlayedEpochDay = getLong("lastPlayedEpochDay"),
            streakShields = (getLong("streakShields") ?: 0L).toInt(),
            dailyChallengeWonAtEpochSecond = getLong("dailyChallengeWonAtEpochSecond"),
            weeklyChallengeWonAtEpochSecond = getLong("weeklyChallengeWonAtEpochSecond"),
            // Previously omitted here (and below in toFirestoreMap) - harmless while every write
            // used SetOptions.merge() (Firestore itself never lost the field), but it meant the
            // PlayerProfile this class returned to ShopRepositoryImpl always reported journeyPoints
            // as 0, which ShopRepositoryImpl.toCacheEntity() then wrote straight into the local Room
            // cache - silently zeroing the player's displayed Memory Journey progress after any
            // shop purchase or spin. Found and fixed while touching this exact mapping for the
            // Discount Coupon / Lucky Spin Ticket work above.
            journeyPoints = getLong("journeyPoints") ?: 0L,
        )
    }

    private fun PlayerProfile.toFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
        "xp" to xp,
        "coins" to coins,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "lastPlayedEpochDay" to lastPlayedEpochDay,
        "streakShields" to streakShields,
        "dailyChallengeWonAtEpochSecond" to dailyChallengeWonAtEpochSecond,
        "weeklyChallengeWonAtEpochSecond" to weeklyChallengeWonAtEpochSecond,
        "journeyPoints" to journeyPoints,
        "updatedAtEpochMs" to clock.millis(),
    )

    private fun DocumentSnapshot.toLuckySpinState(): LuckySpinState {
        if (!exists()) return LuckySpinState.EMPTY
        return LuckySpinState(
            lastFreeSpinEpochDay = getLong("lastFreeSpinEpochDay"),
            lastAdSpinEpochDay = getLong("lastAdSpinEpochDay"),
            hasEverSpun = getBoolean("hasEverSpun") ?: false,
        )
    }

    private fun LuckySpinState.toFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
        "lastFreeSpinEpochDay" to lastFreeSpinEpochDay,
        "lastAdSpinEpochDay" to lastAdSpinEpochDay,
        "hasEverSpun" to hasEverSpun,
        "updatedAtEpochMs" to clock.millis(),
    )

    private companion object {
        const val PROFILES_COLLECTION = "playerProfiles"
        const val COSMETICS_COLLECTION = "playerCosmetics"
        const val RECEIPTS_COLLECTION = "purchaseReceipts"
        const val INVENTORY_COLLECTION = "inventory"
        const val LUCKY_SPIN_STATE_COLLECTION = "luckySpinState"

        fun receiptDocId(uid: String, nonce: String): String = "${uid}_$nonce"
    }
}
