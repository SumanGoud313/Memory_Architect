package com.suman.memoryarchitect.feature.profile

import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.feedback.FeedbackManager
import com.suman.memoryarchitect.core.feedback.ResultMood
import com.suman.memoryarchitect.core.feedback.audio.MusicTrack
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.PurchaseResult
import com.suman.memoryarchitect.domain.model.SpinResult
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import com.suman.memoryarchitect.domain.repository.ShopRepository
import com.suman.memoryarchitect.domain.usecase.ClaimDailyRewardUseCase
import com.suman.memoryarchitect.domain.usecase.GetDailyRewardStatusUseCase
import com.suman.memoryarchitect.domain.usecase.GetEquippedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetLevelCampaignProgressUseCase
import com.suman.memoryarchitect.domain.usecase.GetOwnedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import com.suman.memoryarchitect.domain.usecase.GetStatisticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedAchievementsUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedRewardsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private class FakeProgressionRepository(
    private val profileOutcome: Outcome<PlayerProfile>,
    private val statistics: PlayerStatistics = PlayerStatistics.EMPTY,
    private val unlockedIds: Set<AchievementId> = emptySet(),
    private val unlockedRewardIds: Set<RewardId> = emptySet(),
    private val dailyRewardStatusOutcome: Outcome<DailyRewardStatus> = Outcome.Error(AppError.Network(IOException("offline"))),
) : ProgressionRepository {
    override suspend fun getProfile() = profileOutcome
    override suspend fun getStatistics() = statistics
    override suspend fun getUnlockedAchievementIds() = unlockedIds
    override suspend fun getUnlockedRewardIds() = unlockedRewardIds
    override suspend fun getDailyRewardStatus(todayEpochDay: Long) = dailyRewardStatusOutcome
    override suspend fun claimDailyReward(todayEpochDay: Long): Outcome<DailyRewardClaimResult> =
        throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun submitScore(mode: GameMode, levelSeed: Long, score: ScoreResult, playedOnEpochDay: Long, timeTakenMs: Long, submissionNonce: String): Outcome<ScoreSubmissionResult> =
        throw UnsupportedOperationException("not used by ProfileViewModel")
    override suspend fun resetWinStreak() = Unit
    override suspend fun recordLeaderboardRank(dailyRank: Int?, weeklyRank: Int?, todayEpochDay: Long): List<AchievementId> =
        throw UnsupportedOperationException("not used by ProfileViewModel")
}

private class FakeShopRepository : ShopRepository {
    override suspend fun getOwnedCosmeticIds(): Set<CosmeticId> = emptySet()
    override suspend fun getEquippedCosmetics(): Map<CosmeticCategory, CosmeticId> = emptyMap()
    override suspend fun purchase(id: CosmeticId, purchaseNonce: String): Outcome<PurchaseResult> =
        throw UnsupportedOperationException("not used by ProfileViewModel")
    override suspend fun equip(category: CosmeticCategory, id: CosmeticId): Outcome<Unit> =
        throw UnsupportedOperationException("not used by ProfileViewModel")
    override suspend fun unequip(category: CosmeticCategory): Outcome<Unit> =
        throw UnsupportedOperationException("not used by ProfileViewModel")
    override suspend fun spin(spinNonce: String): Outcome<SpinResult> =
        throw UnsupportedOperationException("not used by ProfileViewModel")
    override suspend fun toggleFavorite(id: CosmeticId) =
        throw UnsupportedOperationException("not used by ProfileViewModel")
    override suspend fun getFavoriteCosmeticIds(): Set<CosmeticId> = emptySet()
    override suspend fun getRecentlyUsedCosmeticIds(limit: Int): List<CosmeticId> = emptyList()
}

private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC)

private class FakeLevelCampaignRepository : LevelCampaignRepository {
    override suspend fun getMaxUnlockedLevel() = 1
    override suspend fun getAllBestTimes() = emptyMap<Int, Long>()
    override suspend fun getAllBestStars() = emptyMap<Int, Int>()
    override suspend fun recordCompletion(levelNumber: Int, timeTakenMs: Long, passed: Boolean, stars: Int): LevelCompletionOutcome =
        throw UnsupportedOperationException("not used by ProfileViewModel")
}

private class FakeAnalyticsLogger : AnalyticsLogger {
    override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `assembles level, xp progress, streak, statistics, and achievements into content`() {
        val repository = FakeProgressionRepository(
            profileOutcome = Outcome.Success(PlayerProfile(xp = 150L, coins = 0L, currentStreak = 2, longestStreak = 5, lastPlayedEpochDay = 10L)),
            statistics = PlayerStatistics(gamesPlayed = 3, totalScore = 450L, bestAccuracy = 0.9f, bestScore = 200),
            unlockedIds = setOf(AchievementId.FIRST_STEPS),
        )
        val viewModel = ProfileViewModel(
            GetPlayerProfileUseCase(repository),
            GetStatisticsUseCase(repository),
            GetUnlockedAchievementsUseCase(repository),
            GetUnlockedRewardsUseCase(repository),
            GetDailyRewardStatusUseCase(repository, fixedClock),
            ClaimDailyRewardUseCase(repository, fixedClock),
            GetLevelCampaignProgressUseCase(FakeLevelCampaignRepository()),
            GetOwnedCosmeticsUseCase(FakeShopRepository()),
            GetEquippedCosmeticsUseCase(FakeShopRepository()),
            FakeAnalyticsLogger(),
            FakeFeedbackManager(),
        )

        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Content)
        state as ProfileUiState.Content
        assertEquals(3, state.statistics.gamesPlayed)
        assertEquals(2, state.profile.currentStreak)
        assertEquals(setOf(AchievementId.FIRST_STEPS), state.unlockedAchievementIds)
    }

    @Test
    fun `surfaces an error state when the profile fetch fails`() {
        val repository = FakeProgressionRepository(profileOutcome = Outcome.Error(AppError.Network(IOException("offline"))))
        val viewModel = ProfileViewModel(
            GetPlayerProfileUseCase(repository),
            GetStatisticsUseCase(repository),
            GetUnlockedAchievementsUseCase(repository),
            GetUnlockedRewardsUseCase(repository),
            GetDailyRewardStatusUseCase(repository, fixedClock),
            ClaimDailyRewardUseCase(repository, fixedClock),
            GetLevelCampaignProgressUseCase(FakeLevelCampaignRepository()),
            GetOwnedCosmeticsUseCase(FakeShopRepository()),
            GetEquippedCosmeticsUseCase(FakeShopRepository()),
            FakeAnalyticsLogger(),
            FakeFeedbackManager(),
        )

        assertTrue(viewModel.uiState.value is ProfileUiState.Error)
    }
}
