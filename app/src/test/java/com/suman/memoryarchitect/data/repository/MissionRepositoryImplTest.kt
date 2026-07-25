package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.MissionProgressDao
import com.suman.memoryarchitect.core.database.MissionProgressEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.data.remote.MissionApi
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardResponseDto
import com.suman.memoryarchitect.data.remote.dto.ConsumeInventoryItemRequestDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.MissionEvent
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
    ) = MissionRepositoryImpl(
        mockBackendSource = MockBackendMissionRemoteSource(api),
        firestoreSource = FirestoreMissionRemoteSource(FakeMissionPlayerIdentityManager(), Clock.systemUTC()),
        playerIdentityManager = FakeMissionPlayerIdentityManager(),
        missionProgressDao = missionProgressDao,
        inventoryItemDao = inventoryItemDao,
        playerProgressDao = playerProgressDao,
        dispatchers = ImmediateDispatcherProvider,
        errorMapper = ErrorMapper(MissionRepoNoopCrashReporter),
    )

    @Test
    fun `getActiveMissions returns the deterministic catalog set with zero progress by default`() = runTest {
        val repository = buildRepository(api = FakeMissionApi())
        val todayEpochDay = 1000L

        val missions = repository.getActiveMissions(todayEpochDay)

        assertEquals(7, missions.size) // 3 daily + 3 weekly + 1 monthly
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
            profile = PlayerProfileDto(xp = definition.reward.xp, coins = definition.reward.coins, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null),
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
