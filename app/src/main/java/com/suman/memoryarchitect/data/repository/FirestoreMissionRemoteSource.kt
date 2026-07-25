package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.MissionCatalog
import kotlinx.coroutines.tasks.await
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real, per-player-scoped counterpart to [MockBackendMissionRemoteSource] - see
 * [MissionRemoteSource]'s doc. Three private Firestore collections:
 *
 * - `playerProfiles/{uid}` - the exact same document [FirestoreProgressionRemoteSource] reads/
 *   writes, so a mission's coin/xp grant lands in the one place the rest of the app already
 *   reads a player's profile from.
 * - `inventory/{uid}` - one `quantities` map field, keyed by [InventoryItemKind.name].
 * - `missionClaims/{uid}` - one `claimedKeys` map field (`"${missionId}_${periodKey}"` ->
 *   epoch-ms claimed), the double-claim guard.
 *
 * [claimMissionReward] independently re-derives whether a claim is legitimate before touching any
 * of the three - unlike `submitScore`'s xp/coins (which depend on a scored round's object-level
 * detail the server never sees), a mission's active-set membership and reward are both pure
 * functions of ([MissionId], `periodKey`) via [MissionCatalog], so this class recomputes them
 * itself rather than trusting the client's claim to be well-formed. Only [progressCount] itself is
 * trusted, the same way a submitted score's accuracy/combo are - see [MissionRemoteSource]'s doc.
 */
@Singleton
class FirestoreMissionRemoteSource @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val clock: Clock,
) : MissionRemoteSource {

    private val firestore by lazy { Firebase.firestore }

    override suspend fun getInventory(): Inventory {
        val uid = requireUid()
        val snapshot = firestore.collection(INVENTORY_COLLECTION).document(uid).get().await()
        return Inventory(snapshot.toQuantities())
    }

    override suspend fun claimMissionReward(missionId: MissionId, periodKey: Long, progressCount: Int): MissionClaimResult {
        val uid = requireUid()
        val definition = MissionCatalog.definitionFor(missionId)
        val activeIds = MissionCatalog.activeMissionIds(definition.period, periodKey)
        if (missionId !in activeIds) throw MissionNotEligibleException("mission not part of period $periodKey's rotation")
        if (progressCount < definition.targetCount) {
            throw MissionNotEligibleException("progress $progressCount below target ${definition.targetCount}")
        }

        val claimKey = claimKeyFor(missionId, periodKey)
        val claimsRef = firestore.collection(MISSION_CLAIMS_COLLECTION).document(uid)
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)

        return firestore.runTransaction { transaction ->
            val claimsSnapshot = transaction.get(claimsRef)
            val claimedKeys = claimsSnapshot.toClaimedKeys()
            if (claimedKeys.containsKey(claimKey)) throw MissionAlreadyClaimedException()

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            val updatedProfile = currentProfile.copy(
                xp = currentProfile.xp + definition.reward.xp,
                coins = currentProfile.coins + definition.reward.coins,
            )

            val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
            definition.reward.inventoryGrants.forEach { (kind, amount) ->
                currentQuantities[kind] = (currentQuantities[kind] ?: 0) + amount
            }

            transaction.set(claimsRef, mapOf("claimedKeys" to (claimedKeys + (claimKey to clock.millis()))))
            transaction.set(profileRef, updatedProfile.toFirestoreMap())
            transaction.set(inventoryRef, currentQuantities.toFirestoreMap())

            MissionClaimResult(missionId, definition.reward, updatedProfile, Inventory(currentQuantities))
        }.await()
    }

    override suspend fun consumeInventoryItem(kind: InventoryItemKind, quantity: Int): Inventory {
        val uid = requireUid()
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)
        return firestore.runTransaction { transaction ->
            val current = transaction.get(inventoryRef).toQuantities().toMutableMap()
            val existing = current[kind] ?: 0
            if (existing < quantity) throw InsufficientInventoryException()
            current[kind] = existing - quantity
            transaction.set(inventoryRef, current.toFirestoreMap())
            Inventory(current)
        }.await()
    }

    private suspend fun requireUid(): String = playerIdentityManager.awaitUid()
        ?: throw IllegalStateException("FirestoreMissionRemoteSource used with no signed-in player - callers must gate on FirebaseAvailability + a resolved uid via MissionRepositoryImpl.activeRemoteSource first")

    private fun claimKeyFor(missionId: MissionId, periodKey: Long): String = "${missionId.name}_$periodKey"

    private fun DocumentSnapshot.toClaimedKeys(): Map<String, Long> {
        if (!exists()) return emptyMap()
        val raw = get("claimedKeys") as? Map<*, *> ?: return emptyMap()
        return raw.mapNotNull { (key, value) ->
            val stringKey = key as? String ?: return@mapNotNull null
            stringKey to ((value as? Number)?.toLong() ?: 0L)
        }.toMap()
    }

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

    /** Duplicated from [FirestoreProgressionRemoteSource]'s own private copy rather than shared -
     * same "three independent copies of the same small mapping" convention its doc already
     * documents for `coinsAwardedFor`. */
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
        )
    }

    private fun PlayerProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
        "xp" to xp,
        "coins" to coins,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "lastPlayedEpochDay" to lastPlayedEpochDay,
        "streakShields" to streakShields,
        "dailyChallengeWonAtEpochSecond" to dailyChallengeWonAtEpochSecond,
        "weeklyChallengeWonAtEpochSecond" to weeklyChallengeWonAtEpochSecond,
        "updatedAtEpochMs" to clock.millis(),
    )

    private companion object {
        const val PROFILES_COLLECTION = "playerProfiles"
        const val INVENTORY_COLLECTION = "inventory"
        const val MISSION_CLAIMS_COLLECTION = "missionClaims"
    }
}
