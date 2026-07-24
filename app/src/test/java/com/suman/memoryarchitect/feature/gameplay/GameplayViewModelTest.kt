package com.suman.memoryarchitect.feature.gameplay

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import com.suman.memoryarchitect.core.ads.RewardedAdController
import com.suman.memoryarchitect.core.ads.RewardedAdFailureReason
import com.suman.memoryarchitect.core.ads.RewardedAdResult
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.FrustrationTracker
import com.suman.memoryarchitect.core.analytics.PerformanceTrace
import com.suman.memoryarchitect.core.analytics.PerformanceTracer
import com.suman.memoryarchitect.core.feedback.FeedbackManager
import com.suman.memoryarchitect.core.feedback.ResultMood
import com.suman.memoryarchitect.core.feedback.audio.MusicTrack
import com.suman.memoryarchitect.domain.generation.DifficultyEngine
import com.suman.memoryarchitect.domain.generation.LevelGenerator
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.GlobalLeaderboardStats
import com.suman.memoryarchitect.domain.model.HintReveal
import com.suman.memoryarchitect.domain.model.LeaderboardResult
import com.suman.memoryarchitect.domain.model.LeaderboardType
import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PeriodicLeaderboardSubmission
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult
import com.suman.memoryarchitect.domain.repository.HintRepository
import com.suman.memoryarchitect.domain.repository.LeaderboardRepository
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import com.suman.memoryarchitect.domain.repository.LevelRepository
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import com.suman.memoryarchitect.domain.repository.RedoRepository
import com.suman.memoryarchitect.domain.repository.RewatchRepository
import com.suman.memoryarchitect.domain.usecase.GenerateLevelUseCase
import com.suman.memoryarchitect.domain.usecase.GetHintsUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetLevelCampaignProgressUseCase
import com.suman.memoryarchitect.domain.usecase.GetRedosUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetRewatchesUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedAchievementsUseCase
import com.suman.memoryarchitect.domain.usecase.GrantBonusHintUseCase
import com.suman.memoryarchitect.domain.usecase.GrantBonusRedoUseCase
import com.suman.memoryarchitect.domain.usecase.RecordHintUsedUseCase
import com.suman.memoryarchitect.domain.usecase.RecordLevelCompletionUseCase
import com.suman.memoryarchitect.domain.usecase.RecordRedoUsedUseCase
import com.suman.memoryarchitect.domain.usecase.RecordRewatchUsedUseCase
import com.suman.memoryarchitect.domain.usecase.ResetHintUsageUseCase
import com.suman.memoryarchitect.domain.usecase.ResetRedoUsageUseCase
import com.suman.memoryarchitect.domain.usecase.ResetRewatchUsageUseCase
import com.suman.memoryarchitect.domain.usecase.ResetWinStreakUseCase
import com.suman.memoryarchitect.domain.usecase.SubmitDailyLeaderboardScoreUseCase
import com.suman.memoryarchitect.domain.usecase.SubmitGlobalLeaderboardStatsUseCase
import com.suman.memoryarchitect.domain.usecase.SubmitMonthlyLeaderboardScoreUseCase
import com.suman.memoryarchitect.domain.usecase.SubmitScoreUseCase
import com.suman.memoryarchitect.domain.usecase.SubmitWeeklyLeaderboardScoreUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private class FakeLevelRepository : LevelRepository {
    var generateCallCount = 0
        private set

    override suspend fun generateLevel(mode: GameMode, difficultyTier: DifficultyTier, streak: Int): Outcome<com.suman.memoryarchitect.domain.model.LevelSpec> {
        generateCallCount++
        return Outcome.Success(
            LevelGenerator().generate(
                seed = 1L,
                mode = mode,
                difficultyTier = difficultyTier,
                constraints = DifficultyEngine().computeConstraints(difficultyTier, streak, mode),
            ),
        )
    }
}

private class FakeProgressionRepository(
    var shouldSucceed: Boolean = true,
) : ProgressionRepository {
    var submitScoreCalled = false

    override suspend fun getProfile() = Outcome.Success(PlayerProfile.EMPTY)

    override suspend fun getStatistics() = PlayerStatistics.EMPTY

    override suspend fun getUnlockedAchievementIds() = emptySet<AchievementId>()

    override suspend fun getUnlockedRewardIds() = emptySet<RewardId>()

    override suspend fun getDailyRewardStatus(todayEpochDay: Long): Outcome<com.suman.memoryarchitect.domain.model.DailyRewardStatus> =
        throw UnsupportedOperationException("not used by GameplayViewModel")

    override suspend fun claimDailyReward(todayEpochDay: Long): Outcome<com.suman.memoryarchitect.domain.model.DailyRewardClaimResult> =
        throw UnsupportedOperationException("not used by GameplayViewModel")

    override suspend fun recordLeaderboardRank(dailyRank: Int?, weeklyRank: Int?, todayEpochDay: Long): List<AchievementId> =
        throw UnsupportedOperationException("not used by GameplayViewModel")

    var resetWinStreakCalled = false
    override suspend fun resetWinStreak() {
        resetWinStreakCalled = true
    }

    override suspend fun submitScore(mode: GameMode, levelSeed: Long, score: ScoreResult, playedOnEpochDay: Long, timeTakenMs: Long, submissionNonce: String): Outcome<ScoreSubmissionResult> {
        submitScoreCalled = true
        return Outcome.Success(
            ScoreSubmissionResult(
                profile = PlayerProfile.EMPTY.copy(xp = score.finalScore.toLong()),
                xpAwarded = score.finalScore.toLong(),
                coinsAwarded = 0L,
                leveledUp = false,
                isPendingSync = !shouldSucceed,
                statistics = PlayerStatistics.EMPTY.copy(gamesPlayed = 1),
                newlyUnlockedAchievements = emptyList(),
            ),
        )
    }
}

private class FakeLeaderboardRepository : LeaderboardRepository {
    var globalStatsSubmitted: GlobalLeaderboardStats? = null
    var dailyScoreSubmitted: PeriodicLeaderboardSubmission? = null
    var weeklyScoreSubmitted: PeriodicLeaderboardSubmission? = null
    var monthlyScoreSubmitted: PeriodicLeaderboardSubmission? = null

    override suspend fun submitGlobalStats(stats: GlobalLeaderboardStats): Outcome<Unit> {
        globalStatsSubmitted = stats
        return Outcome.Success(Unit)
    }

    override suspend fun submitDailyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit> {
        dailyScoreSubmitted = submission
        return Outcome.Success(Unit)
    }

    override suspend fun submitWeeklyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit> {
        weeklyScoreSubmitted = submission
        return Outcome.Success(Unit)
    }

    override suspend fun submitMonthlyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit> {
        monthlyScoreSubmitted = submission
        return Outcome.Success(Unit)
    }

    override suspend fun getGlobalLeaderboard(limit: Int): Outcome<LeaderboardResult> =
        Outcome.Success(LeaderboardResult(LeaderboardType.GLOBAL, emptyList(), null))

    override suspend fun getDailyLeaderboard(limit: Int): Outcome<LeaderboardResult> =
        Outcome.Success(LeaderboardResult(LeaderboardType.DAILY, emptyList(), null))

    override suspend fun getWeeklyLeaderboard(limit: Int): Outcome<LeaderboardResult> =
        Outcome.Success(LeaderboardResult(LeaderboardType.WEEKLY, emptyList(), null))

    override suspend fun getMonthlyLeaderboard(limit: Int): Outcome<LeaderboardResult> =
        Outcome.Success(LeaderboardResult(LeaderboardType.MONTHLY, emptyList(), null))
}

private class FakeHintRepository(initial: Map<Int, Int> = emptyMap()) : HintRepository {
    private val usage = initial.toMutableMap()
    override suspend fun getHintsUsed(levelNumber: Int): Int = usage[levelNumber] ?: 0
    override suspend fun recordHintUsed(levelNumber: Int) {
        usage[levelNumber] = (usage[levelNumber] ?: 0) + 1
    }
    override suspend fun grantBonusHint(levelNumber: Int) {
        usage[levelNumber] = ((usage[levelNumber] ?: 0) - 1).coerceAtLeast(0)
    }
    override suspend fun resetHintUsage(levelNumber: Int) {
        usage.remove(levelNumber)
    }
    fun usedCountFor(levelNumber: Int): Int = usage[levelNumber] ?: 0
}

private class FakeRewardedAdController(
    var result: RewardedAdResult = RewardedAdResult.Rewarded,
    var delayMs: Long = 0L,
) : RewardedAdController {
    var loadAndShowCallCount = 0
        private set

    override suspend fun loadAndShow(activity: Activity, feature: String): RewardedAdResult {
        loadAndShowCallCount++
        if (delayMs > 0) delay(delayMs)
        return result
    }
}

private class FakeRewatchRepository(initial: Map<Int, Int> = emptyMap()) : RewatchRepository {
    private val usage = initial.toMutableMap()
    override suspend fun getRewatchesUsed(levelNumber: Int): Int = usage[levelNumber] ?: 0
    override suspend fun recordRewatchUsed(levelNumber: Int) {
        usage[levelNumber] = (usage[levelNumber] ?: 0) + 1
    }
    override suspend fun resetRewatchUsage(levelNumber: Int) {
        usage.remove(levelNumber)
    }
    fun usedCountFor(levelNumber: Int): Int = usage[levelNumber] ?: 0
}

private class FakeRedoRepository(initial: Map<Int, Int> = emptyMap()) : RedoRepository {
    private val usage = initial.toMutableMap()
    override suspend fun getRedosUsed(levelNumber: Int): Int = usage[levelNumber] ?: 0
    override suspend fun recordRedoUsed(levelNumber: Int) {
        usage[levelNumber] = (usage[levelNumber] ?: 0) + 1
    }
    override suspend fun grantBonusRedo(levelNumber: Int) {
        usage[levelNumber] = ((usage[levelNumber] ?: 0) - 1).coerceAtLeast(0)
    }
    override suspend fun resetRedoUsage(levelNumber: Int) {
        usage.remove(levelNumber)
    }
    fun usedCountFor(levelNumber: Int): Int = usage[levelNumber] ?: 0
}

private class FakeAnalyticsLogger : AnalyticsLogger {
    data class LoggedEvent(val name: String, val params: Map<String, Any?>)

    val events = mutableListOf<LoggedEvent>()

    override fun logEvent(name: String, params: Map<String, Any?>) {
        events += LoggedEvent(name, params)
    }

    override fun setUserProperty(name: String, value: String?) = Unit
}

private class FakePerformanceTracer : PerformanceTracer {
    override fun startTrace(name: String): PerformanceTrace = object : PerformanceTrace {
        override fun putMetric(name: String, value: Long) = Unit
        override fun stop() = Unit
    }
}

private class FakeFeedbackManager : FeedbackManager {
    override fun onUiTap() = Unit
    override fun onUiConfirm() = Unit
    override fun onUiBack() = Unit
    override fun onDialogOpen() = Unit
    override fun onDialogClose() = Unit
    override fun onScreenOpen(track: MusicTrack) = Unit
    override fun setMusicTrack(track: MusicTrack) = Unit
    override fun pauseMusic() = Unit
    override fun resumeMusic() = Unit
    override fun setMusicResumeSuppressed(suppressed: Boolean) = Unit
    override fun onObjectPickup() = Unit
    override fun onObjectRotate() = Unit
    override fun onObjectPlace() = Unit
    override fun onComboStep(step: Int) = Unit
    override fun onTimerTick(remainingMs: Long, isReconstructPhase: Boolean) = Unit
    override fun onTimerPhaseStarted() = Unit
    override fun stopTimerAudio() = Unit
    override fun onResultsRevealed(mood: ResultMood, passed: Boolean) = Unit
    override fun onCoinsAwarded() = Unit
    override fun onXpAwarded() = Unit
    override fun onStarAwarded() = Unit
    override fun onAchievementUnlocked() = Unit
    override fun onLevelUnlocked() = Unit
    override fun onDailyRewardClaimed() = Unit
    override fun onWeeklyRewardClaimed() = Unit
    override fun onWarning() = Unit
    override fun onError() = Unit
}

private class FakeLevelCampaignRepository(var maxUnlockedLevel: Int = 1) : LevelCampaignRepository {
    override suspend fun getMaxUnlockedLevel() = maxUnlockedLevel
    override suspend fun getAllBestTimes() = emptyMap<Int, Long>()
    override suspend fun getAllBestStars() = emptyMap<Int, Int>()
    override suspend fun recordCompletion(levelNumber: Int, timeTakenMs: Long, passed: Boolean, stars: Int) = LevelCompletionOutcome(
        levelNumber = levelNumber,
        passed = passed,
        timeTakenMs = timeTakenMs,
        bestTimeMs = if (passed) timeTakenMs else null,
        isNewBest = passed,
        stars = stars,
        bestStars = stars,
        isNewBestStars = passed,
        nextLevelUnlocked = passed,
        isFinalLevel = false,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameplayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC)
    private val fakeActivity = object : Activity() {}

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun savedState(mode: GameMode, levelNumber: Int = 1) = SavedStateHandle(
        mapOf("mode" to mode.name, "difficulty" to DifficultyTier.EASY.name, "levelNumber" to levelNumber),
    )

    private fun viewModel(
        mode: GameMode,
        progressionRepository: ProgressionRepository = FakeProgressionRepository(),
        campaignRepository: LevelCampaignRepository = FakeLevelCampaignRepository(),
        hintRepository: HintRepository = FakeHintRepository(),
        rewardedAdController: RewardedAdController = FakeRewardedAdController(),
        redoRepository: RedoRepository = FakeRedoRepository(),
        rewatchRepository: RewatchRepository = FakeRewatchRepository(),
        levelRepository: FakeLevelRepository = FakeLevelRepository(),
        leaderboardRepository: LeaderboardRepository = FakeLeaderboardRepository(),
        levelNumber: Int = 1,
        clock: Clock = fixedClock,
        analyticsLogger: AnalyticsLogger = FakeAnalyticsLogger(),
        performanceTracer: PerformanceTracer = FakePerformanceTracer(),
        frustrationTracker: FrustrationTracker = FrustrationTracker(),
        feedback: FeedbackManager = FakeFeedbackManager(),
        savedStateHandle: SavedStateHandle = savedState(mode, levelNumber),
    ) = GameplayViewModel(
        generateLevel = GenerateLevelUseCase(levelRepository),
        submitScore = SubmitScoreUseCase(progressionRepository),
        recordLevelCompletion = RecordLevelCompletionUseCase(campaignRepository),
        getCampaignProgress = GetLevelCampaignProgressUseCase(campaignRepository),
        submitGlobalLeaderboardStats = SubmitGlobalLeaderboardStatsUseCase(leaderboardRepository),
        submitDailyLeaderboardScore = SubmitDailyLeaderboardScoreUseCase(leaderboardRepository),
        submitWeeklyLeaderboardScore = SubmitWeeklyLeaderboardScoreUseCase(leaderboardRepository),
        submitMonthlyLeaderboardScore = SubmitMonthlyLeaderboardScoreUseCase(leaderboardRepository),
        getUnlockedAchievements = GetUnlockedAchievementsUseCase(progressionRepository),
        resetWinStreak = ResetWinStreakUseCase(progressionRepository),
        getHintsUsed = GetHintsUsedUseCase(hintRepository),
        recordHintUsed = RecordHintUsedUseCase(hintRepository),
        grantBonusHint = GrantBonusHintUseCase(hintRepository),
        resetHintUsage = ResetHintUsageUseCase(hintRepository),
        rewardedAdController = rewardedAdController,
        getRedosUsed = GetRedosUsedUseCase(redoRepository),
        recordRedoUsed = RecordRedoUsedUseCase(redoRepository),
        grantBonusRedo = GrantBonusRedoUseCase(redoRepository),
        resetRedoUsage = ResetRedoUsageUseCase(redoRepository),
        getRewatchesUsed = GetRewatchesUsedUseCase(rewatchRepository),
        recordRewatchUsed = RecordRewatchUsedUseCase(rewatchRepository),
        resetRewatchUsage = ResetRewatchUsageUseCase(rewatchRepository),
        clock = clock,
        analytics = analyticsLogger,
        performanceTracer = performanceTracer,
        frustrationTracker = frustrationTracker,
        feedback = feedback,
        savedStateHandle = savedStateHandle,
    )

    /** A [Clock] whose reading can be moved forward mid-test via [advanceBy] - simulates real wall
     * time elapsing while the app is backgrounded or the process is dead, independent of the
     * virtual coroutine-scheduler time [testDispatcher] advances (the two are deliberately
     * decoupled: [com.suman.memoryarchitect.feature.gameplay.engine.CountdownTimer] itself still
     * ticks on virtual/coroutine time, only the wall-clock deadline correction reads this). */
    private class MutableClock(startAt: Instant) : Clock() {
        private var instant = startAt
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = instant
        fun advanceBy(millis: Long) {
            instant = instant.plusMillis(millis)
        }
    }

    /** Advances a fresh view model to Reconstruct and returns its InProgress state. */
    private fun advanceToReconstruct(viewModel: GameplayViewModel): GameplayUiState.InProgress {
        testDispatcher.scheduler.runCurrent()
        val memorizeState = viewModel.uiState.value as GameplayUiState.InProgress
        testDispatcher.scheduler.advanceTimeBy(memorizeState.level.memorizeDurationMs + 1_000L)
        testDispatcher.scheduler.runCurrent()
        return viewModel.uiState.value as GameplayUiState.InProgress
    }

    /** Places every non-distractor target on its own correct slot AND rotation, so the round scores
     * full accuracy and clears [com.suman.memoryarchitect.domain.progression.LevelCampaignEngine.passed]'s
     * threshold regardless of mode. Matching the slot alone is enough when rotation is disabled
     * (Practice), but scored modes generate rotated targets - [rotatePlacedObject] always starts a
     * freshly placed object at 0 degrees, so it takes exactly `target.rotationDegrees / 90` taps to
     * bring it back to the target's real rotation (90-degree steps, see [GenerationRules]). */
    private fun placeAllTargetsCorrectly(viewModel: GameplayViewModel, reconstructState: GameplayUiState.InProgress) {
        reconstructState.level.objects.filter { !it.isDistractor }.forEach { target ->
            viewModel.placeObject(target.objectId, slotIndex = target.slotIndex)
            repeat(target.rotationDegrees / 90) { viewModel.rotatePlacedObject(target.objectId) }
        }
    }

    /** Drives [count] real hint uses through the ViewModel's own public API (arm + reveal a
     * distinct valid target each time) rather than pre-seeding a repository - loadLevel now resets
     * a level's usage on every fresh attempt (see "a fresh attempt at the same level resets a
     * previously used hint budget back to full" below), so a budget can only legitimately start
     * exhausted by actually using it, exactly as a real play session would. */
    private fun exhaustFreeHints(viewModel: GameplayViewModel, reconstructState: GameplayUiState.InProgress, count: Int) {
        val targets = reconstructState.level.objects.filter { !it.isDistractor }.map { it.objectId }
        targets.take(count).forEach { id ->
            viewModel.toggleHintArmed()
            viewModel.requestHint(id)
            testDispatcher.scheduler.runCurrent()
        }
    }

    /** Drives [count] real redo uses through the ViewModel's own public API (place then immediately
     * undo a distinct tray object each time), for the same reason [exhaustFreeHints] avoids
     * repository pre-seeding. */
    private fun exhaustFreeRedos(viewModel: GameplayViewModel, reconstructState: GameplayUiState.InProgress, count: Int) {
        reconstructState.trayObjectIds.take(count).forEach { id ->
            viewModel.placeObject(id, slotIndex = 0)
            viewModel.redoLastPlacement()
            testDispatcher.scheduler.runCurrent()
        }
    }

    @Test
    fun `progresses from memorize through hidden into reconstruct for practice mode`() {
        val viewModel = viewModel(GameMode.PRACTICE)

        // runCurrent (not advanceUntilIdle) - the latter would drain the future-scheduled
        // timer ticks too and skip straight past Hidden into Reconstruct.
        testDispatcher.scheduler.runCurrent()
        val memorizeState = viewModel.uiState.value
        assertTrue(memorizeState is GameplayUiState.InProgress)
        assertEquals(GamePhase.MEMORIZE, (memorizeState as GameplayUiState.InProgress).phase)

        val reconstructState = advanceToReconstruct(viewModel)
        assertEquals(GamePhase.RECONSTRUCT, reconstructState.phase)
        assertEquals(reconstructState.level.objects.size, reconstructState.trayObjectIds.size)
        assertTrue(reconstructState.placements.isEmpty())
    }

    @Test
    fun `placing an object moves it from the tray into placements`() {
        val viewModel = viewModel(GameMode.PRACTICE)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()

        viewModel.placeObject(objectId, slotIndex = 0)

        val updated = viewModel.uiState.value as GameplayUiState.InProgress
        assertFalse(objectId in updated.trayObjectIds)
        assertEquals(0, updated.placements[objectId]?.slotIndex)
    }

    @Test
    fun `rotating a placed object cycles rotation by 90 degrees`() {
        val viewModel = viewModel(GameMode.PRACTICE)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 1)

        viewModel.rotatePlacedObject(objectId)

        val updated = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(90, updated.placements[objectId]?.rotationDegrees)
    }

    @Test
    fun `picking up a placed object returns it to the tray`() {
        val viewModel = viewModel(GameMode.PRACTICE)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 1)

        viewModel.pickUpPlacedObject(objectId)

        val updated = viewModel.uiState.value as GameplayUiState.InProgress
        assertTrue(objectId in updated.trayObjectIds)
        assertTrue(objectId !in updated.placements)
    }

    @Test
    fun `practice mode finishes without ever submitting a score`() {
        val progressionRepository = FakeProgressionRepository()
        val viewModel = viewModel(GameMode.PRACTICE, progressionRepository)
        val reconstructState = advanceToReconstruct(viewModel)
        reconstructState.trayObjectIds.forEachIndexed { index, objectId -> viewModel.placeObject(objectId, slotIndex = index) }

        viewModel.submitReconstruction()
        testDispatcher.scheduler.runCurrent()

        val finished = viewModel.uiState.value as GameplayUiState.Finished
        assertFalse(finished.isSubmitting)
        assertNull(finished.submission)
        assertFalse(progressionRepository.submitScoreCalled)
    }

    @Test
    fun `Finished reports passed=true when every object lands on its real correct slot`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 1)
        val reconstructState = advanceToReconstruct(viewModel)
        placeAllTargetsCorrectly(viewModel, reconstructState)

        // isAutoSubmit=true - the tray's remaining distractors (never placed, irrelevant to this
        // test's focus on target-placement accuracy) would otherwise trip the explicit-submit
        // completeness guard, which only cares whether the tray is empty, not what's correct.
        viewModel.submitReconstruction(isAutoSubmit = true)
        testDispatcher.scheduler.runCurrent()

        val finished = viewModel.uiState.value as GameplayUiState.Finished
        assertTrue(finished.passed)
    }

    @Test
    fun `Finished reports passed=false when the round auto-submits with nothing placed`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 1)
        advanceToReconstruct(viewModel)

        viewModel.submitReconstruction(isAutoSubmit = true)
        testDispatcher.scheduler.runCurrent()

        val finished = viewModel.uiState.value as GameplayUiState.Finished
        assertFalse(finished.passed)
    }

    @Test
    fun `scored mode submits the score and reports the resulting profile`() {
        val progressionRepository = FakeProgressionRepository(shouldSucceed = true)
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, progressionRepository)
        val reconstructState = advanceToReconstruct(viewModel)
        // submitScore now only fires once the round actually passes (see submitReconstruction's
        // passedThisLevel gate) - every target object needs to land on its real correct slot and
        // rotation, not just any slot, to clear the 0.7 accuracy threshold.
        placeAllTargetsCorrectly(viewModel, reconstructState)

        viewModel.submitReconstruction()
        testDispatcher.scheduler.runCurrent()

        val finished = viewModel.uiState.value as GameplayUiState.Finished
        assertTrue(progressionRepository.submitScoreCalled)
        assertFalse(finished.isSubmitting)
        assertFalse(requireNotNull(finished.submission).isPendingSync)
    }

    @Test
    fun `a failed Classic attempt never submits a score, so no XP, coins, achievements, or rewards are granted`() {
        val progressionRepository = FakeProgressionRepository(shouldSucceed = true)
        // maxUnlockedLevel=2 skips the level-1 tutorial gate (see "the tutorial never shows again
        // once level 1 has already been passed" above) so this reaches RECONSTRUCT directly.
        val viewModel = viewModel(
            GameMode.CLASSIC,
            progressionRepository,
            campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 2),
            levelNumber = 1,
        )
        val reconstructState = advanceToReconstruct(viewModel)

        // Nothing placed - 0% accuracy, guaranteed to miss the 0.7 pass threshold.
        viewModel.submitReconstruction(isAutoSubmit = true)
        testDispatcher.scheduler.runCurrent()

        val finished = viewModel.uiState.value as GameplayUiState.Finished
        assertFalse(finished.passed)
        assertFalse(progressionRepository.submitScoreCalled)
        assertNull(finished.submission)
        assertTrue(progressionRepository.resetWinStreakCalled)
    }

    @Test
    fun `submitReconstruction is rejected and stays in reconstruct while objects remain in the tray`() {
        val progressionRepository = FakeProgressionRepository()
        val viewModel = viewModel(GameMode.PRACTICE, progressionRepository)
        advanceToReconstruct(viewModel)

        viewModel.submitReconstruction()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.RECONSTRUCT, (state as GameplayUiState.InProgress).phase)
        assertFalse(progressionRepository.submitScoreCalled)
    }

    @Test
    fun `a rejected submission emits the exact number of objects still remaining`() {
        val viewModel = viewModel(GameMode.PRACTICE)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 0)
        val expectedRemaining = reconstructState.trayObjectIds.size - 1

        // submitRejected has no replay buffer, so the collector has to already be subscribed
        // before the rejection fires — start it (and let it actually reach the suspend point via
        // runCurrent) before triggering the rejected submit, exactly as GameplayScreen's
        // LaunchedEffect does in production by subscribing on composition, well before any tap.
        var rejectedCount: Int? = null
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { rejectedCount = viewModel.submitRejected.first() }
        testDispatcher.scheduler.runCurrent()

        viewModel.submitReconstruction()
        testDispatcher.scheduler.runCurrent()

        assertEquals(expectedRemaining, rejectedCount)
    }

    @Test
    fun `isAutoSubmit bypasses the completeness guard even with objects still in the tray`() {
        val progressionRepository = FakeProgressionRepository()
        val viewModel = viewModel(GameMode.PRACTICE, progressionRepository)
        advanceToReconstruct(viewModel)

        viewModel.submitReconstruction(isAutoSubmit = true)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is GameplayUiState.Finished)
    }

    @Test
    fun `scored mode has a reconstruct timer that auto-submits on expiry`() {
        val progressionRepository = FakeProgressionRepository()
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, progressionRepository)
        val reconstructState = advanceToReconstruct(viewModel)
        // submitScore now only fires once the round actually passes - place every target object
        // on its real correct slot and rotation so the timer-expiry auto-submit below has
        // something to award.
        placeAllTargetsCorrectly(viewModel, reconstructState)

        // Scored modes always carry a fixed time limit, unlike Practice.
        val remaining = requireNotNull(reconstructState.remainingMs)

        testDispatcher.scheduler.advanceTimeBy(remaining + 1_000L)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is GameplayUiState.Finished)
        assertTrue(progressionRepository.submitScoreCalled)
    }

    @Test
    fun `a fresh player's first Classic level 1 attempt shows the tutorial instead of starting the timer`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 1), levelNumber = 1)

        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is GameplayUiState.TutorialPending)
    }

    @Test
    fun `dismissing the tutorial starts the real memorize phase fresh`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 1), levelNumber = 1)
        testDispatcher.scheduler.runCurrent()
        val pending = viewModel.uiState.value as GameplayUiState.TutorialPending

        viewModel.dismissTutorial()

        val state = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, state.phase)
        assertEquals(pending.level.memorizeDurationMs, state.remainingMs)
    }

    @Test
    fun `the tutorial never shows again once level 1 has already been passed`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 2), levelNumber = 1)

        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.MEMORIZE, (state as GameplayUiState.InProgress).phase)
    }

    @Test
    fun `the tutorial only ever applies to level 1, never later Classic levels`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 1), levelNumber = 2)

        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is GameplayUiState.InProgress)
    }

    @Test
    fun `the tutorial never shows for non-Classic modes`() {
        val viewModel = viewModel(GameMode.PRACTICE, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 1), levelNumber = 1)

        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is GameplayUiState.InProgress)
    }

    @Test
    fun `reaching level 30 for the first time shows the rotation intro instead of starting the timer`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 30), levelNumber = 30)

        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.MechanicIntroPending)
        assertEquals(NewMechanic.ROTATION, (state as GameplayUiState.MechanicIntroPending).mechanic)
    }

    @Test
    fun `dismissing the rotation intro starts the real memorize phase fresh`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 30), levelNumber = 30)
        testDispatcher.scheduler.runCurrent()
        val pending = viewModel.uiState.value as GameplayUiState.MechanicIntroPending

        viewModel.dismissMechanicIntro()

        val state = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, state.phase)
        assertEquals(pending.level.memorizeDurationMs, state.remainingMs)
    }

    @Test
    fun `the rotation intro never shows again once level 30 has already been passed`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 31), levelNumber = 30)

        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.MEMORIZE, (state as GameplayUiState.InProgress).phase)
    }

    @Test
    fun `reaching level 55 for the first time shows the order-mode intro instead of starting the timer`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 55), levelNumber = 55)

        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.MechanicIntroPending)
        assertEquals(NewMechanic.ORDER_MODE, (state as GameplayUiState.MechanicIntroPending).mechanic)
    }

    @Test
    fun `the order-mode intro never shows again once level 55 has already been passed`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 56), levelNumber = 55)

        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.MEMORIZE, (state as GameplayUiState.InProgress).phase)
    }

    @Test
    fun `a level that is neither 30 nor 55 never shows a mechanic intro`() {
        val viewModel = viewModel(GameMode.CLASSIC, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 31), levelNumber = 31)

        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.MEMORIZE, (state as GameplayUiState.InProgress).phase)
    }

    @Test
    fun `mechanic intros never show for non-Classic modes`() {
        val viewModel = viewModel(GameMode.PRACTICE, campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 30), levelNumber = 30)

        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is GameplayUiState.InProgress)
    }

    @Test
    fun `an early campaign level grants exactly one hint`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 10)
        advanceToReconstruct(viewModel)

        assertEquals(1, viewModel.hintState.value.maxHints)
        assertEquals(1, viewModel.hintState.value.remaining)
    }

    @Test
    fun `a late campaign level grants three hints`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        advanceToReconstruct(viewModel)

        assertEquals(3, viewModel.hintState.value.maxHints)
        assertEquals(3, viewModel.hintState.value.remaining)
    }

    @Test
    fun `toggleHintArmed arms hint mode while hints remain`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        advanceToReconstruct(viewModel)

        viewModel.toggleHintArmed()

        assertTrue(viewModel.hintState.value.isArmed)
    }

    @Test
    fun `toggleHintArmed with no hints remaining denies instead of arming`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 1)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeHints(viewModel, reconstructState, count = 1)
        assertEquals(0, viewModel.hintState.value.remaining)

        var denied = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { viewModel.hintDenied.first(); denied = true }
        testDispatcher.scheduler.runCurrent()

        viewModel.toggleHintArmed()
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.hintState.value.isArmed)
        assertTrue(denied)
    }

    @Test
    fun `requestHint while not armed emits hintDenied and changes nothing`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()

        var denied = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { viewModel.hintDenied.first(); denied = true }
        testDispatcher.scheduler.runCurrent()

        viewModel.requestHint(objectId)
        testDispatcher.scheduler.runCurrent()

        assertTrue(denied)
        assertEquals(0, viewModel.hintState.value.hintsUsed)
        assertNull(viewModel.hintState.value.activeReveal)
    }

    @Test
    fun `requestHint for a valid target reveals its exact slot, decrements remaining, disarms, and records the usage`() {
        val hintRepository = FakeHintRepository()
        val viewModel = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        // trayObjectIds is shuffled (unseeded) at Reconstruct start, so .first() could land on a
        // distractor - pick a real target explicitly instead, which is guaranteed hintable.
        val target = reconstructState.level.objects.first { !it.isDistractor }
        val objectId = target.objectId
        val expectedSlot = target.slotIndex
        val expectedRotation = target.rotationDegrees

        viewModel.toggleHintArmed()
        viewModel.requestHint(objectId)
        testDispatcher.scheduler.runCurrent()

        val hint = viewModel.hintState.value
        assertEquals(HintReveal(objectId, expectedSlot, expectedRotation), hint.activeReveal)
        assertFalse(hint.isArmed)
        assertEquals(1, hint.hintsUsed)
        assertEquals(2, hint.remaining)

        // Written through to the repository immediately (not just held in the in-memory ViewModel
        // state) - this is what lets a process-death restore of the SAME live session pick the
        // usage back up. A brand new ViewModel for this level (a genuine fresh attempt, not a
        // restore) resets this back to 0 instead - see "a fresh attempt at the same level resets a
        // previously used hint budget back to full".
        assertEquals(1, hintRepository.usedCountFor(30))
    }

    @Test
    fun `a fresh attempt at the same level resets a previously used hint budget back to full`() {
        val hintRepository = FakeHintRepository()
        val firstAttempt = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, levelNumber = 30)
        val firstReconstruct = advanceToReconstruct(firstAttempt)
        exhaustFreeHints(firstAttempt, firstReconstruct, count = 3)
        assertEquals(0, firstAttempt.hintState.value.remaining)

        // A brand new ViewModel for the same level, with no live-session snapshot to restore from
        // - exactly what replaying or retrying level 30 produces - must start with a full budget
        // again, not the exhausted one left behind by the first attempt.
        val secondAttempt = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, levelNumber = 30)
        advanceToReconstruct(secondAttempt)
        assertEquals(3, secondAttempt.hintState.value.remaining)
        assertEquals(0, secondAttempt.hintState.value.hintsUsed)
    }

    @Test
    fun `requestHint for an id no longer in the tray is a silent no-op and stays armed`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        advanceToReconstruct(viewModel)
        viewModel.toggleHintArmed()

        viewModel.requestHint("not-a-real-object-id")

        val hint = viewModel.hintState.value
        assertTrue(hint.isArmed)
        assertNull(hint.activeReveal)
        assertEquals(0, hint.hintsUsed)
    }

    @Test
    fun `the active hint reveal clears itself after the reveal window elapses without refunding the hint`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        // Same reasoning as above - .first() on the shuffled tray isn't guaranteed hintable.
        val objectId = reconstructState.level.objects.first { !it.isDistractor }.objectId
        viewModel.toggleHintArmed()
        viewModel.requestHint(objectId)
        testDispatcher.scheduler.runCurrent()
        assertEquals(objectId, viewModel.hintState.value.activeReveal?.objectId)

        testDispatcher.scheduler.advanceTimeBy(3_100L)
        testDispatcher.scheduler.runCurrent()

        val hint = viewModel.hintState.value
        assertNull(hint.activeReveal)
        assertEquals(1, hint.hintsUsed)
    }

    @Test
    fun `watchRewardedAd with a rewarded result grants exactly one hint and records it`() {
        val hintRepository = FakeHintRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeHints(viewModel, reconstructState, count = 3)
        assertEquals(0, viewModel.hintState.value.remaining)

        viewModel.watchRewardedAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.hintState.value.remaining)
        assertEquals(RewardedAdUiState.Idle, viewModel.rewardedHintAdState.value)
        // Written through to the repository (3 real uses, then one refunded by the bonus).
        assertEquals(2, hintRepository.usedCountFor(30))
    }

    @Test
    fun `watchRewardedAd with a cancelled result leaves the hint budget unchanged and notifies the player`() {
        val hintRepository = FakeHintRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Cancelled)
        val viewModel = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeHints(viewModel, reconstructState, count = 3)

        var cancelled = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { viewModel.rewardedHintAdCancelled.first(); cancelled = true }
        testDispatcher.scheduler.runCurrent()

        viewModel.watchRewardedAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.hintState.value.remaining)
        assertEquals(RewardedAdUiState.Idle, viewModel.rewardedHintAdState.value)
        assertTrue(cancelled)
    }

    @Test
    fun `watchRewardedAd with a failed result surfaces the exact failure reason and leaves the budget untouched`() {
        val hintRepository = FakeHintRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.NO_INTERNET))
        val viewModel = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeHints(viewModel, reconstructState, count = 3)

        viewModel.watchRewardedAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.rewardedHintAdState.value
        assertTrue(state is RewardedAdUiState.Failed)
        assertEquals(RewardedAdFailureReason.NO_INTERNET, (state as RewardedAdUiState.Failed).reason)
        assertEquals(0, viewModel.hintState.value.remaining)
    }

    @Test
    fun `watchRewardedAd ignores a second trigger while the first is still loading`() {
        val hintRepository = FakeHintRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(GameMode.PRACTICE, hintRepository = hintRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeHints(viewModel, reconstructState, count = 3)

        // Both calls happen before the dispatcher runs the launched coroutine, so the state is
        // still Loading from the first call when the second one arrives - exactly the race a
        // double-tap on the button would produce.
        viewModel.watchRewardedAd(fakeActivity)
        viewModel.watchRewardedAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, adController.loadAndShowCallCount)
    }

    @Test
    fun `an early campaign level grants exactly one redo`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 10)
        advanceToReconstruct(viewModel)

        assertEquals(1, viewModel.redoState.value.maxRedos)
        assertEquals(1, viewModel.redoState.value.remaining)
    }

    @Test
    fun `a late campaign level grants three redos`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        advanceToReconstruct(viewModel)

        assertEquals(3, viewModel.redoState.value.maxRedos)
        assertEquals(3, viewModel.redoState.value.remaining)
    }

    @Test
    fun `redoLastPlacement returns the most recently placed object to the tray, keeping earlier placements intact`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val firstId = reconstructState.trayObjectIds[0]
        val secondId = reconstructState.trayObjectIds[1]
        viewModel.placeObject(firstId, slotIndex = 0)
        viewModel.placeObject(secondId, slotIndex = 1)

        viewModel.redoLastPlacement()

        val updated = viewModel.uiState.value as GameplayUiState.InProgress
        assertTrue(secondId in updated.trayObjectIds)
        assertTrue(secondId !in updated.placements)
        assertTrue(firstId in updated.placements)
        assertEquals(listOf(firstId), updated.placementOrder)
        assertEquals(1, viewModel.redoState.value.redosUsed)
    }

    @Test
    fun `redoLastPlacement restores the object to the tray without carrying its rotation forward`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 0)
        viewModel.rotatePlacedObject(objectId)
        assertEquals(90, (viewModel.uiState.value as GameplayUiState.InProgress).placements[objectId]?.rotationDegrees)

        viewModel.redoLastPlacement()
        viewModel.placeObject(objectId, slotIndex = 2)

        val updated = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(0, updated.placements[objectId]?.rotationDegrees)
    }

    @Test
    fun `redoLastPlacement is denied when nothing has been placed yet`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        advanceToReconstruct(viewModel)

        var reason: RedoDenialReason? = null
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { reason = viewModel.redoDenied.first() }
        testDispatcher.scheduler.runCurrent()

        viewModel.redoLastPlacement()
        testDispatcher.scheduler.runCurrent()

        assertEquals(RedoDenialReason.NOTHING_TO_UNDO, reason)
        assertEquals(0, viewModel.redoState.value.redosUsed)
    }

    @Test
    fun `redoLastPlacement is denied once the redo budget is exhausted, leaving the placement untouched`() {
        val redoRepository = FakeRedoRepository()
        val viewModel = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, levelNumber = 1)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeRedos(viewModel, reconstructState, count = 1)
        assertEquals(0, viewModel.redoState.value.remaining)

        val objectId = reconstructState.trayObjectIds[1]
        viewModel.placeObject(objectId, slotIndex = 1)

        var reason: RedoDenialReason? = null
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { reason = viewModel.redoDenied.first() }
        testDispatcher.scheduler.runCurrent()

        viewModel.redoLastPlacement()
        testDispatcher.scheduler.runCurrent()

        assertEquals(RedoDenialReason.OUT_OF_USES, reason)
        assertTrue(objectId in (viewModel.uiState.value as GameplayUiState.InProgress).placements)
    }

    @Test
    fun `redoLastPlacement records usage in the repository`() {
        val redoRepository = FakeRedoRepository()
        val viewModel = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 0)

        viewModel.redoLastPlacement()
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, viewModel.redoState.value.remaining)
        // Written through to the repository, same distinction as hint usage - a live session's
        // process-death restore picks this up, but a fresh attempt at this level resets it (see
        // "a fresh attempt at the same level resets a previously used redo budget back to full").
        assertEquals(1, redoRepository.usedCountFor(30))
    }

    @Test
    fun `a fresh attempt at the same level resets a previously used redo budget back to full`() {
        val redoRepository = FakeRedoRepository()
        val firstAttempt = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, levelNumber = 30)
        val firstReconstruct = advanceToReconstruct(firstAttempt)
        exhaustFreeRedos(firstAttempt, firstReconstruct, count = 3)
        assertEquals(0, firstAttempt.redoState.value.remaining)

        val secondAttempt = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, levelNumber = 30)
        advanceToReconstruct(secondAttempt)
        assertEquals(3, secondAttempt.redoState.value.remaining)
        assertEquals(0, secondAttempt.redoState.value.redosUsed)
    }

    @Test
    fun `redoLastPlacement undoes placements in reverse chronological order across multiple redos`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val ids = reconstructState.trayObjectIds.take(3)
        ids.forEachIndexed { index, id -> viewModel.placeObject(id, slotIndex = index) }

        viewModel.redoLastPlacement()
        assertEquals(listOf(ids[0], ids[1]), (viewModel.uiState.value as GameplayUiState.InProgress).placementOrder)

        viewModel.redoLastPlacement()
        assertEquals(listOf(ids[0]), (viewModel.uiState.value as GameplayUiState.InProgress).placementOrder)

        assertEquals(2, viewModel.redoState.value.redosUsed)
    }

    @Test
    fun `watchRewardedRedoAd with a rewarded result grants exactly one redo and records it`() {
        val redoRepository = FakeRedoRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeRedos(viewModel, reconstructState, count = 3)
        assertEquals(0, viewModel.redoState.value.remaining)

        viewModel.watchRewardedRedoAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.redoState.value.remaining)
        assertEquals(RewardedAdUiState.Idle, viewModel.rewardedRedoAdState.value)
        // Written through to the repository (3 real uses, then one refunded by the bonus).
        assertEquals(2, redoRepository.usedCountFor(30))
    }

    @Test
    fun `watchRewardedRedoAd with a cancelled result leaves the redo budget unchanged and notifies the player`() {
        val redoRepository = FakeRedoRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Cancelled)
        val viewModel = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeRedos(viewModel, reconstructState, count = 3)

        var cancelled = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { viewModel.rewardedRedoAdCancelled.first(); cancelled = true }
        testDispatcher.scheduler.runCurrent()

        viewModel.watchRewardedRedoAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.redoState.value.remaining)
        assertEquals(RewardedAdUiState.Idle, viewModel.rewardedRedoAdState.value)
        assertTrue(cancelled)
    }

    @Test
    fun `watchRewardedRedoAd with a failed result surfaces the exact failure reason and leaves the budget untouched`() {
        val redoRepository = FakeRedoRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.NO_INTERNET))
        val viewModel = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeRedos(viewModel, reconstructState, count = 3)

        viewModel.watchRewardedRedoAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.rewardedRedoAdState.value
        assertTrue(state is RewardedAdUiState.Failed)
        assertEquals(RewardedAdFailureReason.NO_INTERNET, (state as RewardedAdUiState.Failed).reason)
        assertEquals(0, viewModel.redoState.value.remaining)
    }

    @Test
    fun `watchRewardedRedoAd ignores a second trigger while the first is still loading`() {
        val redoRepository = FakeRedoRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(GameMode.PRACTICE, redoRepository = redoRepository, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        exhaustFreeRedos(viewModel, reconstructState, count = 3)

        // Both calls happen before the dispatcher runs the launched coroutine, so the state is
        // still Loading from the first call when the second one arrives.
        viewModel.watchRewardedRedoAd(fakeActivity)
        viewModel.watchRewardedRedoAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, adController.loadAndShowCallCount)
    }

    @Test
    fun `watchRewatchAd replays the exact same LevelSpec and never regenerates`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val originalLevel = reconstructState.level

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        val memorizeReplay = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, memorizeReplay.phase)
        // Reference identity, not just structural equality - proves the exact same LevelSpec
        // instance was reused, not a fresh (even if deterministically identical) generation.
        assertSame(originalLevel, memorizeReplay.level)
    }

    @Test
    fun `watchRewatchAd preserves placements, tray, and order through the full replay back into reconstruct`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val placedId = reconstructState.trayObjectIds[0]
        val remainingTrayId = reconstructState.trayObjectIds[1]
        viewModel.placeObject(placedId, slotIndex = 0)
        val beforeRewatch = viewModel.uiState.value as GameplayUiState.InProgress

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()
        val memorizeReplay = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, memorizeReplay.phase)
        // The board state already rides along in the replay's own state, even though nothing
        // renders it while phase == MEMORIZE (GameplayScenePanel shows level.objects then).
        assertEquals(beforeRewatch.placements, memorizeReplay.placements)
        assertEquals(beforeRewatch.trayObjectIds, memorizeReplay.trayObjectIds)

        // One jump (same technique as advanceToReconstruct) crosses both the replay's Memorize
        // duration and the brief Hidden gap that follows it, landing directly back in Reconstruct.
        testDispatcher.scheduler.advanceTimeBy(memorizeReplay.level.memorizeDurationMs + 1_000L)
        testDispatcher.scheduler.runCurrent()

        val afterRewatch = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, afterRewatch.phase)
        assertEquals(beforeRewatch.placements, afterRewatch.placements)
        assertEquals(beforeRewatch.trayObjectIds, afterRewatch.trayObjectIds)
        assertEquals(beforeRewatch.placementOrder, afterRewatch.placementOrder)
        assertTrue(remainingTrayId in afterRewatch.trayObjectIds)
    }

    @Test
    fun `watchRewatchAd pauses and resumes the reconstruct timer with the remaining time intact`() {
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val originalRemaining = requireNotNull(reconstructState.remainingMs)

        // Let some Reconstruct time tick away first, so "resume" is meaningfully different from
        // "restart at the original full limit."
        testDispatcher.scheduler.advanceTimeBy(2_000L)
        testDispatcher.scheduler.runCurrent()
        val remainingBeforeRewatch = requireNotNull((viewModel.uiState.value as GameplayUiState.InProgress).remainingMs)
        assertTrue(remainingBeforeRewatch < originalRemaining)

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()
        val memorizeReplay = viewModel.uiState.value as GameplayUiState.InProgress
        // While replaying, the HUD's own countdown is the replay's Memorize timer, not the
        // paused Reconstruct one.
        assertEquals(memorizeReplay.level.memorizeDurationMs, memorizeReplay.remainingMs)

        testDispatcher.scheduler.advanceTimeBy(memorizeReplay.level.memorizeDurationMs + 1_000L)
        testDispatcher.scheduler.runCurrent()

        // Whatever the exact tick-boundary lands on, the point of "resume" (not "restart") is
        // that it picks back up close to where it was paused, not back at the original full
        // limit - a small tolerance for the scheduler's own tick granularity is expected here,
        // bit-exact timing to the millisecond isn't the behavior under test.
        val afterRewatch = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, afterRewatch.phase)
        val afterRemaining = requireNotNull(afterRewatch.remainingMs)
        assertTrue(afterRemaining < originalRemaining)
        assertTrue(kotlin.math.abs(afterRemaining - remainingBeforeRewatch) <= 1_000L)
    }

    @Test
    fun `watchRewatchAd in practice mode replays and returns to reconstruct with no timer to resume`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        assertNull(reconstructState.remainingMs)

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()
        val memorizeReplay = viewModel.uiState.value as GameplayUiState.InProgress
        testDispatcher.scheduler.advanceTimeBy(memorizeReplay.level.memorizeDurationMs + 1_000L)
        testDispatcher.scheduler.runCurrent()

        val afterRewatch = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, afterRewatch.phase)
        assertNull(afterRewatch.remainingMs)
    }

    @Test
    fun `watchRewatchAd with a cancelled result stays in reconstruct with placements untouched`() {
        val adController = FakeRewardedAdController(result = RewardedAdResult.Cancelled)
        val viewModel = viewModel(GameMode.PRACTICE, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 0)

        var cancelled = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { viewModel.rewatchAdCancelled.first(); cancelled = true }
        testDispatcher.scheduler.runCurrent()

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.RECONSTRUCT, (state as GameplayUiState.InProgress).phase)
        assertTrue(objectId in state.placements)
        assertEquals(RewardedAdUiState.Idle, viewModel.rewatchAdState.value)
        assertTrue(cancelled)
    }

    @Test
    fun `watchRewatchAd with a failed result surfaces the exact failure reason and stays in reconstruct`() {
        val adController = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.AD_UNAVAILABLE))
        val viewModel = viewModel(GameMode.PRACTICE, rewardedAdController = adController, levelNumber = 30)
        advanceToReconstruct(viewModel)

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        val adState = viewModel.rewatchAdState.value
        assertTrue(adState is RewardedAdUiState.Failed)
        assertEquals(RewardedAdFailureReason.AD_UNAVAILABLE, (adState as RewardedAdUiState.Failed).reason)
        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.RECONSTRUCT, (state as GameplayUiState.InProgress).phase)
    }

    @Test
    fun `watchRewatchAd ignores a second trigger while the first is still loading`() {
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(GameMode.PRACTICE, rewardedAdController = adController, levelNumber = 30)
        advanceToReconstruct(viewModel)

        viewModel.watchRewatchAd(fakeActivity)
        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, adController.loadAndShowCallCount)
    }

    @Test
    fun `watchRewatchAd persists rewatch usage for the level`() {
        val rewatchRepository = FakeRewatchRepository()
        val viewModel = viewModel(GameMode.PRACTICE, rewatchRepository = rewatchRepository, levelNumber = 30)
        advanceToReconstruct(viewModel)

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, rewatchRepository.usedCountFor(30))
    }

    // --- Rewatch has no free tier at any level - ad-only end to end ------------------------

    @Test
    fun `an early campaign level grants zero free rewatches`() {
        val viewModel = viewModel(GameMode.CLASSIC, levelNumber = 5)
        advanceToReconstruct(viewModel)

        assertEquals(0, viewModel.rewatchState.value.maxRewatches)
        assertEquals(0, viewModel.rewatchState.value.remaining)
    }

    @Test
    fun `a late campaign level also grants zero free rewatches`() {
        val viewModel = viewModel(GameMode.CLASSIC, levelNumber = 95)
        advanceToReconstruct(viewModel)

        assertEquals(0, viewModel.rewatchState.value.maxRewatches)
        assertEquals(0, viewModel.rewatchState.value.remaining)
    }

    @Test
    fun `watchRewatchFree always denies since Rewatch has no free tier`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 50)
        advanceToReconstruct(viewModel)

        var denied = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { viewModel.rewatchDenied.first(); denied = true }
        testDispatcher.scheduler.runCurrent()

        val beforeState = viewModel.uiState.value
        viewModel.watchRewatchFree()
        testDispatcher.scheduler.runCurrent()

        assertTrue(denied)
        assertEquals(beforeState, viewModel.uiState.value)
    }

    @Test
    fun `watchRewatchFree does nothing outside reconstruct`() {
        val viewModel = viewModel(GameMode.PRACTICE, levelNumber = 50)
        testDispatcher.scheduler.runCurrent() // still in Memorize

        viewModel.watchRewatchFree()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is GameplayUiState.InProgress)
        assertEquals(GamePhase.MEMORIZE, (state as GameplayUiState.InProgress).phase)
        assertEquals(0, viewModel.rewatchState.value.remaining)
    }

    @Test
    fun `the reconstruct timer stays paused through a slow ad and never auto-submits, even past the original deadline`() {
        val rewatchRepository = FakeRewatchRepository()
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(
            GameMode.WEEKLY_CHALLENGE,
            rewatchRepository = rewatchRepository,
            rewardedAdController = adController,
            levelNumber = 30,
        )
        val reconstructState = advanceToReconstruct(viewModel)
        val remaining = requireNotNull(reconstructState.remainingMs)
        // Chosen relative to the level's own remaining time so the ad is still "loading" well
        // past what would have been the original deadline, regardless of how generous a given
        // level's Reconstruct time limit is.
        adController.delayMs = remaining + 10_000L

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent() // enters the ad's delay(); the timer was paused synchronously before this

        // Cross what would have been the original Reconstruct deadline while the ad is still
        // "showing" - the timer was paused the instant the ad started, so this must NOT
        // auto-submit the round out from under the player.
        testDispatcher.scheduler.advanceTimeBy(remaining + 1_000L)
        testDispatcher.scheduler.runCurrent()
        val stillWaiting = viewModel.uiState.value
        assertTrue(stillWaiting is GameplayUiState.InProgress)
        assertEquals(GamePhase.RECONSTRUCT, (stillWaiting as GameplayUiState.InProgress).phase)

        // Let the ad's delay finish and the reward resolve - the timer resumes and the replay
        // begins from Memorize, exactly as an on-time reward would.
        testDispatcher.scheduler.advanceTimeBy(10_000L)
        testDispatcher.scheduler.runCurrent()

        val memorizeReplay = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, memorizeReplay.phase)
        assertEquals(1, rewatchRepository.usedCountFor(30))
    }

    // --- Process-death save-state restore -----------------------------------------------------

    @Test
    fun `restores an in-progress reconstruct board after simulated process death without regenerating the level`() {
        val handle = savedState(GameMode.PRACTICE, levelNumber = 7)
        val levelRepository = FakeLevelRepository()
        val viewModel1 = viewModel(GameMode.PRACTICE, levelNumber = 7, savedStateHandle = handle, levelRepository = levelRepository)
        val reconstructState = advanceToReconstruct(viewModel1)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel1.placeObject(objectId, slotIndex = 0)
        val placedState = viewModel1.uiState.value as GameplayUiState.InProgress
        assertEquals(1, levelRepository.generateCallCount)

        // Simulate process death + recreation: a brand-new ViewModel instance reading the SAME
        // SavedStateHandle (in production, one repopulated from the process's saved Bundle).
        val viewModel2 = viewModel(GameMode.PRACTICE, levelNumber = 7, savedStateHandle = handle, levelRepository = levelRepository)
        testDispatcher.scheduler.runCurrent()

        val restored = viewModel2.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, restored.phase)
        assertEquals(placedState.level.seed, restored.level.seed)
        assertEquals(0, restored.placements[objectId]?.slotIndex)
        assertFalse(objectId in restored.trayObjectIds)
        assertEquals(placedState.trayObjectIds.size, restored.trayObjectIds.size)
        // The level was never regenerated on restore - same "never a new scene" guarantee Rewatch gives a live session.
        assertEquals(1, levelRepository.generateCallCount)
    }

    @Test
    fun `recomputes remaining reconstruct time from the wall clock after process death`() {
        val handle = savedState(GameMode.WEEKLY_CHALLENGE, levelNumber = 20)
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel1 = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 20, savedStateHandle = handle, clock = clock)
        val reconstructState = advanceToReconstruct(viewModel1)
        // The full phase duration, not the live UI value - advanceToReconstruct's own generous
        // overshoot can let a handful of 100ms ticks fire before this point, so the UI's
        // remainingMs is already a little under the full duration by the time we read it here.
        val totalRemaining = requireNotNull(reconstructState.level.timeLimitMs)

        // Real wall-clock time elapses while the process is dead - the coroutine scheduler's own
        // virtual time never advances here, only the wall clock the deadline is anchored to.
        val elapsedWhileDead = 4_000L
        clock.advanceBy(elapsedWhileDead)

        val viewModel2 = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 20, savedStateHandle = handle, clock = clock)
        testDispatcher.scheduler.runCurrent()

        val restored = viewModel2.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, restored.phase)
        val restoredRemaining = requireNotNull(restored.remainingMs)
        val expected = totalRemaining - elapsedWhileDead
        assertTrue("expected ~$expected, was $restoredRemaining", kotlin.math.abs(restoredRemaining - expected) <= 200L)
    }

    @Test
    fun `auto-submits reconstruct if its deadline fully elapsed while the process was dead`() {
        val handle = savedState(GameMode.WEEKLY_CHALLENGE, levelNumber = 21)
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel1 = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 21, savedStateHandle = handle, clock = clock)
        val reconstructState = advanceToReconstruct(viewModel1)
        val totalRemaining = requireNotNull(reconstructState.level.timeLimitMs)

        clock.advanceBy(totalRemaining + 5_000L)

        val viewModel2 = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 21, savedStateHandle = handle, clock = clock)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel2.uiState.value is GameplayUiState.Finished)
    }

    @Test
    fun `restoring a finished round starts a fresh level instead of resurrecting results`() {
        val handle = savedState(GameMode.PRACTICE, levelNumber = 8)
        val levelRepository = FakeLevelRepository()
        val viewModel1 = viewModel(GameMode.PRACTICE, levelNumber = 8, savedStateHandle = handle, levelRepository = levelRepository)
        val reconstructState = advanceToReconstruct(viewModel1)
        reconstructState.trayObjectIds.forEach { viewModel1.placeObject(it, slotIndex = 0) }
        viewModel1.submitReconstruction()
        assertTrue(viewModel1.uiState.value is GameplayUiState.Finished)

        val viewModel2 = viewModel(GameMode.PRACTICE, levelNumber = 8, savedStateHandle = handle, levelRepository = levelRepository)
        testDispatcher.scheduler.runCurrent()

        // Submitting clears the snapshot - a fresh level loads exactly as it would with no saved state at all.
        assertEquals(2, levelRepository.generateCallCount)
        assertTrue(viewModel2.uiState.value is GameplayUiState.InProgress)
    }

    // --- Background-resume timer correction ----------------------------------------------------

    @Test
    fun `onResumed recomputes memorize remaining time after a simulated background gap`() {
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel = viewModel(GameMode.PRACTICE, clock = clock)
        testDispatcher.scheduler.runCurrent()
        val memorizeState = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, memorizeState.phase)
        val totalDuration = memorizeState.level.memorizeDurationMs

        val elapsedInBackground = totalDuration / 3
        clock.advanceBy(elapsedInBackground)
        viewModel.onResumed()

        val corrected = viewModel.uiState.value as GameplayUiState.InProgress
        val correctedRemaining = requireNotNull(corrected.remainingMs)
        val expected = totalDuration - elapsedInBackground
        assertTrue("expected ~$expected, was $correctedRemaining", kotlin.math.abs(correctedRemaining - expected) <= 50L)
    }

    @Test
    fun `onResumed advances past memorize when it fully elapsed in the background`() {
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel = viewModel(GameMode.PRACTICE, clock = clock)
        testDispatcher.scheduler.runCurrent()
        val memorizeState = viewModel.uiState.value as GameplayUiState.InProgress
        val totalDuration = memorizeState.level.memorizeDurationMs

        clock.advanceBy(totalDuration + 5_000L)
        viewModel.onResumed()

        val next = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.HIDDEN, next.phase)
    }

    @Test
    fun `onResumed auto-submits reconstruct when its deadline fully elapsed in the background`() {
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 22, clock = clock)
        val reconstructState = advanceToReconstruct(viewModel)
        val totalRemaining = requireNotNull(reconstructState.remainingMs)

        clock.advanceBy(totalRemaining + 5_000L)
        viewModel.onResumed()

        assertTrue(viewModel.uiState.value is GameplayUiState.Finished)
    }

    @Test
    fun `onResumed firing mid rewatch-replay still resumes the same reconstruct round, not a fresh one`() {
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, rewardedAdController = adController, levelNumber = 30, clock = clock)
        val reconstructState = advanceToReconstruct(viewModel)
        val placedId = reconstructState.trayObjectIds[0]
        viewModel.placeObject(placedId, slotIndex = 0)
        val beforeRewatch = viewModel.uiState.value as GameplayUiState.InProgress
        val pausedRemaining = requireNotNull(beforeRewatch.remainingMs)

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()
        val memorizeReplay = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.MEMORIZE, memorizeReplay.phase)

        // Simulates the exact race this guards against: on a real device there's no ordering
        // guarantee between the ad SDK's own callback chain (which flips adPauseActive back to
        // false via resumeTimerAfterAd) and Android actually dispatching the ON_RESUME lifecycle
        // event for the ad closing - so onResumed can genuinely fire here, mid-replay, with
        // adPauseActive already false. It must still route this MEMORIZE phase through the
        // rewatch-aware completion handler rather than the plain one, which knows nothing about
        // pausedReconstruct and would wipe it.
        clock.advanceBy(500L)
        viewModel.onResumed()

        testDispatcher.scheduler.advanceTimeBy(memorizeReplay.level.memorizeDurationMs + 1_000L)
        testDispatcher.scheduler.runCurrent()

        val afterReplay = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, afterReplay.phase)
        assertEquals(beforeRewatch.placements, afterReplay.placements)
        assertEquals(beforeRewatch.trayObjectIds, afterReplay.trayObjectIds)
        val resumedRemaining = requireNotNull(afterReplay.remainingMs)
        assertTrue(kotlin.math.abs(resumedRemaining - pausedRemaining) <= 1_000L)
    }

    @Test
    fun `onResumed is a no-op during hidden, which has no active phase timer`() {
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel = viewModel(GameMode.PRACTICE, clock = clock)
        testDispatcher.scheduler.runCurrent()
        val memorizeState = viewModel.uiState.value as GameplayUiState.InProgress
        val totalDuration = memorizeState.level.memorizeDurationMs

        // Let Memorize actually finish via its own (virtual-time) timer, landing in Hidden - which
        // has no active CountdownTimer/deadline of its own, just a brief scheduled transition.
        testDispatcher.scheduler.advanceTimeBy(totalDuration + 200L)
        testDispatcher.scheduler.runCurrent()
        val hiddenState = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.HIDDEN, hiddenState.phase)

        clock.advanceBy(10_000L)
        viewModel.onResumed() // Must not throw or change phase - Hidden has nothing to correct.

        assertEquals(GamePhase.HIDDEN, (viewModel.uiState.value as GameplayUiState.InProgress).phase)
    }

    @Test
    fun `onPaused freezes the reconstruct timer with no time lost, and the Privacy Shield gates resume until Continue is tapped`() {
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, levelNumber = 20, clock = clock)
        advanceToReconstruct(viewModel)

        // Let some Reconstruct time tick away first, so "resume" is meaningfully different from
        // "restart at the original full limit."
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()
        val pausedRemaining = requireNotNull((viewModel.uiState.value as GameplayUiState.InProgress).remainingMs)

        viewModel.onPaused()
        assertEquals(PrivacyShieldPhase.HIDDEN_AWAY, (viewModel.uiState.value as GameplayUiState.InProgress).privacyShield)

        // Real wall-clock time passes while backgrounded - unlike onResumed's usual wall-clock
        // catch-up, a true background pause must not charge any of it against the player, the
        // same "no time lost" guarantee a rewarded ad already gets.
        clock.advanceBy(60_000L)
        viewModel.onResumed()

        // Returning to the app alone never resumes anything - the Privacy Shield gates it behind
        // an explicit Continue tap, and the timer stays frozen at its exact paused value the whole
        // time the shield is up, no matter how long that takes.
        val shielded = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(PrivacyShieldPhase.READY_TO_CONTINUE, shielded.privacyShield)
        assertEquals(GamePhase.RECONSTRUCT, shielded.phase)
        assertEquals(pausedRemaining, shielded.remainingMs)
        testDispatcher.scheduler.advanceTimeBy(5_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(pausedRemaining, (viewModel.uiState.value as GameplayUiState.InProgress).remainingMs)

        viewModel.onPrivacyShieldContinue()

        val resumed = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, resumed.phase)
        assertEquals(PrivacyShieldPhase.NONE, resumed.privacyShield)
        assertEquals(pausedRemaining, resumed.remainingMs)
    }

    @Test
    fun `onPaused is a no-op while a rewarded ad is already pausing the round`() {
        val adController = FakeRewardedAdController(result = RewardedAdResult.Rewarded, delayMs = 5_000L)
        val viewModel = viewModel(GameMode.WEEKLY_CHALLENGE, rewardedAdController = adController, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val pausedRemaining = requireNotNull(reconstructState.remainingMs)

        viewModel.watchRewardedAd(fakeActivity)
        testDispatcher.scheduler.runCurrent() // enters the ad's delay(); pauseTimerForAd already fired

        // The app also happens to background while the ad is loading - onPaused must defer
        // entirely to the ad's own freeze rather than layering a second, conflicting one on top.
        viewModel.onPaused()

        testDispatcher.scheduler.advanceTimeBy(5_000L)
        testDispatcher.scheduler.runCurrent()

        val afterReward = viewModel.uiState.value as GameplayUiState.InProgress
        assertEquals(GamePhase.RECONSTRUCT, afterReward.phase)
        assertEquals(pausedRemaining, afterReward.remainingMs)
    }

    // --- Analytics --------------------------------------------------------------------------

    @Test
    fun `starting memorize logs level_started with the mode and level number`() {
        val analytics = FakeAnalyticsLogger()
        viewModel(GameMode.CLASSIC, analyticsLogger = analytics, levelNumber = 40)
        testDispatcher.scheduler.runCurrent()

        val event = analytics.events.single { it.name == "level_started" }
        assertEquals(GameMode.CLASSIC.name, event.params["mode"])
        assertEquals(40, event.params["level_number"])
    }

    @Test
    fun `dismissing the tutorial does not double-log level_started`() {
        val analytics = FakeAnalyticsLogger()
        val viewModel = viewModel(
            GameMode.CLASSIC,
            campaignRepository = FakeLevelCampaignRepository(maxUnlockedLevel = 1),
            analyticsLogger = analytics,
            levelNumber = 1,
        )
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value is GameplayUiState.TutorialPending)
        assertTrue(analytics.events.none { it.name == "level_started" })

        viewModel.dismissTutorial()

        assertEquals(1, analytics.events.count { it.name == "level_started" })
    }

    @Test
    fun `submitting in practice mode logs level_completed with a null passed value`() {
        val analytics = FakeAnalyticsLogger()
        val viewModel = viewModel(GameMode.PRACTICE, analyticsLogger = analytics)
        val reconstructState = advanceToReconstruct(viewModel)
        reconstructState.trayObjectIds.forEach { viewModel.placeObject(it, slotIndex = 0) }

        viewModel.submitReconstruction()

        val event = analytics.events.single { it.name == "level_completed" }
        assertEquals(GameMode.PRACTICE.name, event.params["mode"])
        assertEquals(null, event.params["passed"])
    }

    @Test
    fun `submitting in classic mode logs level_completed with a real passed value`() {
        val analytics = FakeAnalyticsLogger()
        val viewModel = viewModel(GameMode.CLASSIC, analyticsLogger = analytics, levelNumber = 40)
        val reconstructState = advanceToReconstruct(viewModel)
        reconstructState.trayObjectIds.forEach { viewModel.placeObject(it, slotIndex = 0) }

        viewModel.submitReconstruction()
        testDispatcher.scheduler.runCurrent()

        val event = analytics.events.single { it.name == "level_completed" }
        assertEquals(GameMode.CLASSIC.name, event.params["mode"])
        assertTrue(event.params["passed"] is Boolean)
    }

    @Test
    fun `using a hint logs hint_used`() {
        val analytics = FakeAnalyticsLogger()
        val viewModel = viewModel(GameMode.PRACTICE, analyticsLogger = analytics, levelNumber = 30)
        val reconstructState = advanceToReconstruct(viewModel)
        val target = reconstructState.level.objects.first { !it.isDistractor }

        viewModel.toggleHintArmed()
        viewModel.requestHint(target.objectId)

        val event = analytics.events.single { it.name == "hint_used" }
        assertEquals(30, event.params["level_number"])
    }

    @Test
    fun `redoing a placement logs redo_used`() {
        val analytics = FakeAnalyticsLogger()
        val viewModel = viewModel(GameMode.PRACTICE, analyticsLogger = analytics)
        val reconstructState = advanceToReconstruct(viewModel)
        val objectId = reconstructState.trayObjectIds.first()
        viewModel.placeObject(objectId, slotIndex = 0)

        viewModel.redoLastPlacement()

        assertTrue(analytics.events.any { it.name == "redo_used" })
    }

    @Test
    fun `watching a rewatch ad logs rewatch_used`() {
        val analytics = FakeAnalyticsLogger()
        val viewModel = viewModel(GameMode.PRACTICE, analyticsLogger = analytics, levelNumber = 30)
        advanceToReconstruct(viewModel)

        viewModel.watchRewatchAd(fakeActivity)
        testDispatcher.scheduler.runCurrent()

        assertTrue(analytics.events.any { it.name == "rewatch_used" })
    }

    @Test
    fun `clearing the view model logs session_ended with the elapsed duration`() {
        val analytics = FakeAnalyticsLogger()
        val clock = MutableClock(Instant.parse("2026-07-10T12:00:00Z"))
        val viewModel = viewModel(GameMode.PRACTICE, analyticsLogger = analytics, clock = clock)
        testDispatcher.scheduler.runCurrent()

        clock.advanceBy(45_000L)
        invokeOnCleared(viewModel)

        val event = analytics.events.single { it.name == "session_ended" }
        assertEquals(45_000L, event.params["duration_ms"])
    }
}

/** [GameplayViewModel.onCleared] only ever runs via [androidx.lifecycle.ViewModel]'s internal
 * `clear()`, which a JVM unit test has no real `ViewModelStore` to trigger - reflection is the
 * standard, narrowly-scoped way to exercise the same "session ended" cleanup path directly. */
private fun invokeOnCleared(viewModel: GameplayViewModel) {
    val method = GameplayViewModel::class.java.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(viewModel)
}
