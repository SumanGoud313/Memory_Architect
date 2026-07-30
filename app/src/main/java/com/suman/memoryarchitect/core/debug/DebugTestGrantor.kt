package com.suman.memoryarchitect.core.debug

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.billing.CosmeticCollectionGrantor
import com.suman.memoryarchitect.core.cosmetics.EquippedCosmeticsStore
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressEntity
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.RemoteConfigCacheEntity
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.core.notifications.DailyReminderWorker
import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.LevelCampaignRules
import com.suman.memoryarchitect.domain.progression.LiveEventCatalog
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
import com.suman.memoryarchitect.domain.repository.InventoryRepository
import com.suman.memoryarchitect.domain.repository.MissionRepository
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only testing convenience, never called from any release-build UI (every entry point into
 * this class is gated by `BuildConfig.DEBUG` at the call site, same convention as
 * `SettingsScreen`'s `AnalyticsDashboard` entry point) - and, as of this pass, every public method
 * *also* re-asserts `BuildConfig.DEBUG` for itself via [requireDebugBuild], so a future call site
 * that forgets the UI-level gate still can't make this class do anything in a release build.
 *
 * Writes directly to the same Firestore documents the real gameplay/purchase/claim flows use (see
 * [FirestoreShopRemoteSource], [FirestoreMissionRemoteSource], [FirestoreProgressionRemoteSource]),
 * then mirrors the change into the local Room cache so it's reflected immediately without waiting
 * for a fresh fetch. Every write goes through the exact same `firestore.rules` validation path a
 * real client write would - nothing here uses an Admin SDK shortcut or a rule exception carved out
 * for this class. The two exceptions ([debugResetDailyReward]/[debugResetReturningPlayer]) instead
 * *delete* their target documents outright (see [performFullProgressionReset]) - the same
 * `allow delete: if isOwner(uid)` rule the real "Reset Progress" feature uses - because there is no
 * client-*writable* path that can turn a real claim back into "never claimed."
 *
 * Both writes send the **full** document shape (every field `isValidProfile`/`isValidCosmetics` in
 * `firestore.rules` requires), never a partial [SetOptions.merge] of just the changed field - a
 * player who hasn't submitted a score or claimed a daily reward yet has no `playerProfiles/{uid}`
 * document at all, and a merge write containing only `coins` would create one missing `xp`/
 * `currentStreak`/etc., which `isValidProfile` rejects outright (a real bug this class hit and had
 * to be fixed for - the first version failed silently for exactly this reason on a fresh account).
 * [toFullFirestoreMap] previously omitted `streakShields`/`journeyPoints` too, both required
 * non-nullable by `isValidProfile` - the same class of bug, now fixed the same way.
 *
 * Firestore-only (not mock-backend) - both this app's debug and release builds ship
 * `google-services.json`, so [FirebaseAvailabilityProvider.isConfigured] is true and an anonymous
 * sign-in succeeds long before a player ever reaches the Shop, making Firestore the path actually
 * in use.
 */
@Singleton
class DebugTestGrantor @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val progressDao: PlayerProgressDao,
    private val ownedCosmeticDao: OwnedCosmeticDao,
    private val equippedCosmeticDao: EquippedCosmeticDao,
    private val equippedCosmeticsStore: EquippedCosmeticsStore,
    private val inventoryRepository: InventoryRepository,
    private val missionRepository: MissionRepository,
    private val levelCampaignProgressDao: LevelCampaignProgressDao,
    private val remoteConfigDao: RemoteConfigDao,
    private val debugLiveEventOverride: DebugLiveEventOverride,
    private val workManager: WorkManager,
    private val clock: Clock,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
    private val cosmeticCollectionGrantor: CosmeticCollectionGrantor,
) {
    private val firestore by lazy { Firebase.firestore }

    /** Every public method below calls this first, inside its own `runCatching` - a violation
     * surfaces as an ordinary [Result.failure] like any other precondition here, never a crash,
     * but it means none of this class's Firestore/WorkManager/local-cache side effects can ever
     * run outside a debug build even if a future call site forgets its own `BuildConfig.DEBUG`
     * check. */
    private fun requireDebugBuild() {
        check(BuildConfig.DEBUG) { "DebugTestGrantor must never execute outside a debug build" }
    }

    suspend fun grantTestCoins(amount: Long): Result<Long> = runCatching {
        requireDebugBuild()
        mutateProfile { it.copy(coins = it.coins + amount) }.coins
    }

    suspend fun grantTestXp(amount: Long): Result<Long> = runCatching {
        requireDebugBuild()
        mutateProfile { it.copy(xp = it.xp + amount) }.xp
    }

    /** Bounded to [MAX_JOURNEY_POINTS_GRANT_PER_CALL] per call - kept in line with what one real
     * scored round could plausibly grant, so a debug session's profile state stays realistic even
     * though nothing server-side enforces this bound (this project runs no Cloud Function - see
     * the Spark migration report). Call it more than once to grant more. */
    suspend fun grantTestJourneyPoints(amount: Long = MAX_JOURNEY_POINTS_GRANT_PER_CALL): Result<Long> = runCatching {
        requireDebugBuild()
        require(amount in 1..MAX_JOURNEY_POINTS_GRANT_PER_CALL) {
            "grantTestJourneyPoints is bounded to 1..$MAX_JOURNEY_POINTS_GRANT_PER_CALL per call"
        }
        mutateProfile { it.copy(journeyPoints = it.journeyPoints + amount) }.journeyPoints
    }

    /** Grants exactly one Streak Shield, capped at [MAX_STREAK_SHIELDS] - matches `StreakRules`'
     * own cap, same "stay realistic even with no server enforcing it" reasoning
     * [grantTestJourneyPoints]'s doc describes. */
    suspend fun grantTestStreakShield(): Result<Int> = runCatching {
        requireDebugBuild()
        mutateProfile { it.copy(streakShields = (it.streakShields + 1).coerceAtMost(MAX_STREAK_SHIELDS)) }.streakShields
    }

    /** Covers Hint/Redo/Rewatch tokens, Lucky Spin Tickets, Mystery Chests, and every other
     * [InventoryItemKind] - one generic grant rather than one method per kind, since the write
     * shape (`inventory/{uid}`'s `quantities` map) is identical for all of them. [quantity]
     * defaults to, and is hard-capped at, [MAX_INVENTORY_GRANT_PER_CALL] - same "stay realistic
     * even with no server enforcing it" reasoning [grantTestJourneyPoints]'s doc describes. Call it
     * more than once to grant more of the same kind. */
    suspend fun grantInventoryItem(kind: InventoryItemKind, quantity: Int = MAX_INVENTORY_GRANT_PER_CALL): Result<Int> = runCatching {
        requireDebugBuild()
        check(firebaseAvailabilityProvider.isConfigured) { "Firebase isn't configured - no server inventory to grant on" }
        require(quantity in 1..MAX_INVENTORY_GRANT_PER_CALL) {
            "grantInventoryItem is bounded to 1..$MAX_INVENTORY_GRANT_PER_CALL per call"
        }
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val ref = firestore.collection("inventory").document(uid)
        val updatedQuantities = firestore.runTransaction { transaction ->
            val current = transaction.get(ref).toQuantities()
            val updated = current + (kind to (current[kind] ?: 0) + quantity)
            transaction.set(
                ref,
                mapOf("quantities" to updated.mapKeys { it.key.name }, "updatedAtEpochMs" to clock.millis()),
                SetOptions.merge(),
            )
            updated
        }.await()
        inventoryRepository.cacheInventory(Inventory(updatedQuantities))
        updatedQuantities[kind] ?: 0
    }

    suspend fun unlockAllCosmeticsForTesting(): Result<Unit> = runCatching {
        requireDebugBuild()
        check(firebaseAvailabilityProvider.isConfigured) { "Firebase isn't configured - no server profile to unlock cosmetics on" }
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
        requireDebugBuild()
        check(firebaseAvailabilityProvider.isConfigured) { "Firebase isn't configured - no server profile to lock cosmetics on" }
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

    /** Unlocks every Classic campaign level (1..[LevelCampaignRules.maxLevel]) for testing - a
     * direct Room write, unlike every grant above: [com.suman.memoryarchitect.data.repository.LevelCampaignRepositoryImpl]'s
     * own doc notes campaign progress is local-only, with no Firestore counterpart to sync against
     * at all, so there's no server document to mirror here. */
    suspend fun unlockAllLevelsForTesting(): Result<Unit> = runCatching {
        requireDebugBuild()
        levelCampaignProgressDao.upsert(LevelCampaignProgressEntity(maxUnlockedLevel = LevelCampaignRules.Default.maxLevel))
    }

    /** Developer Test Mode - simulates a successful Premium Shop purchase without any real Google
     * Play Billing transaction, delegating to the exact same [CosmeticCollectionGrantor] a real
     * purchase in `core.billing.BillingManagerImpl` uses - one grant path, not two independently-
     * maintained copies. Also this class's answer to "Cosmetic Tokens" - this codebase has no
     * separate cosmetic-token currency (cosmetics are owned/unowned, not bought with a token type),
     * so the real grant primitives are this method and [unlockAllCosmeticsForTesting]/
     * [lockAllCosmeticsForTesting] above.
     *
     * [replayGuardToken] is a synthetic, per-call value (never a real Play purchase token, since
     * none exists here) - it still lands in `claimedPurchaseTokens` the same way a real purchase's
     * token would, just for record-keeping/idempotency rather than fraud-prevention, since this path
     * is already `BuildConfig.DEBUG`-gated twice over (see class doc). */
    suspend fun debugGrantPremiumProduct(productId: String): Result<Unit> = runCatching {
        requireDebugBuild()
        val product = PremiumShopCatalog.productOrNull(productId) ?: error("Unknown premium product: $productId")
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val replayGuardToken = "debug-grant-$uid-$productId-${clock.millis()}"
        cosmeticCollectionGrantor.grant(uid, product, replayGuardToken)
    }

    /** Fast-forwards [mission] to its claimable target by replaying the exact real
     * [MissionRepository.recordMissionEvent] path a genuine player action would - never a direct
     * Room/Firestore write, since mission progress is local-only, trusted state (see
     * [MissionRepository]'s own doc) that this method has no need to special-case. Amount-based
     * requirements (coins/stars earned) close the remaining gap in one call; every other
     * requirement replays its one matching unit event [remaining] times. No-op if already claimed
     * or already complete. */
    suspend fun debugCompleteMission(mission: ActiveMission, todayEpochDay: Long): Result<Unit> = runCatching {
        requireDebugBuild()
        val remaining = (mission.definition.targetCount - mission.currentCount).coerceAtLeast(0)
        if (remaining <= 0 || mission.claimed) return@runCatching
        when (val requirement = mission.definition.requirement) {
            MissionRequirementType.EARN_COINS -> missionRepository.recordMissionEvent(MissionEvent.CoinsEarned(remaining.toLong()), todayEpochDay)
            MissionRequirementType.EARN_STARS -> missionRepository.recordMissionEvent(MissionEvent.StarsEarned(remaining), todayEpochDay)
            else -> {
                val event = unitEventFor(requirement)
                repeat(remaining) { missionRepository.recordMissionEvent(event, todayEpochDay) }
            }
        }
    }

    /** Both reset actions below share this one reset - direct client-side deletes of
     * `playerProfiles`/`dailyRewards`/`inventory`/`missionClaims`/`returningPlayerGifts`, the same
     * 5 collections and same `allow delete: if isOwner(uid)` rule
     * [LocalProgressResetRepositoryImpl]'s real "Reset Progress" feature uses - there's no way to
     * narrow this to just `dailyRewards/{uid}` or `returningPlayerGifts/{uid}` alone (a client write
     * can only ever look like a real, eligible claim, never "never claimed"), so this resets all 5,
     * not just the one collection the caller asked about - see [debugResetDailyReward]/
     * [debugResetReturningPlayer]'s own docs, this is documented there so a tester isn't surprised
     * by the broader scope. */
    private suspend fun performFullProgressionReset() {
        requireDebugBuild()
        check(firebaseAvailabilityProvider.isConfigured) { "Firebase isn't configured - nothing to reset" }
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        listOf("playerProfiles", "dailyRewards", "inventory", "missionClaims", "returningPlayerGifts")
            .map { collection -> firestore.collection(collection).document(uid).delete() }
            .forEach { it.await() }
    }

    /** See [performFullProgressionReset]'s doc - this also resets profile/inventory/missions, not
     * only the Daily Check-in cycle, since that is the only rules-respecting reset primitive that
     * exists today. */
    suspend fun debugResetDailyReward(): Result<Unit> = runCatching { performFullProgressionReset() }

    /** See [performFullProgressionReset]'s doc - same full-scope caveat as [debugResetDailyReward]. */
    suspend fun debugResetReturningPlayer(): Result<Unit> = runCatching { performFullProgressionReset() }

    /** Forces [DailyReminderWorker] to run right now instead of waiting for its scheduled window -
     * a one-time request under its own unique-work name ([DEBUG_NOTIFICATION_WORK_NAME]), entirely
     * separate from [com.suman.memoryarchitect.core.notifications.NotificationScheduler]'s real
     * periodic chain, so triggering this can never disturb or re-delay the real schedule. Runs the
     * worker's own real eligibility checks unmodified (already played today, category toggles,
     * notification permission) - if none of those hold today, this correctly no-ops exactly like
     * the real scheduled run would, rather than forcing a notification the real logic wouldn't
     * have sent. */
    fun debugTriggerNotification(): Result<Unit> = runCatching {
        requireDebugBuild()
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
        workManager.enqueueUniqueWork(DEBUG_NOTIFICATION_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Sets [debugLiveEventOverride] to preview any of the 9 real event templates, regardless of
     * that template's own date window and without touching the real Firebase Remote Config
     * project at all - both [com.suman.memoryarchitect.domain.usecase.GetActiveLiveEventUseCase]
     * and [MissionRepository]'s own event resolution check that override first, ahead of Remote
     * Config, specifically so it can't be silently clobbered by the next live fetch (see
     * [DebugLiveEventOverride]'s doc for the exact bug this replaced - a first version wrote only
     * to the Room Remote Config cache, which looked identical to "nothing happened" because the
     * very next live fetch overwrote it before a tester ever saw it). Also mirrors the write into
     * that same Room cache for any incidental direct reader, though nothing in this app's
     * production code path actually needs it now. [eventId] must name a real template in
     * [LiveEventCatalog.events] - a typo is rejected up front rather than silently overriding to an
     * id nothing resolves, the same "no event" result a real unrecognized id would produce anyway. */
    suspend fun debugTriggerSeasonalEvent(eventId: String): Result<Unit> = runCatching {
        requireDebugBuild()
        require(LiveEventCatalog.events.any { it.id == eventId }) { "Unknown live event id: $eventId" }
        debugLiveEventOverride.activeEventId = eventId
        remoteConfigDao.upsert(listOf(RemoteConfigCacheEntity("event_active_id", eventId, clock.millis())))
    }

    /** Clears [debugLiveEventOverride], falling back to whatever the real Remote Config project's
     * own `event_active_id` last resolved to. */
    suspend fun debugClearSeasonalEventOverride(): Result<Unit> = runCatching {
        requireDebugBuild()
        debugLiveEventOverride.activeEventId = null
        remoteConfigDao.upsert(listOf(RemoteConfigCacheEntity("event_active_id", "", clock.millis())))
    }

    private fun unitEventFor(requirement: MissionRequirementType): MissionEvent = when (requirement) {
        MissionRequirementType.COMPLETE_LEVELS -> MissionEvent.LevelCompleted
        MissionRequirementType.COMPLETE_PRACTICE_ROUNDS -> MissionEvent.PracticeRoundCompleted
        MissionRequirementType.COMPLETE_DAILY_CHALLENGE -> MissionEvent.DailyChallengeWon
        MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE -> MissionEvent.WeeklyChallengeWon
        MissionRequirementType.ZERO_HINT_LEVEL_CLEAR -> MissionEvent.ZeroHintLevelClear
        MissionRequirementType.HIGH_ACCURACY_CLEAR -> MissionEvent.HighAccuracyClear
        MissionRequirementType.UNLOCK_COSMETIC -> MissionEvent.CosmeticUnlocked
        MissionRequirementType.EQUIP_COSMETIC -> MissionEvent.CosmeticEquipped
        MissionRequirementType.WATCH_REWARDED_AD -> MissionEvent.RewardedAdWatched
        MissionRequirementType.EARN_COINS, MissionRequirementType.EARN_STARS ->
            error("$requirement is amount-based - handled directly in debugCompleteMission, never via unitEventFor")
    }

    /** Shared read-mutate-write for every profile field this class grants (coins/xp/journeyPoints/
     * streakShields) - one Firestore transaction, one local-cache mirror update, so
     * [grantTestCoins]/[grantTestXp]/[grantTestJourneyPoints]/[grantTestStreakShield] don't each
     * repeat the same transaction/cache-upsert boilerplate. */
    private suspend fun mutateProfile(mutate: (PlayerProfile) -> PlayerProfile): PlayerProfile {
        check(firebaseAvailabilityProvider.isConfigured) { "Firebase isn't configured - no server profile to grant on" }
        val uid = playerIdentityManager.awaitUid() ?: error("No signed-in player yet - try again in a moment")
        val ref = firestore.collection("playerProfiles").document(uid)
        val updated = firestore.runTransaction { transaction ->
            val current = transaction.get(ref).toProfile()
            val next = mutate(current)
            transaction.set(ref, next.toFullFirestoreMap(clock), SetOptions.merge())
            next
        }.await()
        val cached = progressDao.get() ?: PlayerProgressCacheEntity(
            xp = 0L, coins = 0L, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null,
            lastSyncedAt = System.currentTimeMillis(),
        )
        progressDao.upsert(
            cached.copy(
                xp = updated.xp,
                coins = updated.coins,
                streakShields = updated.streakShields,
                journeyPoints = updated.journeyPoints,
                lastSyncedAt = System.currentTimeMillis(),
            ),
        )
        return updated
    }

    private fun DocumentSnapshot.toProfile(): PlayerProfile {
        if (!exists()) return PlayerProfile.EMPTY
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

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toQuantities(): Map<InventoryItemKind, Int> {
        if (!exists()) return emptyMap()
        val raw = get("quantities") as? Map<*, *> ?: return emptyMap()
        return raw.mapNotNull { (key, value) ->
            val kind = runCatching { InventoryItemKind.valueOf(key as String) }.getOrNull() ?: return@mapNotNull null
            kind to ((value as? Number)?.toInt() ?: 0)
        }.toMap()
    }

    /** Every field [isValidProfile][firestore.rules] requires, present every time - see the class
     * doc for why a partial merge of just one field is unsafe on a not-yet-existing document.
     * Previously omitted `streakShields`/`journeyPoints` - both required non-nullable by
     * `isValidProfile` - which rejected this class's very first grant on any account with no prior
     * `playerProfiles/{uid}` document (nothing to merge onto), contradicting this class's whole
     * purpose. Fixed by including them here like every other field already was. */
    private fun PlayerProfile.toFullFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
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
        const val DEBUG_NOTIFICATION_WORK_NAME = "debug_trigger_daily_reminder"

        // See grantTestJourneyPoints's doc.
        const val MAX_JOURNEY_POINTS_GRANT_PER_CALL = 500L

        // Mirrors StreakRules' own cap - see grantTestStreakShield's doc.
        const val MAX_STREAK_SHIELDS = 3

        // See grantInventoryItem's doc.
        const val MAX_INVENTORY_GRANT_PER_CALL = 5
    }
}
