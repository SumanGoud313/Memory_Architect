package com.suman.memoryarchitect.feature.ads

import android.app.Activity
import com.suman.memoryarchitect.core.ads.InterstitialAdController
import com.suman.memoryarchitect.core.ads.InterstitialAdResult
import com.suman.memoryarchitect.core.ads.InterstitialPacingGate
import com.suman.memoryarchitect.core.ads.InterstitialPacingPreferences
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RemoteConfig
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import com.suman.memoryarchitect.domain.usecase.GetStatisticsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock

// FakeBillingManager/FakeAnalyticsLogger used below live in AdsTestFakes.kt, shared with
// BannerAdViewModelTest - see that file's doc for why they're not duplicated per-file here.

/** Always resolves to "eligible" (every field at its most permissive) - these tests are about
 * whether the Remove Ads entitlement check happens at all, not about pacing specifics (already
 * covered by InterstitialPacingGateTest). */
private class PermissiveRemoteConfigRepository : RemoteConfigRepository {
    override suspend fun getRemoteConfig(): Outcome<RemoteConfig> = Outcome.Success(
        RemoteConfig(
            values = mapOf(
                "interstitial_ads_enabled" to "true",
                "interstitial_min_level_completions_before_first" to "0",
                "interstitial_min_session_count" to "0",
            ),
            fetchedAt = 0L,
        ),
    )
}

private class FakePreferences : InterstitialPacingPreferences {
    override val sessionCount: Flow<Int> = MutableStateFlow(99)
    override val lastInterstitialShownAtEpochMs: Flow<Long?> = MutableStateFlow(null)
    override suspend fun setLastInterstitialShownAtEpochMs(epochMs: Long) = Unit
    override val lastRewardedAdShownAtEpochMs: Flow<Long?> = MutableStateFlow(null)
    override suspend fun setLastRewardedAdShownAtEpochMs(epochMs: Long) = Unit
    override val gamesPlayedAtLastInterstitial: Flow<Int> = MutableStateFlow(0)
    override suspend fun setGamesPlayedAtLastInterstitial(gamesPlayed: Int) = Unit
    override val interstitialsShownToday: Flow<Int> = MutableStateFlow(0)
    override val interstitialsShownTodayEpochDay: Flow<Long?> = MutableStateFlow(null)
    override suspend fun setInterstitialsShownToday(count: Int, epochDay: Long) = Unit
}

private class FakeInterstitialAdController : InterstitialAdController {
    var loadAndShowCallCount = 0
        private set

    override suspend fun loadAndShow(activity: Activity, placement: String): InterstitialAdResult {
        loadAndShowCallCount++
        return InterstitialAdResult.Shown
    }
}

/** Throws if [getStatistics] is ever called, unless [statistics] is supplied - used to prove the
 * Remove Ads entitlement check short-circuits before this use case is even touched. */
private class FakeProgressionRepository(private val statistics: PlayerStatistics? = null) : com.suman.memoryarchitect.domain.repository.ProgressionRepository {
    override suspend fun getStatistics(): PlayerStatistics = statistics ?: throw AssertionError("getStatistics() should never be called for a Remove Ads purchaser")
    override suspend fun getProfile() = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun getUnlockedAchievementIds() = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun getUnlockedRewardIds() = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun getDailyRewardStatus(todayEpochDay: Long) = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun claimDailyReward(todayEpochDay: Long) = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun claimReturningPlayerGift(todayEpochDay: Long) = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun retryPendingSubmissions() = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun submitScore(mode: com.suman.memoryarchitect.domain.model.GameMode, levelSeed: Long, score: com.suman.memoryarchitect.domain.model.ScoreResult, playedOnEpochDay: Long, timeTakenMs: Long, submissionNonce: String, awardXp: Boolean) =
        throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun resetWinStreak() = throw UnsupportedOperationException("not exercised by these tests")
    override suspend fun recordLeaderboardRank(dailyRank: Int?, weeklyRank: Int?, todayEpochDay: Long) = throw UnsupportedOperationException("not exercised by these tests")
}

/**
 * The specific behavior this suite exists to prove: once a player has purchased Remove Ads,
 * [InterstitialGateViewModel.maybeShowInterstitial] must never attempt to load/show an interstitial
 * again - checked first, before even reading player statistics or consulting the pacing gate (see
 * that function's own doc for why order matters: a purchaser's navigation should never wait on
 * either lookup for an ad that was never going to show).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterstitialGateViewModelTest {

    private val fakeActivity = object : Activity() {}

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun gate(preferences: InterstitialPacingPreferences = FakePreferences()) =
        InterstitialPacingGate(preferences, PermissiveRemoteConfigRepository(), Clock.systemUTC())

    @Test
    fun `a Remove Ads purchaser never triggers an ad load, and completion still fires`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = true)
        val adController = FakeInterstitialAdController()
        val analytics = FakeAnalyticsLogger()
        val viewModel = InterstitialGateViewModel(
            interstitialAdController = adController,
            interstitialPacingGate = gate(),
            billingManager = billingManager,
            getStatistics = GetStatisticsUseCase(FakeProgressionRepository(statistics = null)), // throws if called
            analytics = analytics,
        )

        var completed = false
        viewModel.maybeShowInterstitial(fakeActivity) { completed = true }
        advanceUntilIdle()

        assertTrue("onComplete must still fire so navigation is never blocked", completed)
        assertEquals("no ad should ever be requested for a purchaser", 0, adController.loadAndShowCallCount)
        assertTrue(
            "a Remove Ads purchaser skip should never log interstitial_skipped - it's not a pacing decision",
            analytics.loggedEventNames.none { it == "interstitial_skipped" },
        )
    }

    @Test
    fun `a non-purchaser still sees interstitials load normally when eligible`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val adController = FakeInterstitialAdController()
        val viewModel = InterstitialGateViewModel(
            interstitialAdController = adController,
            interstitialPacingGate = gate(),
            billingManager = billingManager,
            getStatistics = GetStatisticsUseCase(FakeProgressionRepository(statistics = PlayerStatistics(gamesPlayed = 10, totalScore = 0L, bestAccuracy = 0f, bestScore = 0))),
            analytics = FakeAnalyticsLogger(),
        )

        var completed = false
        viewModel.maybeShowInterstitial(fakeActivity) { completed = true }
        advanceUntilIdle()

        assertTrue(completed)
        assertEquals(1, adController.loadAndShowCallCount)
    }

    @Test
    fun `purchasing mid-session stops the very next interstitial attempt, no restart needed`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val adController = FakeInterstitialAdController()
        val viewModel = InterstitialGateViewModel(
            interstitialAdController = adController,
            interstitialPacingGate = gate(),
            billingManager = billingManager,
            getStatistics = GetStatisticsUseCase(FakeProgressionRepository(statistics = PlayerStatistics(gamesPlayed = 10, totalScore = 0L, bestAccuracy = 0f, bestScore = 0))),
            analytics = FakeAnalyticsLogger(),
        )

        viewModel.maybeShowInterstitial(fakeActivity) {}
        advanceUntilIdle()
        assertEquals(1, adController.loadAndShowCallCount)

        // Purchase completes mid-session (BillingManagerImpl flips the shared hasRemovedAds flow).
        billingManager.setHasRemovedAds(true)

        viewModel.maybeShowInterstitial(fakeActivity) {}
        advanceUntilIdle()
        assertEquals("the second attempt after purchasing must not load another ad", 1, adController.loadAndShowCallCount)
    }

    @Test
    fun `a null activity completes immediately without consulting billing state at all`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val adController = FakeInterstitialAdController()
        val viewModel = InterstitialGateViewModel(
            interstitialAdController = adController,
            interstitialPacingGate = gate(),
            billingManager = billingManager,
            getStatistics = GetStatisticsUseCase(FakeProgressionRepository(statistics = null)),
            analytics = FakeAnalyticsLogger(),
        )

        var completed = false
        viewModel.maybeShowInterstitial(null) { completed = true }
        advanceUntilIdle()

        assertTrue(completed)
        assertEquals(0, adController.loadAndShowCallCount)
    }
}
