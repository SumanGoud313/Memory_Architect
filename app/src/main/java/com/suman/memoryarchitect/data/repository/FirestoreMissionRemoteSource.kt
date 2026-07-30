package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MemoryJourneyRules
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.MissionCatalog
import com.suman.memoryarchitect.domain.progression.MissionCategoryBonusCatalog
import com.suman.memoryarchitect.domain.progression.MysteryChestReward
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
 * - `missionCategoryBonusClaims/{uid}` - same shape, keyed `"${period}_${periodKey}"` instead - the
 *   per-period completion bonus's own double-grant guard. Deliberately its own document, not a
 *   piggybacked key on `missionClaims/{uid}` - a `"BONUS_..."`-shaped key would never parse as a
 *   real [MissionId], which every key in that other map is expected to.
 * - `missionRefreshState/{uid}` - the "pay to skip the countdown" override, see
 *   [MissionRefreshState]'s doc.
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
                journeyPoints = currentProfile.journeyPoints + journeyPointsForClaim(definition.period),
            )

            val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
            definition.reward.inventoryGrants.forEach { (kind, amount) ->
                currentQuantities[kind] = (currentQuantities[kind] ?: 0) + amount
            }

            transaction.set(claimsRef, mapOf("claimedKeys" to (claimedKeys + (claimKey to clock.millis()))))
            // lastWriteSource: no longer read by anything server-side (this project runs no Cloud
            // Function), kept purely as a manual-debugging aid for which write path last touched a
            // given profile.
            transaction.set(profileRef, updatedProfile.toFirestoreMap() + mapOf("lastWriteSource" to "claim_mission_reward"), SetOptions.merge())
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

    override suspend fun openMysteryChest(reward: MysteryChestReward): Pair<PlayerProfile, Inventory> {
        val uid = requireUid()
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)
        return firestore.runTransaction { transaction ->
            val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
            val owned = currentQuantities[InventoryItemKind.MYSTERY_CHEST] ?: 0
            if (owned < 1) throw InsufficientInventoryException()
            currentQuantities[InventoryItemKind.MYSTERY_CHEST] = owned - 1

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            val updatedProfile = currentProfile.copy(coins = currentProfile.coins + reward.coinsAwarded)

            // lastWriteSource: no longer read by anything server-side - see claimMissionReward's
            // identical comment above.
            transaction.set(profileRef, updatedProfile.toFirestoreMap() + mapOf("lastWriteSource" to "mystery_chest_open"), SetOptions.merge())
            transaction.set(inventoryRef, currentQuantities.toFirestoreMap())

            updatedProfile to Inventory(currentQuantities)
        }.await()
    }

    override suspend fun applyXpBoost(xpGranted: Long): Pair<PlayerProfile, Inventory> {
        val uid = requireUid()
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)
        return firestore.runTransaction { transaction ->
            val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
            val owned = currentQuantities[InventoryItemKind.XP_BOOST] ?: 0
            if (owned < 1) throw InsufficientInventoryException()
            currentQuantities[InventoryItemKind.XP_BOOST] = owned - 1

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            val updatedProfile = currentProfile.copy(xp = currentProfile.xp + xpGranted)

            // See openMysteryChest's own lastWriteSource comment above.
            transaction.set(profileRef, updatedProfile.toFirestoreMap() + mapOf("lastWriteSource" to "xp_boost_consumed"), SetOptions.merge())
            transaction.set(inventoryRef, currentQuantities.toFirestoreMap())

            updatedProfile to Inventory(currentQuantities)
        }.await()
    }

    override suspend fun claimCategoryBonus(period: MissionPeriod, periodKey: Long, coinsAwarded: Long, xpAwarded: Long): MissionCategoryBonusOutcome {
        val uid = requireUid()
        val bonus = MissionCategoryBonusCatalog.forPeriod(period) ?: throw MissionNotEligibleException("no category bonus for $period")
        if (coinsAwarded !in bonus.coinRange) throw MissionNotEligibleException("coinsAwarded $coinsAwarded outside the configured range for $period")
        if (xpAwarded != bonus.xp) throw MissionNotEligibleException("xpAwarded $xpAwarded does not match the configured xp for $period")
        val reward = MissionReward(coins = coinsAwarded, xp = xpAwarded, inventoryGrants = bonus.inventoryGrants)
        val bonusKey = bonusClaimKeyFor(period, periodKey)
        val bonusClaimsRef = firestore.collection(BONUS_CLAIMS_COLLECTION).document(uid)
        val claimsRef = firestore.collection(MISSION_CLAIMS_COLLECTION).document(uid)
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val inventoryRef = firestore.collection(INVENTORY_COLLECTION).document(uid)

        return firestore.runTransaction { transaction ->
            val bonusClaimedKeys = transaction.get(bonusClaimsRef).toClaimedKeys()
            if (bonusClaimedKeys.containsKey(bonusKey)) throw MissionAlreadyClaimedException()

            // Re-derived from the same claim records claimMissionReward itself writes - never
            // trusts the caller's belief that the whole set is done.
            val claimedKeys = transaction.get(claimsRef).toClaimedKeys()
            val activeIds = MissionCatalog.activeMissionIds(period, periodKey)
            val allClaimed = activeIds.isNotEmpty() && activeIds.all { claimedKeys.containsKey(claimKeyFor(it, periodKey)) }
            if (!allClaimed) throw MissionNotEligibleException("not every active $period mission is claimed for period $periodKey")

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            val updatedProfile = currentProfile.copy(
                xp = currentProfile.xp + reward.xp,
                coins = currentProfile.coins + reward.coins,
            )

            val currentQuantities = transaction.get(inventoryRef).toQuantities().toMutableMap()
            reward.inventoryGrants.forEach { (kind, amount) -> currentQuantities[kind] = (currentQuantities[kind] ?: 0) + amount }

            transaction.set(bonusClaimsRef, mapOf("claimedKeys" to (bonusClaimedKeys + (bonusKey to clock.millis()))))
            // See claimMissionReward's own lastWriteSource comment above.
            transaction.set(profileRef, updatedProfile.toFirestoreMap() + mapOf("lastWriteSource" to "claim_mission_category_bonus"), SetOptions.merge())
            transaction.set(inventoryRef, currentQuantities.toFirestoreMap())

            MissionCategoryBonusOutcome(reward, updatedProfile, Inventory(currentQuantities))
        }.await()
    }

    override suspend fun unlockAllMissionsEarly(dailyPeriodKey: Long, weeklyPeriodKey: Long, monthlyPeriodKey: Long): MissionRefreshOutcome {
        val uid = requireUid()
        val claimsRef = firestore.collection(MISSION_CLAIMS_COLLECTION).document(uid)
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val refreshStateRef = firestore.collection(REFRESH_STATE_COLLECTION).document(uid)
        val periods = listOf(
            MissionPeriod.DAILY to dailyPeriodKey,
            MissionPeriod.WEEKLY to weeklyPeriodKey,
            MissionPeriod.MONTHLY to monthlyPeriodKey,
        )

        return firestore.runTransaction { transaction ->
            val claimedKeys = transaction.get(claimsRef).toClaimedKeys()
            val allDone = periods.all { (period, periodKey) ->
                val activeIds = MissionCatalog.activeMissionIds(period, periodKey)
                activeIds.isNotEmpty() && activeIds.all { claimedKeys.containsKey(claimKeyFor(it, periodKey)) }
            }
            if (!allDone) throw MissionNotEligibleException("not every period is fully claimed yet")

            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            if (currentProfile.coins < UNLOCK_ALL_COST_COINS) throw InsufficientCoinsException()
            val updatedProfile = currentProfile.copy(coins = currentProfile.coins - UNLOCK_ALL_COST_COINS)

            val updatedRefreshState = MissionRefreshState(
                dailyForcedPeriodKey = MissionCatalog.nextDifferentPeriodKey(
                    MissionPeriod.DAILY, dailyPeriodKey, MissionCatalog.activeMissionIds(MissionPeriod.DAILY, dailyPeriodKey).toSet(),
                ),
                weeklyForcedPeriodKey = MissionCatalog.nextDifferentPeriodKey(
                    MissionPeriod.WEEKLY, weeklyPeriodKey, MissionCatalog.activeMissionIds(MissionPeriod.WEEKLY, weeklyPeriodKey).toSet(),
                ),
                monthlyForcedPeriodKey = MissionCatalog.nextDifferentPeriodKey(
                    MissionPeriod.MONTHLY, monthlyPeriodKey, MissionCatalog.activeMissionIds(MissionPeriod.MONTHLY, monthlyPeriodKey).toSet(),
                ),
            )

            // lastWriteSource: see claimMissionReward's own comment above.
            transaction.set(profileRef, updatedProfile.toFirestoreMap() + mapOf("lastWriteSource" to "mission_unlock_all"), SetOptions.merge())
            transaction.set(refreshStateRef, updatedRefreshState.toFirestoreMap(clock))

            MissionRefreshOutcome(updatedProfile, updatedRefreshState)
        }.await()
    }

    private suspend fun requireUid(): String = playerIdentityManager.awaitUid()
        ?: throw IllegalStateException("FirestoreMissionRemoteSource used with no signed-in player - callers must gate on FirebaseAvailability + a resolved uid via MissionRepositoryImpl.activeRemoteSource first")

    private fun claimKeyFor(missionId: MissionId, periodKey: Long): String = "${missionId.name}_$periodKey"

    private fun bonusClaimKeyFor(period: MissionPeriod, periodKey: Long): String = "${period.name}_$periodKey"

    private fun MissionRefreshState.toFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
        "dailyForcedPeriodKey" to dailyForcedPeriodKey,
        "weeklyForcedPeriodKey" to weeklyForcedPeriodKey,
        "monthlyForcedPeriodKey" to monthlyForcedPeriodKey,
        "updatedAtEpochMs" to clock.millis(),
    )

    /** A deliberate simplification of the plan doc's "weekly mission set completed" bonus -
     * see [MemoryJourneyRules]'s doc for why this scales by period rather than detecting a full
     * set. */
    private fun journeyPointsForClaim(period: MissionPeriod): Long {
        val rules = MemoryJourneyRules.Default
        return when (period) {
            MissionPeriod.DAILY -> rules.pointsPerDailyMissionClaimed
            MissionPeriod.WEEKLY -> rules.pointsPerWeeklyMissionClaimed
            MissionPeriod.MONTHLY -> rules.pointsPerMonthlyMissionClaimed
            MissionPeriod.EVENT -> rules.pointsPerEventMissionClaimed
        }
    }

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
            journeyPoints = getLong("journeyPoints") ?: 0L,
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
        "journeyPoints" to journeyPoints,
        "updatedAtEpochMs" to clock.millis(),
    )

    private companion object {
        const val PROFILES_COLLECTION = "playerProfiles"
        const val INVENTORY_COLLECTION = "inventory"
        const val MISSION_CLAIMS_COLLECTION = "missionClaims"
        const val BONUS_CLAIMS_COLLECTION = "missionCategoryBonusClaims"
        const val REFRESH_STATE_COLLECTION = "missionRefreshState"
        const val UNLOCK_ALL_COST_COINS = 1000L
    }
}
