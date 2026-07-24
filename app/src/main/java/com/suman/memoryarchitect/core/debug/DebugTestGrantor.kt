package com.suman.memoryarchitect.core.debug

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.cosmetics.EquippedCosmeticsStore
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only testing convenience, never called from any release-build UI (every entry point into
 * this class is gated by `BuildConfig.DEBUG` at the call site, same convention as
 * `SettingsScreen`'s `AnalyticsDashboard` entry point). Writes directly to the same
 * `playerProfiles/{uid}`/`playerCosmetics/{uid}` Firestore documents the real purchase/spin flow
 * uses (see `FirestoreShopRemoteSource`), then mirrors the change into the local Room cache so it's
 * reflected immediately without waiting for a fresh fetch.
 *
 * Both writes send the **full** document shape (every field `isValidProfile`/`isValidCosmetics` in
 * `firestore.rules` requires), never a partial [SetOptions.merge] of just the changed field - a
 * player who hasn't submitted a score or claimed a daily reward yet has no `playerProfiles/{uid}`
 * document at all, and a merge write containing only `coins` would create one missing `xp`/
 * `currentStreak`/etc., which `isValidProfile` rejects outright (a real bug this class hit and had
 * to be fixed for - the first version failed silently for exactly this reason on a fresh account).
 *
 * Firestore-only (not mock-backend) - both this app's debug and release builds ship
 * `google-services.json`, so [FirebaseAvailability.isConfigured] is true and an anonymous sign-in
 * succeeds long before a player ever reaches the Shop, making Firestore the path actually in use.
 */
@Singleton
class DebugTestGrantor @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val progressDao: PlayerProgressDao,
    private val ownedCosmeticDao: OwnedCosmeticDao,
    private val equippedCosmeticDao: EquippedCosmeticDao,
    private val equippedCosmeticsStore: EquippedCosmeticsStore,
    private val clock: Clock,
) {
    private val firestore by lazy { Firebase.firestore }

    suspend fun grantTestCoins(amount: Long): Result<Long> = runCatching {
        check(FirebaseAvailability.isConfigured) { "Firebase isn't configured - no server profile to grant coins on" }
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val ref = firestore.collection("playerProfiles").document(uid)
        val newBalance = firestore.runTransaction { transaction ->
            val current = transaction.get(ref).toProfile()
            val updated = current.copy(coins = current.coins + amount)
            transaction.set(ref, updated.toFullFirestoreMap(clock), SetOptions.merge())
            updated.coins
        }.await()
        progressDao.get()?.let { cached -> progressDao.upsert(cached.copy(coins = newBalance)) }
            ?: run {
                // No local cache row yet either (matches "no playerProfiles doc yet") - seed one so
                // Profile/Shop reflect the grant immediately without waiting on a network refetch.
                progressDao.upsert(
                    com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity(
                        xp = 0L, coins = newBalance, currentStreak = 0, longestStreak = 0,
                        lastPlayedEpochDay = null, lastSyncedAt = System.currentTimeMillis(),
                    ),
                )
            }
        newBalance
    }

    suspend fun unlockAllCosmeticsForTesting(): Result<Unit> = runCatching {
        check(FirebaseAvailability.isConfigured) { "Firebase isn't configured - no server profile to unlock cosmetics on" }
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val allSkus = CosmeticId.entries.map { it.name }
        val ref = firestore.collection("playerCosmetics").document(uid)
        val existingEquipped = runCatching { ref.get().await().get("equipped") as? Map<*, *> }.getOrNull() ?: emptyMap<String, String>()
        ref.set(
            mapOf("ownedSkus" to allSkus, "equipped" to existingEquipped, "updatedAtEpochMs" to clock.millis()),
            SetOptions.merge(),
        ).await()
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        allSkus.forEach { sku -> ownedCosmeticDao.upsert(OwnedCosmeticEntity(sku, todayEpochDay, "DEBUG_GRANT", System.currentTimeMillis())) }
    }

    /** The inverse of [unlockAllCosmeticsForTesting] - re-locks every cosmetic, matching a brand
     * new account's true starting state. Clears `equipped` too, not just `ownedSkus`: leaving an
     * equipped reference to a now-unowned item is a state a real player could never reach (equip
     * always requires ownership first), and every equipped-cosmetic renderer in this app assumes
     * that invariant holds. Pushes the cleared map into [equippedCosmeticsStore] directly so every
     * border/frame/ring reverts app-wide immediately, the same instant-propagation path
     * `ShopRepositoryImpl.unequip()` already uses. */
    suspend fun lockAllCosmeticsForTesting(): Result<Unit> = runCatching {
        check(FirebaseAvailability.isConfigured) { "Firebase isn't configured - no server profile to lock cosmetics on" }
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val ref = firestore.collection("playerCosmetics").document(uid)
        ref.set(
            mapOf("ownedSkus" to emptyList<String>(), "equipped" to emptyMap<String, String>(), "updatedAtEpochMs" to clock.millis()),
            SetOptions.merge(),
        ).await()
        ownedCosmeticDao.clearAll()
        equippedCosmeticDao.clearAll()
        equippedCosmeticsStore.setAll(emptyMap())
    }

    /** Developer Test Mode - simulates a successful Premium Shop purchase without any real Google
     * Play Billing transaction, granting exactly what `verifyPremiumPurchase` (the real Cloud
     * Function, see `functions/src/index.ts`) would have granted. `BuildConfig.DEBUG`-gated at
     * every call site, same as every other method in this class - stripped from release builds
     * entirely, never a code path a real purchase could accidentally hit.
     *
     * Deliberately does **not** write to `premiumPurchases`/`claimedPurchaseTokens` - both are
     * Admin-SDK-only collections with no `firestore.rules` entry (rules default-deny anything
     * without an explicit `allow`, and a client write from a debug build is still a client write),
     * and there's no real purchase token here to audit anyway. This class's whole design already
     * avoids special debug-only paths through production data - see the class doc - so it simply
     * grants the cosmetics and stops there. */
    suspend fun debugGrantPremiumProduct(productId: String): Result<Unit> = runCatching {
        check(FirebaseAvailability.isConfigured) { "Firebase isn't configured - no server profile to grant cosmetics on" }
        val product = PremiumShopCatalog.productOrNull(productId) ?: error("Unknown premium product: $productId")
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val grantedSkus = product.grantedCosmeticIds.map { it.name }
        val ref = firestore.collection("playerCosmetics").document(uid)
        val existingSnapshot = ref.get().await()
        val existingOwned = (existingSnapshot.get("ownedSkus") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val existingEquipped = existingSnapshot.get("equipped") as? Map<*, *> ?: emptyMap<String, String>()
        val updatedOwned = (existingOwned + grantedSkus).distinct()
        ref.set(
            mapOf("ownedSkus" to updatedOwned, "equipped" to existingEquipped, "updatedAtEpochMs" to clock.millis()),
            SetOptions.merge(),
        ).await()
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        grantedSkus.forEach { sku -> ownedCosmeticDao.upsert(OwnedCosmeticEntity(sku, todayEpochDay, "PREMIUM_DEBUG_GRANT", System.currentTimeMillis())) }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProfile(): PlayerProfile {
        if (!exists()) return PlayerProfile.EMPTY
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

    /** Every field [isValidProfile][firestore.rules] requires, present every time - see the class
     * doc for why a partial merge of just `coins` is unsafe on a not-yet-existing document. */
    private fun PlayerProfile.toFullFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
        "xp" to xp,
        "coins" to coins,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "lastPlayedEpochDay" to lastPlayedEpochDay,
        "dailyChallengeWonAtEpochSecond" to dailyChallengeWonAtEpochSecond,
        "weeklyChallengeWonAtEpochSecond" to weeklyChallengeWonAtEpochSecond,
        "updatedAtEpochMs" to clock.millis(),
    )
}
