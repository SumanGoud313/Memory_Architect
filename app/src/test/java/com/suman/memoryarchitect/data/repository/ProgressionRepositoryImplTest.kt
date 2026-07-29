package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.PendingScoreSubmissionDao
import com.suman.memoryarchitect.core.database.PendingScoreSubmissionEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.StatisticsCacheEntity
import com.suman.memoryarchitect.core.database.StatisticsDao
import com.suman.memoryarchitect.core.sync.PendingScoreSyncScheduler
import com.suman.memoryarchitect.core.database.UnlockedAchievementDao
import com.suman.memoryarchitect.core.database.UnlockedAchievementEntity
import com.suman.memoryarchitect.core.database.UnlockedRewardDao
import com.suman.memoryarchitect.core.database.UnlockedRewardEntity
import com.suman.memoryarchitect.data.remote.ProgressionApi
import com.suman.memoryarchitect.data.remote.dto.ClaimDailyRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimDailyRewardResponseDto
import com.suman.memoryarchitect.data.remote.dto.ClaimReturningPlayerGiftRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimReturningPlayerGiftResponseDto
import com.suman.memoryarchitect.data.remote.dto.DailyRewardStatusDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.data.remote.dto.ScoreSubmissionRequestDto
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock

private object ProgressionRepoNoopCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}

/** Forces the mock-backend path deterministically, independent of whether this machine's local
 * `app/google-services.json` happens to exist - see [FirebaseAvailabilityProvider]'s doc. */
private class FakeProgressionFirebaseAvailabilityProvider(override val isConfigured: Boolean = false) : FirebaseAvailabilityProvider

private class FakeProgressionApi(
    private val profile: PlayerProfileDto? = null,
    private val error: Throwable? = null,
    private val dailyRewardStatus: DailyRewardStatusDto? = null,
    private val claimResponse: ClaimDailyRewardResponseDto? = null,
    private val claimReturningPlayerGiftResponse: ClaimReturningPlayerGiftResponseDto? = null,
) : ProgressionApi {
    override suspend fun getProfile(): PlayerProfileDto {
        error?.let { throw it }
        return requireNotNull(profile)
    }

    override suspend fun submitScore(body: ScoreSubmissionRequestDto): PlayerProfileDto {
        error?.let { throw it }
        return requireNotNull(profile)
    }

    override suspend fun getDailyRewardStatus(todayEpochDay: Long): DailyRewardStatusDto {
        error?.let { throw it }
        return requireNotNull(dailyRewardStatus)
    }

    override suspend fun claimDailyReward(body: ClaimDailyRewardRequestDto): ClaimDailyRewardResponseDto {
        error?.let { throw it }
        return requireNotNull(claimResponse)
    }

    override suspend fun claimReturningPlayerGift(body: ClaimReturningPlayerGiftRequestDto): ClaimReturningPlayerGiftResponseDto {
        error?.let { throw it }
        return requireNotNull(claimReturningPlayerGiftResponse)
    }

    override suspend fun resetProfile(): PlayerProfileDto {
        error?.let { throw it }
        return requireNotNull(profile)
    }
}

private class FakePlayerProgressDao : PlayerProgressDao {
    private var stored: PlayerProgressCacheEntity? = null

    override suspend fun get(): PlayerProgressCacheEntity? = stored

    override suspend fun upsert(entity: PlayerProgressCacheEntity) {
        stored = entity
    }

    override suspend fun clearAll() {
        stored = null
    }
}

private class FakeProgressionInventoryItemDao : InventoryItemDao {
    private val stored = mutableMapOf<String, InventoryItemEntity>()
    override suspend fun getAll(): List<InventoryItemEntity> = stored.values.toList()
    override suspend fun get(kind: String): InventoryItemEntity? = stored[kind]
    override suspend fun upsert(entity: InventoryItemEntity) {
        stored[entity.kind] = entity
    }
    override suspend fun clearAll() = stored.clear()
}

/** No-op by default - these tests never need a real WorkManager instance, only to verify
 * submitScore/retryPendingSubmissions' own queuing/flushing logic. See PendingScoreSyncScheduler's
 * doc. Tracks call count for the one test that cares whether a retry was actually scheduled. */
private class FakePendingScoreSyncScheduler : PendingScoreSyncScheduler {
    var scheduleRetryCallCount = 0
        private set
    override fun scheduleRetry() {
        scheduleRetryCallCount++
    }
}

private class FakePendingScoreSubmissionDao : PendingScoreSubmissionDao {
    val inserted = mutableListOf<PendingScoreSubmissionEntity>()

    override suspend fun insert(entity: PendingScoreSubmissionEntity): Long {
        inserted += entity
        return inserted.size.toLong()
    }

    override suspend fun getAll(): List<PendingScoreSubmissionEntity> = inserted

    override suspend fun delete(entity: PendingScoreSubmissionEntity) {
        inserted.remove(entity)
    }

    override suspend fun clearAll() {
        inserted.clear()
    }
}

private class FakeStatisticsDao : StatisticsDao {
    private var stored: StatisticsCacheEntity? = null

    override suspend fun get(): StatisticsCacheEntity? = stored

    override suspend fun upsert(entity: StatisticsCacheEntity) {
        stored = entity
    }

    override suspend fun clearAll() {
        stored = null
    }
}

private class FakeUnlockedAchievementDao : UnlockedAchievementDao {
    private val stored = mutableMapOf<String, UnlockedAchievementEntity>()

    override suspend fun getAll(): List<UnlockedAchievementEntity> = stored.values.toList()

    override suspend fun upsert(entity: UnlockedAchievementEntity) {
        stored[entity.achievementId] = entity
    }

    override suspend fun clearAll() {
        stored.clear()
    }
}

private class FakeUnlockedRewardDao : UnlockedRewardDao {
    private val stored = mutableMapOf<String, UnlockedRewardEntity>()

    override suspend fun getAll(): List<UnlockedRewardEntity> = stored.values.toList()

    override suspend fun upsert(entity: UnlockedRewardEntity) {
        stored[entity.rewardId] = entity
    }

    override suspend fun clearAll() {
        stored.clear()
    }
}

/** Never signed in - forces [ProgressionRepositoryImpl.activeRemoteSource] to fall back to the
 * mock-backend path every time, which is exactly what these tests exercise (they're all written
 * against [FakeProgressionApi]/[MockBackendProgressionRemoteSource]'s behavior). */
private class FakePlayerIdentityManager : PlayerIdentityManager {
    override val uid: StateFlow<String?> = MutableStateFlow(null)
    override val isVerified: StateFlow<Boolean> = MutableStateFlow(false)
    override val displayName: StateFlow<String?> = MutableStateFlow(null)
    override val photoUrl: StateFlow<String?> = MutableStateFlow(null)
    override fun ensureSignedIn() = Unit
    override suspend fun awaitUid(timeoutMs: Long): String? = null
    override suspend fun linkWithGoogle(idToken: String): Result<Unit> = Result.failure(UnsupportedOperationException("not exercised by these tests"))
}

private val sampleScore = ScoreResult(objectScores = emptyList(), sceneAccuracy = 0.8f, placementScore = 140, timeBonus = 10, comboBonus = 0, finalScore = 150, comboCount = 0)

private class FakeLevelCampaignRepository(private val bestStars: Map<Int, Int> = emptyMap()) : LevelCampaignRepository {
    override suspend fun getMaxUnlockedLevel() = 1
    override suspend fun getAllBestTimes() = emptyMap<Int, Long>()
    override suspend fun getAllBestStars() = bestStars
    override suspend fun recordCompletion(levelNumber: Int, timeTakenMs: Long, passed: Boolean, stars: Int): LevelCompletionOutcome =
        throw UnsupportedOperationException("not used by these tests")
}

class ProgressionRepositoryImplTest {

    private fun buildRepository(
        api: ProgressionApi,
        progressDao: PlayerProgressDao = FakePlayerProgressDao(),
        pendingDao: PendingScoreSubmissionDao = FakePendingScoreSubmissionDao(),
        statisticsDao: StatisticsDao = FakeStatisticsDao(),
        unlockedAchievementDao: UnlockedAchievementDao = FakeUnlockedAchievementDao(),
        unlockedRewardDao: UnlockedRewardDao = FakeUnlockedRewardDao(),
        levelCampaignRepository: LevelCampaignRepository = FakeLevelCampaignRepository(),
        inventoryItemDao: InventoryItemDao = FakeProgressionInventoryItemDao(),
        pendingSyncScheduler: PendingScoreSyncScheduler = FakePendingScoreSyncScheduler(),
    ) = ProgressionRepositoryImpl(
        mockBackendSource = MockBackendProgressionRemoteSource(api),
        firestoreSource = FirestoreProgressionRemoteSource(FakePlayerIdentityManager(), Clock.systemUTC()),
        playerIdentityManager = FakePlayerIdentityManager(),
        progressDao = progressDao,
        pendingDao = pendingDao,
        statisticsDao = statisticsDao,
        unlockedAchievementDao = unlockedAchievementDao,
        unlockedRewardDao = unlockedRewardDao,
        levelCampaignRepository = levelCampaignRepository,
        inventoryItemDao = inventoryItemDao,
        dispatchers = ImmediateDispatcherProvider,
        errorMapper = ErrorMapper(ProgressionRepoNoopCrashReporter),
        firebaseAvailabilityProvider = FakeProgressionFirebaseAvailabilityProvider(),
        pendingSyncScheduler = pendingSyncScheduler,
    )

    @Test
    fun `getProfile success writes through to cache and returns the server profile`() = runTest {
        val dao = FakePlayerProgressDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 500, currentStreak = 3, longestStreak = 5, lastPlayedEpochDay = 100L)),
            progressDao = dao,
        )

        val result = repository.getProfile()

        assertTrue(result is Outcome.Success)
        assertEquals(500L, (result as Outcome.Success).data.xp)
        assertEquals(500L, dao.get()?.xp)
    }

    @Test
    fun `getProfile failure with existing cache falls back to cached values`() = runTest {
        val dao = FakePlayerProgressDao().apply {
            upsert(PlayerProgressCacheEntity(xp = 200L, coins = 0L, currentStreak = 1, longestStreak = 2, lastPlayedEpochDay = 50L, lastSyncedAt = 0L))
        }
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")), progressDao = dao)

        val result = repository.getProfile()

        assertTrue(result is Outcome.Success)
        assertEquals(200L, (result as Outcome.Success).data.xp)
    }

    @Test
    fun `getProfile failure with no cache returns an error`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")))

        val result = repository.getProfile()

        assertTrue(result is Outcome.Error)
        assertTrue((result as Outcome.Error).error is AppError.Network)
    }

    @Test
    fun `submitScore success updates the cache and is not pending sync`() = runTest {
        val dao = FakePlayerProgressDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 150, currentStreak = 1, longestStreak = 1, lastPlayedEpochDay = 10L)),
            progressDao = dao,
        )

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 10L, submissionNonce = "nonce-10")

        assertTrue(result is Outcome.Success)
        val submission = (result as Outcome.Success).data
        assertFalse(submission.isPendingSync)
        assertEquals(150L, dao.get()?.xp)
    }

    @Test
    fun `submitScore failure queues the submission, schedules a retry, and returns an optimistic pending result`() = runTest {
        val dao = FakePlayerProgressDao().apply {
            upsert(PlayerProgressCacheEntity(xp = 0L, coins = 0L, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null, lastSyncedAt = 0L))
        }
        val pendingDao = FakePendingScoreSubmissionDao()
        val scheduler = FakePendingScoreSyncScheduler()
        val repository = buildRepository(
            api = FakeProgressionApi(error = IOException("offline")), progressDao = dao, pendingDao = pendingDao, pendingSyncScheduler = scheduler,
        )

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 10L, submissionNonce = "nonce-10")

        assertTrue(result is Outcome.Success)
        val submission = (result as Outcome.Success).data
        assertTrue(submission.isPendingSync)
        assertEquals(150L, submission.profile.xp)
        assertEquals(1, pendingDao.inserted.size)
        assertEquals(sampleScore.comboCount, pendingDao.inserted.single().comboCount)
        assertEquals(150L, dao.get()?.xp)
        assertEquals(1, scheduler.scheduleRetryCallCount)
    }

    @Test
    fun `retryPendingSubmissions flushes a queued entry, updates the cache, and removes it from the queue`() = runTest {
        val dao = FakePlayerProgressDao()
        val pendingDao = FakePendingScoreSubmissionDao().apply {
            insert(
                PendingScoreSubmissionEntity(
                    mode = GameMode.CLASSIC.name, levelSeed = 1L, finalScore = 150, sceneAccuracy = 0.8f,
                    playedOnEpochDay = 10L, createdAt = 10L, submissionNonce = "nonce-10", comboCount = 3, newlyUnlockedAchievementCount = 1,
                ),
            )
        }
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 150, coins = 40, currentStreak = 1, longestStreak = 1, lastPlayedEpochDay = 10L)),
            progressDao = dao,
            pendingDao = pendingDao,
        )

        repository.retryPendingSubmissions()

        assertTrue(pendingDao.inserted.isEmpty())
        assertEquals(150L, dao.get()?.xp)
        assertEquals(40L, dao.get()?.coins)
    }

    @Test
    fun `retryPendingSubmissions drops an entry the server already has, not just leaves it queued`() = runTest {
        // FakeProgressionApi's `error` applies uniformly to every method, including the
        // reconciliation getProfile() call retryPendingSubmissions makes in this branch - so this
        // also exercises that reconciliation failing gracefully (runCatching + getOrNull) rather
        // than crashing, while still confirming the actual point: a confirmed-duplicate entry is
        // removed from the queue, never retried forever.
        val pendingDao = FakePendingScoreSubmissionDao().apply {
            insert(
                PendingScoreSubmissionEntity(
                    mode = GameMode.CLASSIC.name, levelSeed = 1L, finalScore = 150, sceneAccuracy = 0.8f,
                    playedOnEpochDay = 10L, createdAt = 10L, submissionNonce = "already-landed",
                ),
            )
        }
        val repository = buildRepository(api = FakeProgressionApi(error = DuplicateSubmissionException()), pendingDao = pendingDao)

        repository.retryPendingSubmissions()

        assertTrue(pendingDao.inserted.isEmpty())
    }

    @Test
    fun `retryPendingSubmissions leaves the queue intact while still offline`() = runTest {
        val pendingDao = FakePendingScoreSubmissionDao().apply {
            insert(
                PendingScoreSubmissionEntity(
                    mode = GameMode.CLASSIC.name, levelSeed = 1L, finalScore = 150, sceneAccuracy = 0.8f,
                    playedOnEpochDay = 10L, createdAt = 10L, submissionNonce = "still-offline",
                ),
            )
        }
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("still offline")), pendingDao = pendingDao)

        try {
            repository.retryPendingSubmissions()
            org.junit.Assert.fail("expected the IOException to propagate so the caller can back off and retry")
        } catch (_: IOException) {
            // expected
        }

        assertEquals(1, pendingDao.inserted.size)
    }

    @Test
    fun `submitScore failure computes journey points including the achievement-unlock bonus`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")))

        // sampleScore's 0.8 accuracy is not perfect and playedOnEpochDay 1 (streak=1) is not a
        // milestone - isolates the level-completed base (10) plus this round's one newly-unlocked
        // achievement (FIRST_STEPS, 25) from the other two journey point sources.
        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")

        val submission = (result as Outcome.Success).data
        assertEquals(1, submission.newlyUnlockedAchievements.size)
        assertEquals(35L, submission.journeyPointsAwarded)
        assertEquals(35L, submission.profile.journeyPoints)
    }

    @Test
    fun `submitScore failure computes a perfect-accuracy journey points bonus`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")))
        val perfectScore = sampleScore.copy(sceneAccuracy = 1f)

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = perfectScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")

        // 10 (level completed) + 5 (perfect accuracy) + 25 per achievement this exact perfect
        // round newly unlocks (a perfect score can satisfy more than one at once, e.g. FIRST_STEPS
        // and an accuracy-gated achievement together) - no streak milestone on a first-ever play.
        val submission = (result as Outcome.Success).data
        val expectedPoints = 10L + 5L + 25L * submission.newlyUnlockedAchievements.size
        assertEquals(expectedPoints, submission.journeyPointsAwarded)
    }

    @Test
    fun `submitScore failure computes a streak-milestone journey points bonus once achievements are already unlocked`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")))

        // Three consecutive days extends the streak to 3 (the first milestone) - round 1 already
        // exhausts the achievement-unlock bonus (see the achievement-only test above), isolating
        // round 3's journey points to exactly the level-completed base plus the milestone bonus.
        repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")
        repository.submitScore(GameMode.CLASSIC, levelSeed = 2L, score = sampleScore, playedOnEpochDay = 2L, submissionNonce = "nonce-2")
        val third = repository.submitScore(GameMode.CLASSIC, levelSeed = 3L, score = sampleScore, playedOnEpochDay = 3L, submissionNonce = "nonce-3")

        val submission = (third as Outcome.Success).data
        assertEquals(3, submission.streakMilestoneReached)
        assertEquals(0, submission.newlyUnlockedAchievements.size)
        assertEquals(40L, submission.journeyPointsAwarded)
    }

    @Test
    fun `submitScore maps a duplicate-submission-nonce rejection to a routine 409, not a pending retry`() = runTest {
        val pendingDao = FakePendingScoreSubmissionDao()
        val repository = buildRepository(
            api = FakeProgressionApi(error = DuplicateSubmissionException()),
            pendingDao = pendingDao,
        )

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "reused-nonce")

        assertTrue(result is Outcome.Error)
        val error = (result as Outcome.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(409, (error as AppError.Server).code)
        // A confirmed duplicate means the original submission already succeeded server-side -
        // queuing it for a future retry would just replay the same rejected nonce forever.
        assertTrue(pendingDao.inserted.isEmpty())
    }

    @Test
    fun `submitScore accumulates statistics across submissions`() = runTest {
        val statisticsDao = FakeStatisticsDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            statisticsDao = statisticsDao,
        )

        repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")
        val secondScore = sampleScore.copy(finalScore = 200, sceneAccuracy = 0.5f)
        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 2L, score = secondScore, playedOnEpochDay = 2L, submissionNonce = "nonce-2")

        val statistics = (result as Outcome.Success).data.statistics
        assertEquals(2, statistics.gamesPlayed)
        assertEquals(350L, statistics.totalScore)
        assertEquals(0.8f, statistics.bestAccuracy, 0.001f)
        assertEquals(200, statistics.bestScore)
    }

    @Test
    fun `win streak extends across successive submissions and resets to zero via resetWinStreak`() = runTest {
        val statisticsDao = FakeStatisticsDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            statisticsDao = statisticsDao,
        )

        val first = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")
        assertEquals(1, (first as Outcome.Success).data.statistics.currentWinStreak)
        val second = repository.submitScore(GameMode.CLASSIC, levelSeed = 2L, score = sampleScore, playedOnEpochDay = 2L, submissionNonce = "nonce-2")
        val secondStats = (second as Outcome.Success).data.statistics
        assertEquals(2, secondStats.currentWinStreak)
        assertEquals(2, secondStats.longestWinStreak)

        repository.resetWinStreak()

        assertEquals(0, repository.getStatistics().currentWinStreak)
        // longestWinStreak is a high-water mark - resetting the current streak must never erase it.
        assertEquals(2, repository.getStatistics().longestWinStreak)
    }

    @Test
    fun `submitScore evaluates and persists newly unlocked achievements`() = runTest {
        val unlockedDao = FakeUnlockedAchievementDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            unlockedAchievementDao = unlockedDao,
        )

        val firstResult = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")
        val firstSubmission = (firstResult as Outcome.Success).data
        assertTrue(AchievementId.FIRST_STEPS in firstSubmission.newlyUnlockedAchievements)
        assertEquals(1, unlockedDao.getAll().size)

        // Second submission should not re-report an achievement already unlocked.
        val secondResult = repository.submitScore(GameMode.CLASSIC, levelSeed = 2L, score = sampleScore, playedOnEpochDay = 2L, submissionNonce = "nonce-2")
        val secondSubmission = (secondResult as Outcome.Success).data
        assertFalse(AchievementId.FIRST_STEPS in secondSubmission.newlyUnlockedAchievements)
    }

    @Test
    fun `getStatistics reads from the local cache with no network involved`() = runTest {
        val statisticsDao = FakeStatisticsDao().apply {
            upsert(StatisticsCacheEntity(gamesPlayed = 4, totalScore = 900L, bestAccuracy = 0.75f, bestScore = 300))
        }
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")), statisticsDao = statisticsDao)

        val statistics = repository.getStatistics()

        assertEquals(4, statistics.gamesPlayed)
        assertEquals(900L, statistics.totalScore)
    }

    @Test
    fun `getStatistics with nothing cached yet returns empty`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")))

        val statistics = repository.getStatistics()

        assertEquals(PlayerStatistics.EMPTY, statistics)
    }

    @Test
    fun `getUnlockedAchievementIds reads from the local cache with no network involved`() = runTest {
        val unlockedDao = FakeUnlockedAchievementDao().apply {
            upsert(UnlockedAchievementEntity(AchievementId.FIRST_STEPS.name, unlockedAtEpochDay = 5L))
        }
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")), unlockedAchievementDao = unlockedDao)

        val unlocked = repository.getUnlockedAchievementIds()

        assertEquals(setOf(AchievementId.FIRST_STEPS), unlocked)
    }

    @Test
    fun `submitScore evaluates and persists newly unlocked rewards once xp crosses the first milestone`() = runTest {
        val rewardDao = FakeUnlockedRewardDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            unlockedRewardDao = rewardDao,
        )
        // Reward unlocks at level 5, which under the current XpCurve (500 * (level-1)^1.5) needs
        // 4000 XP - was 700 back when the curve was 100 * (level-1)^1.4 and level 5 needed ~697.
        val bigScore = sampleScore.copy(finalScore = 4000)

        val firstResult = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = bigScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")
        val firstSubmission = (firstResult as Outcome.Success).data
        assertEquals(1, firstSubmission.newlyUnlockedRewards.size)
        assertEquals(RewardId.ROOM_KITCHEN, firstSubmission.newlyUnlockedRewards.single().id)
        assertEquals(1, rewardDao.getAll().size)

        // Second submission should not re-report a reward already unlocked.
        val secondResult = repository.submitScore(GameMode.CLASSIC, levelSeed = 2L, score = sampleScore, playedOnEpochDay = 2L, submissionNonce = "nonce-2")
        val secondSubmission = (secondResult as Outcome.Success).data
        assertTrue(secondSubmission.newlyUnlockedRewards.isEmpty())
    }

    @Test
    fun `submitScore unlocks grand architect once level 100 has ever been passed`() = runTest {
        val unlockedDao = FakeUnlockedAchievementDao()
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            unlockedAchievementDao = unlockedDao,
            levelCampaignRepository = FakeLevelCampaignRepository(bestStars = mapOf(100 to 2)),
        )

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")

        val submission = (result as Outcome.Success).data
        assertTrue(AchievementId.GRAND_ARCHITECT in submission.newlyUnlockedAchievements)
    }

    @Test
    fun `submitScore does not unlock grand architect while only earlier levels have been passed`() = runTest {
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            levelCampaignRepository = FakeLevelCampaignRepository(bestStars = mapOf(99 to 3)),
        )

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")

        val submission = (result as Outcome.Success).data
        assertFalse(AchievementId.GRAND_ARCHITECT in submission.newlyUnlockedAchievements)
    }

    @Test
    fun `submitScore unlocks flawless once any level has ever been three-starred`() = runTest {
        val repository = buildRepository(
            api = FakeProgressionApi(profile = PlayerProfileDto(xp = 0, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)),
            levelCampaignRepository = FakeLevelCampaignRepository(bestStars = mapOf(12 to 3)),
        )

        val result = repository.submitScore(GameMode.CLASSIC, levelSeed = 1L, score = sampleScore, playedOnEpochDay = 1L, submissionNonce = "nonce-1")

        val submission = (result as Outcome.Success).data
        assertTrue(AchievementId.FLAWLESS in submission.newlyUnlockedAchievements)
    }

    @Test
    fun `getUnlockedRewardIds reads from the local cache with no network involved`() = runTest {
        val rewardDao = FakeUnlockedRewardDao().apply {
            upsert(UnlockedRewardEntity(RewardId.ROOM_KITCHEN.name, unlockedAtEpochDay = 5L))
        }
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")), unlockedRewardDao = rewardDao)

        val unlocked = repository.getUnlockedRewardIds()

        assertEquals(setOf(RewardId.ROOM_KITCHEN), unlocked)
    }

    @Test
    fun `getDailyRewardStatus returns the server-reported status`() = runTest {
        val repository = buildRepository(
            api = FakeProgressionApi(dailyRewardStatus = DailyRewardStatusDto(cycleDay = 3, canClaimToday = true, lastClaimedEpochDay = 99L)),
        )

        val result = repository.getDailyRewardStatus(todayEpochDay = 100L)

        assertTrue(result is Outcome.Success)
        val status = (result as Outcome.Success).data
        assertEquals(3, status.cycleDay)
        assertTrue(status.canClaimToday)
    }

    @Test
    fun `getDailyRewardStatus surfaces an error when offline, unlike getProfile it has no local fallback`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = IOException("offline")))

        val result = repository.getDailyRewardStatus(todayEpochDay = 100L)

        assertTrue(result is Outcome.Error)
    }

    @Test
    fun `claimDailyReward writes the awarded profile through to the local cache`() = runTest {
        val dao = FakePlayerProgressDao()
        val repository = buildRepository(
            api = FakeProgressionApi(
                claimResponse = ClaimDailyRewardResponseDto(
                    cycleDay = 2,
                    coinsAwarded = 60L,
                    xpAwarded = 0L,
                    profile = PlayerProfileDto(xp = 200L, coins = 260L, currentStreak = 1, longestStreak = 1, lastPlayedEpochDay = 100L),
                ),
            ),
            progressDao = dao,
        )

        val result = repository.claimDailyReward(todayEpochDay = 100L)

        assertTrue(result is Outcome.Success)
        val claim = (result as Outcome.Success).data
        assertEquals(2, claim.cycleDay)
        assertEquals(60L, claim.coinsAwarded)
        assertEquals(260L, dao.get()?.coins)
    }

    @Test
    fun `claimReturningPlayerGift writes the granted inventory through to the local cache`() = runTest {
        val inventoryDao = FakeProgressionInventoryItemDao()
        val repository = buildRepository(
            api = FakeProgressionApi(
                claimReturningPlayerGiftResponse = ClaimReturningPlayerGiftResponseDto(
                    inventory = InventoryDto(mapOf("MYSTERY_CHEST" to 1)),
                ),
            ),
            inventoryItemDao = inventoryDao,
        )

        val result = repository.claimReturningPlayerGift(todayEpochDay = 100L)

        assertTrue(result is Outcome.Success)
        assertEquals(1, inventoryDao.get("MYSTERY_CHEST")?.quantity)
    }

    @Test
    fun `claimReturningPlayerGift maps an already-claimed rejection to a routine 409`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = ReturningPlayerGiftAlreadyClaimedException()))

        val result = repository.claimReturningPlayerGift(todayEpochDay = 100L)

        assertTrue(result is Outcome.Error)
        val error = (result as Outcome.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(409, (error as AppError.Server).code)
    }

    @Test
    fun `claimReturningPlayerGift maps a not-eligible rejection to a 400`() = runTest {
        val repository = buildRepository(api = FakeProgressionApi(error = ReturningPlayerGiftNotEligibleException("gap too short")))

        val result = repository.claimReturningPlayerGift(todayEpochDay = 100L)

        assertTrue(result is Outcome.Error)
        val error = (result as Outcome.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(400, (error as AppError.Server).code)
    }
}
