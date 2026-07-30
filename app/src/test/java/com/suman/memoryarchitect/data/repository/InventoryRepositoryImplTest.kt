package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
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
import com.suman.memoryarchitect.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Forces the mock-backend path deterministically - same reasoning
 * [com.suman.memoryarchitect.data.repository.MissionRepositoryImplTest]'s identical copy gives. */
private class FakeInventoryFirebaseAvailabilityProvider(override val isConfigured: Boolean = false) : FirebaseAvailabilityProvider

private object InventoryRepoNoopCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}

private class FakeInventoryPlayerIdentityManager : PlayerIdentityManager {
    override val uid: StateFlow<String?> = MutableStateFlow(null)
    override val isVerified: StateFlow<Boolean> = MutableStateFlow(false)
    override val displayName: StateFlow<String?> = MutableStateFlow(null)
    override val photoUrl: StateFlow<String?> = MutableStateFlow(null)
    override fun ensureSignedIn() = Unit
    override suspend fun awaitUid(timeoutMs: Long): String? = null
    override suspend fun linkWithGoogle(idToken: String): Result<Unit> = Result.failure(UnsupportedOperationException("not exercised"))
    override suspend fun signOutAfterAccountDeletion() = Unit
}

private class FakeInventoryPlayerProgressDao : PlayerProgressDao {
    private var stored: PlayerProgressCacheEntity? = null
    override suspend fun get(): PlayerProgressCacheEntity? = stored
    override suspend fun upsert(entity: PlayerProgressCacheEntity) {
        stored = entity
    }
    override suspend fun clearAll() {
        stored = null
    }
}

private class FakeInventoryEconomyItemDao : InventoryItemDao {
    private val stored = mutableMapOf<String, InventoryItemEntity>()
    override suspend fun getAll(): List<InventoryItemEntity> = stored.values.toList()
    override suspend fun get(kind: String): InventoryItemEntity? = stored[kind]
    override suspend fun upsert(entity: InventoryItemEntity) {
        stored[entity.kind] = entity
    }
    override suspend fun clearAll() = stored.clear()
}

/** [inventory] mirrors the mock-backend's in-memory inventory state across calls, same convention
 * [com.suman.memoryarchitect.data.repository.MissionRepositoryImplTest]'s `FakeMissionApi` uses. */
private class FakeInventoryMissionApi(
    initialInventory: Map<String, Int> = emptyMap(),
    initialProfile: PlayerProfileDto = PlayerProfileDto(xp = 0L, coins = 0L, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null),
) : MissionApi {
    private var inventory = initialInventory.toMutableMap()
    private var profile = initialProfile

    override suspend fun getInventory(): InventoryDto = InventoryDto(inventory.toMap())

    override suspend fun claimMissionReward(body: ClaimMissionRewardRequestDto): ClaimMissionRewardResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")

    override suspend fun consumeInventoryItem(body: ConsumeInventoryItemRequestDto): InventoryDto =
        throw UnsupportedOperationException("not exercised by these tests")

    override suspend fun openMysteryChest(body: OpenMysteryChestRequestDto): InventoryEconomyResponseDto {
        val owned = inventory["MYSTERY_CHEST"] ?: 0
        if (owned < 1) error("insufficient_inventory")
        inventory["MYSTERY_CHEST"] = owned - 1
        profile = profile.copy(coins = profile.coins + body.coinsAwarded)
        return InventoryEconomyResponseDto(profile = profile, inventory = InventoryDto(inventory.toMap()))
    }

    override suspend fun applyXpBoost(body: ApplyXpBoostRequestDto): InventoryEconomyResponseDto {
        val owned = inventory["XP_BOOST"] ?: 0
        if (owned < 1) error("insufficient_inventory")
        inventory["XP_BOOST"] = owned - 1
        profile = profile.copy(xp = profile.xp + body.xpAwarded)
        return InventoryEconomyResponseDto(profile = profile, inventory = InventoryDto(inventory.toMap()))
    }

    override suspend fun claimCategoryBonus(body: ClaimCategoryBonusRequestDto): ClaimCategoryBonusResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")

    override suspend fun unlockAllMissionsEarly(body: UnlockAllMissionsEarlyRequestDto): UnlockAllMissionsEarlyResponseDto =
        throw UnsupportedOperationException("not exercised by these tests")
}

class InventoryRepositoryImplTest {

    private fun buildRepository(
        api: MissionApi,
        inventoryItemDao: InventoryItemDao = FakeInventoryEconomyItemDao(),
        playerProgressDao: PlayerProgressDao = FakeInventoryPlayerProgressDao(),
    ) = InventoryRepositoryImpl(
        mockBackendSource = MockBackendMissionRemoteSource(api),
        firestoreSource = FirestoreMissionRemoteSource(FakeInventoryPlayerIdentityManager(), java.time.Clock.systemUTC()),
        playerIdentityManager = FakeInventoryPlayerIdentityManager(),
        inventoryItemDao = inventoryItemDao,
        playerProgressDao = playerProgressDao,
        dispatchers = ImmediateDispatcherProvider,
        errorMapper = ErrorMapper(InventoryRepoNoopCrashReporter),
        firebaseAvailabilityProvider = FakeInventoryFirebaseAvailabilityProvider(),
    )

    @Test
    fun `openMysteryChest grants coins and consumes exactly one chest`() = runTest {
        val progressDao = FakeInventoryPlayerProgressDao()
        val repository = buildRepository(
            api = FakeInventoryMissionApi(initialInventory = mapOf("MYSTERY_CHEST" to 2)),
            playerProgressDao = progressDao,
        )

        val result = repository.openMysteryChest()

        assertTrue(result is Outcome.Success)
        val reward = (result as Outcome.Success).data
        assertTrue(reward.coinsAwarded > 0)
        assertEquals(reward.coinsAwarded, progressDao.get()?.coins)
    }

    @Test
    fun `openMysteryChest fails cleanly without an owned chest`() = runTest {
        val repository = buildRepository(api = FakeInventoryMissionApi(initialInventory = emptyMap()))

        val result = repository.openMysteryChest()

        assertTrue(result is Outcome.Error)
    }

    @Test
    fun `applyXpBoost grants the configured flat XP and consumes exactly one boost`() = runTest {
        val progressDao = FakeInventoryPlayerProgressDao()
        val repository = buildRepository(
            api = FakeInventoryMissionApi(initialInventory = mapOf("XP_BOOST" to 1)),
            playerProgressDao = progressDao,
        )

        val result = repository.applyXpBoost()

        assertTrue(result is Outcome.Success)
        assertEquals(com.suman.memoryarchitect.domain.progression.XpBoostRules.Default.xpGrantedPerBoost, (result as Outcome.Success).data)
        assertEquals(result.data, progressDao.get()?.xp)
    }

    @Test
    fun `applyXpBoost fails cleanly without an owned boost`() = runTest {
        val repository = buildRepository(api = FakeInventoryMissionApi(initialInventory = emptyMap()))

        val result = repository.applyXpBoost()

        assertTrue(result is Outcome.Error)
    }
}
