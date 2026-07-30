package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.MissionProgressDao
import com.suman.memoryarchitect.core.database.MissionProgressEntity
import com.suman.memoryarchitect.core.database.MissionRefreshStateDao
import com.suman.memoryarchitect.core.database.MissionRefreshStateEntity
import com.suman.memoryarchitect.core.database.PendingMissionClaimDao
import com.suman.memoryarchitect.core.database.PendingMissionClaimEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.RemoteConfigCacheEntity
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.core.sync.PendingMissionClaimSyncScheduler
import com.suman.memoryarchitect.data.remote.MissionApi
import com.suman.memoryarchitect.data.remote.dto.ApplyXpBoostRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimCategoryBonusRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimCategoryBonusResponseDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardResponseDto
import com.suman.memoryarchitect.data.remote.dto.ConsumeInventoryItemRequestDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
import com.suman.memoryarchitect.data.remote.dto.InventoryEconomyResponseDto
import com.suman.memoryarchitect.data.remote.dto.OpenMysteryChestRequestDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.data.remote.dto.UnlockAllMissionsEarlyRequestDto
import com.suman.memoryarchitect.data.remote.dto.UnlockAllMissionsEarlyResponseDto
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.MissionCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Forces the mock-backend path deterministically, independent of whether this machine's local
 * `app/google-services.json` happens to exist - see [FirebaseAvailabilityProvider]'s doc. */
private class FakeMissionFirebaseAvailabilityProvider(override val isConfigured: Boolean = false) : FirebaseAvailabilityProvider

private object MissionRepoNoopCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}

private class FakeMissionApi(
    private val inventory: InventoryDto = InventoryDto(),
    private val claimResponse: ClaimMissionRewardResponseDto? = null,
    private val error: Throwable? = null,
) : MissionApi {
    var lastClaimRequest: ClaimMissionRewardRequestDto? = null

    override suspend fun getInventory(): InventoryDto {
        error?.let { throw it }
        return inventory
    }

    override suspend fun claimMissionReward(body: ClaimMissionRewardRequestDto): ClaimMissionRewardResponseDto {
        lastClaimRequest = body
        error?.let { throw it }
        return requireNotNull(claimResponse)
    }

    override suspend fun consumeInventoryItem(body: ConsumeInventoryItemRequestDto): InventoryDto {
        error?.let { throw it }
        return inventory
    }

    override suspend fun openMysteryChest(body: OpenMysteryChestRequestDto): InventoryEconomyResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")

    override suspend fun applyXpBoost(body: ApplyXpBoostRequestDto): InventoryEconomyResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")

    override suspend fun claimCategoryBonus(body: ClaimCategoryBonusRequestDto): ClaimCategoryBonusResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")

    override suspend fun unlockAllMissionsEarly(body: UnlockAllMissionsEarlyRequestDto): UnlockAllMissionsEarlyResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")
}

private class FakeMissionProgressDao : MissionProgressDao {
    private val stored = mutableMapOf<String, MissionProgressEntity>()
    override suspend fun getAll(): List<MissionProgressEntity> = stored.values.toList()
    override suspend fun get(missionId: String): MissionProgressEntity? = stored[missionId]
    override suspend fun upsert(entity: MissionProgressEntity) {
        stored[entity.missionId] = entity
    }
    override suspend fun clearAll() = stored.clear()
}

private class FakeInventoryItemDao : InventoryItemDao {
    private val stored = mutableMapOf<String, InventoryItemEntity>()
    override suspend fun getAll(): List<InventoryItemEntity> = stored.values.toList()
    override suspend fun get(kind: String): InventoryItemEntity? = stored[kind]
    override suspend fun upsert(entity: InventoryItemEntity) {
        stored[entity.kind] = entity
    }
    override suspend fun clearAll() = stored.clear()
}

private class FakeMissionPlayerProgressDao : PlayerProgressDao {
    private var stored: PlayerProgressCacheEntity? = null
    override suspend fun get(): PlayerProgressCacheEntity? = stored
    override suspend fun upsert(entity: PlayerProgressCacheEntity) {
        stored = entity
    }
    override suspend fun clearAll() {
        stored = null
    }
}

/** Empty by default (the "cold cache, no event" case) - populate via [seed] to simulate a
 * previously-fetched Remote Config, exactly as [com.suman.memoryarchitect.data.repository.MissionRepositoryImpl.activeEvent]
 * reads it. */
private class FakeMissionRemoteConfigDao : RemoteConfigDao {
    private val store = mutableMapOf<String, RemoteConfigCacheEntity>()
    fun seed(values: Map<String, String>) {
        values.forEach { (key, value) -> store[key] = RemoteConfigCacheEntity(key, value, fetchedAt = 0L) }
    }
    override suspend fun getByKey(key: String) = store[key]
    override suspend fun getAll() = store.values.toList()
    override suspend fun upsert(entities: List<RemoteConfigCacheEntity>) {
        entities.forEach { store[it.configKey] = it }
    }
    override suspend fun deleteFetchedBefore(olderThan: Long) {
        store.entries.removeAll { it.value.fetchedAt < olderThan }
    }
}

/** Empty by default (the common "nothing queued" case) - insert()/getAll() mirror
 * [PendingMissionClaimDao]'s real Room-backed contract closely enough for these tests (oldest
 * first, delete removes exactly the matching row). */
private class FakePendingMissionClaimDao : PendingMissionClaimDao {
    private val stored = mutableListOf<PendingMissionClaimEntity>()
    private var nextId = 1L
    override suspend fun insert(entity: PendingMissionClaimEntity): Long {
        val withId = entity.copy(localId = nextId++)
        stored += withId
        return withId.localId
    }
    override suspend fun getAll(): List<PendingMissionClaimEntity> = stored.sortedBy { it.createdAt }
    override suspend fun delete(entity: PendingMissionClaimEntity) {
        stored.removeAll { it.localId == entity.localId }
    }
    override suspend fun clearAll() = stored.clear()
    fun sizeForTest(): Int = stored.size
}

private class FakePendingMissionClaimSyncScheduler : PendingMissionClaimSyncScheduler {
    var scheduleRetryCallCount = 0
        private set
    override fun scheduleRetry() {
        scheduleRetryCallCount++
    }
}

private class FakeMissionRefreshStateDao : MissionRefreshStateDao {
    private var stored: MissionRefreshStateEntity? = null
    override suspend fun get(): MissionRefreshStateEntity? = stored
    override suspend fun upsert(entity: MissionRefreshStateEntity) {
        stored = entity
    }
}

/** Never signed in - forces [MissionRepositoryImpl.activeRemoteSource] to fall back to the
 * mock-backend path every time, same reasoning as [ProgressionRepositoryImplTest]'s own copy. */
private class FakeMissionPlayerIdentityManager : PlayerIdentityManager {
    override val uid: StateFlow<String?> = MutableStateFlow(null)
    override val isVerified: StateFlow<Boolean> = MutableStateFlow(false)
    override val displayName: StateFlow<String?> = MutableStateFlow(null)
    override val photoUrl: StateFlow<String?> = MutableStateFlow(null)
    override fun ensureSignedIn() = Unit
    override suspend fun awaitUid(timeoutMs: Long): String? = null
    override suspend fun linkWithGoogle(idToken: String): Result<Unit> = Result.failure(UnsupportedOperationException("not exercised by these tests"))
    override suspend fun signOutAfterAccountDeletion() = Unit
}

/** The requirement types that advance by a flat +1 per event, independent of any event payload -
 * used to find a deterministic (day, mission) pair to test against without hardcoding which three
 * of the nine daily missions happen to be active on a given day. */
private val flatIncrementRequirements = setOf(
    MissionRequirementType.COMPLETE_LEVELS,
    MissionRequirementType.COMPLETE_PRACTICE_ROUNDS,
    MissionRequirementType.COMPLETE_DAILY_CHALLENGE,
    MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE,
    MissionRequirementType.ZERO_HINT_LEVEL_CLEAR,
    MissionRequirementType.HIGH_ACCURACY_CLEAR,
    MissionRequirementType.UNLOCK_COSMETIC,
    MissionRequirementType.EQUIP_COSMETIC,
    MissionRequirementType.WATCH_REWARDED_AD,
)

private fun eventFor(requirement: MissionRequirementType): MissionEvent = when (requirement) {
    MissionRequirementType.COMPLETE_LEVELS -> MissionEvent.LevelCompleted
    MissionRequirementType.COMPLETE_PRACTICE_ROUNDS -> MissionEvent.PracticeRoundCompleted
    MissionRequirementType.COMPLETE_DAILY_CHALLENGE -> MissionEvent.DailyChallengeWon
    MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE -> MissionEvent.WeeklyChallengeWon
    MissionRequirementType.ZERO_HINT_LEVEL_CLEAR -> MissionEvent.ZeroHintLevelClear
    MissionRequirementType.HIGH_ACCURACY_CLEAR -> MissionEvent.HighAccuracyClear
    MissionRequirementType.UNLOCK_COSMETIC -> MissionEvent.CosmeticUnlocked
    MissionRequirementType.EQUIP_COSMETIC -> MissionEvent.CosmeticEquipped
    MissionRequirementType.WATCH_REWARDED_AD -> MissionEvent.RewardedAdWatched
    else -> error("no flat-increment event mapped for $requirement")
}

/** Deterministically finds the first day (within a generous search range) on which at least one
 * active Daily mission has a flat +1 requirement - see [flatIncrementRequirements]'s doc. */
private fun findDayWithFlatIncrementMission(): Long {
    for (day in 0L until 500L) {
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, day)
        val hasMatch = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey)
            .any { MissionCatalog.definitionFor(it).requirement in flatIncrementRequirements }
        if (hasMatch) return day
    }
    error("no day found in range with a flat-increment active mission - unexpected given MissionCatalog's pool")
}

class MissionRepositoryImplTest {

    private fun buildRepository(
        api: MissionApi,
        missionProgressDao: MissionProgressDao = FakeMissionProgressDao(),
        inventoryItemDao: InventoryItemDao = FakeInventoryItemDao(),
        playerProgressDao: PlayerProgressDao = FakeMissionPlayerProgressDao(),
        remoteConfigDao: RemoteConfigDao = FakeMissionRemoteConfigDao(),
        pendingMissionClaimDao: PendingMissionClaimDao = FakePendingMissionClaimDao(),
        pendingMissionClaimSyncScheduler: PendingMissionClaimSyncScheduler = FakePendingMissionClaimSyncScheduler(),
        missionRefreshStateDao: MissionRefreshStateDao = FakeMissionRefreshStateDao(),
        clock: Clock = Clock.systemUTC(),
    ) = MissionRepositoryImpl(
        mockBackendSource = MockBackendMissionRemoteSource(api),
        firestoreSource = FirestoreMissionRemoteSource(FakeMissionPlayerIdentityManager(), Clock.systemUTC()),
        playerIdentityManager = FakeMissionPlayerIdentityManager(),
        missionProgressDao = missionProgressDao,
        inventoryItemDao = inventoryItemDao,
        playerProgressDao = playerProgressDao,
        remoteConfigDao = remoteConfigDao,
        pendingMissionClaimDao = pendingMissionClaimDao,
        pendingMissionClaimSyncScheduler = pendingMissionClaimSyncScheduler,
        missionRefreshStateDao = missionRefreshStateDao,
        clock = clock,
        dispatchers = ImmediateDispatcherProvider,
        errorMapper = ErrorMapper(MissionRepoNoopCrashReporter),
        firebaseAvailabilityProvider = FakeMissionFirebaseAvailabilityProvider(),
    )

    @Test
    fun `getActiveMissions returns the deterministic catalog set with zero progress by default`() = runTest {
        val repository = buildRepository(api = FakeMissionApi())
        val todayEpochDay = 1000L

        val missions = repository.getActiveMissions(todayEpochDay)

        assertEquals(9, missions.size) // 3 daily + 3 weekly + 3 monthly
        assertTrue(missions.all { it.currentCount == 0 && !it.claimed })
    }

    @Test
    fun `recordMissionEvent increments progress for a matching active mission`() = runTest {
        val progressDao = FakeMissionProgressDao()
        val repository = buildRepository(api = FakeMissionApi(), missionProgressDao = progressDao)
        val day = findDayWithFlatIncrementMission()
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, day)
        val targetId = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey)
            .first { MissionCatalog.definitionFor(it).requirement in flatIncrementRequirements }
        val event = eventFor(MissionCatalog.definitionFor(targetId).requirement)

        repository.recordMissionEvent(event, day)

        val updated = repository.getActiveMissions(day).first { it.definition.id == targetId }
        assertEquals(1, updated.currentCount)
    }

    @Test
    fun `recordMissionEvent does not affect a mission whose requirement does not match the event`() = runTest {
        val repository = buildRepository(api = FakeMissionApi())
        val day = findDayWithFlatIncrementMission()
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, day)
        val targetId = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey)
            .first { MissionCatalog.definitionFor(it).requirement in flatIncrementRequirements }
        val mismatchedRequirement = flatIncrementRequirements.first { it != MissionCatalog.definitionFor(targetId).requirement }

        repository.recordMissionEvent(eventFor(mismatchedRequirement), day)

        val updated = repository.getActiveMissions(day).first { it.definition.id == targetId }
        assertEquals(0, updated.currentCount)
    }

    @Test
    fun `claimMissionReward on success marks the mission claimed and caches the profile`() = runTest {
        val progressDao = FakeMissionProgressDao()
        val playerProgressDao = FakeMissionPlayerProgressDao()
        val day = findDayWithFlatIncrementMission()
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, day)
        val targetId = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey)
            .first { MissionCatalog.definitionFor(it).requirement in flatIncrementRequirements }
        val definition = MissionCatalog.definitionFor(targetId)
        val response = ClaimMissionRewardResponseDto(
            missionId = targetId.name,
            coinsAwarded = definition.reward.coins,
            xpAwarded = definition.reward.xp,
            profile = PlayerProfileDto(
                xp = definition.reward.xp, coins = definition.reward.coins, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null,
                // The real point value is computed server-side (mock-backend/missions.js, not
                // exercised by this JVM test) - this stands in for whatever it returns, so this
                // test only verifies MissionRepositoryImpl propagates/caches it, same as coins/xp.
                journeyPoints = 5L,
            ),
            inventory = InventoryDto(),
        )
        val repository = buildRepository(
            api = FakeMissionApi(claimResponse = response),
            missionProgressDao = progressDao,
            playerProgressDao = playerProgressDao,
        )
        // Advance progress to the target so the claim is for an already-complete mission.
        repeat(definition.targetCount) { repository.recordMissionEvent(eventFor(definition.requirement), day) }

        val result = repository.claimMissionReward(targetId, day)

        assertTrue(result is Outcome.Success)
        assertEquals(definition.reward.coins, playerProgressDao.get()?.coins)
        assertEquals(5L, playerProgressDao.get()?.journeyPoints)
        val afterClaim = repository.getActiveMissions(day).first { it.definition.id == targetId }
        assertTrue(afterClaim.claimed)
    }

    @Test
    fun `claimMissionReward maps an already-claimed rejection to a routine 409`() = runTest {
        val repository = buildRepository(api = FakeMissionApi(error = MissionAlreadyClaimedException()))

        val result = repository.claimMissionReward(MissionCatalog.definitions.first().id, todayEpochDay = 1L)

        assertTrue(result is Outcome.Error)
        val error = (result as Outcome.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(409, (error as AppError.Server).code)
    }

    @Test
    fun `claimMissionReward maps a not-eligible rejection to a 400`() = runTest {
        val repository = buildRepository(api = FakeMissionApi(error = MissionNotEligibleException("progress incomplete")))

        val result = repository.claimMissionReward(MissionCatalog.definitions.first().id, todayEpochDay = 1L)

        assertTrue(result is Outcome.Error)
        val error = (result as Outcome.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(400, (error as AppError.Server).code)
    }

    @Test
    fun `claimMissionReward with a generic network failure queues the claim and schedules a retry`() = runTest {
        val pendingDao = FakePendingMissionClaimDao()
        val scheduler = FakePendingMissionClaimSyncScheduler()
        val repository = buildRepository(
            api = FakeMissionApi(error = RuntimeException("network down")),
            pendingMissionClaimDao = pendingDao,
            pendingMissionClaimSyncScheduler = scheduler,
        )
        val missionId = MissionCatalog.definitions.first().id

        val result = repository.claimMissionReward(missionId, todayEpochDay = 1L)

        assertTrue(result is Outcome.Error)
        assertEquals(1, pendingDao.sizeForTest())
        assertEquals(missionId.name, pendingDao.getAll().single().missionId)
        assertEquals(1, scheduler.scheduleRetryCallCount)
    }

    @Test
    fun `retryPendingClaims flushes a queued claim on success and drops it`() = runTest {
        val progressDao = FakeMissionProgressDao()
        val playerProgressDao = FakeMissionPlayerProgressDao()
        val pendingDao = FakePendingMissionClaimDao()
        val day = findDayWithFlatIncrementMission()
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, day)
        val targetId = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey)
            .first { MissionCatalog.definitionFor(it).requirement in flatIncrementRequirements }
        val definition = MissionCatalog.definitionFor(targetId)
        val response = ClaimMissionRewardResponseDto(
            missionId = targetId.name,
            coinsAwarded = definition.reward.coins,
            xpAwarded = definition.reward.xp,
            profile = PlayerProfileDto(
                xp = definition.reward.xp, coins = definition.reward.coins, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null,
            ),
            inventory = InventoryDto(),
        )
        val repository = buildRepository(
            api = FakeMissionApi(claimResponse = response),
            missionProgressDao = progressDao,
            playerProgressDao = playerProgressDao,
            pendingMissionClaimDao = pendingDao,
        )
        // Simulates the state right after the original claim failed offline: progress already at
        // target, unclaimed, and one entry sitting in the queue for this exact (mission, period).
        progressDao.upsert(MissionProgressEntity(targetId.name, periodKey, currentCount = definition.targetCount, claimed = false))
        pendingDao.insert(PendingMissionClaimEntity(missionId = targetId.name, periodKey = periodKey, progressCount = definition.targetCount, createdAt = 500L))

        repository.retryPendingClaims()

        assertEquals(0, pendingDao.sizeForTest())
        assertTrue(progressDao.get(targetId.name)?.claimed == true)
        assertEquals(definition.reward.coins, playerProgressDao.get()?.coins)
    }

    @Test
    fun `retryPendingClaims drops a claim that is no longer eligible instead of retrying forever`() = runTest {
        val pendingDao = FakePendingMissionClaimDao()
        val repository = buildRepository(
            api = FakeMissionApi(error = MissionNotEligibleException("period rotated away")),
            pendingMissionClaimDao = pendingDao,
        )
        val missionId = MissionCatalog.definitions.first().id
        pendingDao.insert(PendingMissionClaimEntity(missionId = missionId.name, periodKey = 1L, progressCount = 1, createdAt = 500L))

        repository.retryPendingClaims()

        assertEquals(0, pendingDao.sizeForTest())
    }

    @Test
    fun `getActiveMissions omits the Event pool when no event is live`() = runTest {
        val repository = buildRepository(api = FakeMissionApi(), remoteConfigDao = FakeMissionRemoteConfigDao())

        val missions = repository.getActiveMissions(todayEpochDay = 1000L)

        assertTrue(missions.none { it.definition.period == MissionPeriod.EVENT })
    }

    @Test
    fun `getActiveMissions includes all three Event missions while an event is live`() = runTest {
        val remoteConfigDao = FakeMissionRemoteConfigDao().apply {
            seed(mapOf("event_active_id" to "HALLOWEEN", "event_start_epoch" to "1000", "event_end_epoch" to "2000"))
        }
        val fixedClock = Clock.fixed(Instant.ofEpochSecond(1500), ZoneOffset.UTC)
        val repository = buildRepository(api = FakeMissionApi(), remoteConfigDao = remoteConfigDao, clock = fixedClock)

        val missions = repository.getActiveMissions(todayEpochDay = 1000L)

        val eventMissions = missions.filter { it.definition.period == MissionPeriod.EVENT }
        assertEquals(3, eventMissions.size)
        assertTrue(eventMissions.all { it.periodKey == 1000L })
    }

    @Test
    fun `recordMissionEvent advances Event mission progress only while the event is live`() = runTest {
        val progressDao = FakeMissionProgressDao()
        val remoteConfigDao = FakeMissionRemoteConfigDao().apply {
            seed(mapOf("event_active_id" to "HALLOWEEN", "event_start_epoch" to "1000", "event_end_epoch" to "2000"))
        }
        val fixedClock = Clock.fixed(Instant.ofEpochSecond(1500), ZoneOffset.UTC)
        val repository = buildRepository(api = FakeMissionApi(), missionProgressDao = progressDao, remoteConfigDao = remoteConfigDao, clock = fixedClock)

        repository.recordMissionEvent(MissionEvent.LevelCompleted, todayEpochDay = 1000L)

        val updated = repository.getActiveMissions(todayEpochDay = 1000L).first { it.definition.id == MissionId.EVENT_CLEAR_FIVE_LEVELS }
        assertEquals(1, updated.currentCount)
    }

    @Test
    fun `recordMissionEvent is a no-op once the mission is already claimed`() = runTest {
        val progressDao = FakeMissionProgressDao()
        val day = findDayWithFlatIncrementMission()
        val periodKey = MissionCatalog.periodKeyFor(MissionPeriod.DAILY, day)
        val targetId = MissionCatalog.activeMissionIds(MissionPeriod.DAILY, periodKey)
            .first { MissionCatalog.definitionFor(it).requirement in flatIncrementRequirements }
        // Deliberately seeded *below* target while already claimed - an event firing here must
        // never advance the count, isolating the "already claimed" guard from the separate
        // coerceAtMost(targetCount) cap that would otherwise mask this same outcome at the target.
        progressDao.upsert(MissionProgressEntity(targetId.name, periodKey, currentCount = 0, claimed = true))
        val repository = buildRepository(api = FakeMissionApi(), missionProgressDao = progressDao)

        repository.recordMissionEvent(eventFor(MissionCatalog.definitionFor(targetId).requirement), day)

        val stored = progressDao.get(targetId.name)
        assertEquals(0, stored?.currentCount)
        assertTrue(stored?.claimed == true)
    }
}
