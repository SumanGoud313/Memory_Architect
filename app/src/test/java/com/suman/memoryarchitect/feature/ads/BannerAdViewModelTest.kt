package com.suman.memoryarchitect.feature.ads

import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.RemoteConfig
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeRemoteConfigRepository(private val bannerEnabled: Boolean = true) : RemoteConfigRepository {
    override suspend fun getRemoteConfig(): Outcome<RemoteConfig> =
        Outcome.Success(RemoteConfig(values = mapOf("banner_ads_enabled" to bannerEnabled.toString()), fetchedAt = 0L))
}

private class FailingRemoteConfigRepository : RemoteConfigRepository {
    override suspend fun getRemoteConfig(): Outcome<RemoteConfig> = Outcome.Error(AppError.FeatureUnavailable)
}

/**
 * [FakeBillingManager]/[FakeAnalyticsLogger] used below live in AdsTestFakes.kt, shared with
 * [InterstitialGateViewModelTest] - see that file's doc.
 *
 * The specific behavior this test suite exists to prove: a Remove Ads purchase completing must stop
 * the banner instantly and reactively, with no screen restart needed - [BannerAdViewModel.shouldShowBanner]
 * is a `combine` over [BillingManager.hasRemovedAds] directly, so flipping that shared singleton flow
 * is the entire mechanism, verified end-to-end here via [FakeBillingManager] rather than assumed.
 *
 * [shouldShowBanner] is built with `SharingStarted.WhileSubscribed`, so a `backgroundScope` collector
 * is required in every test here to actually start the upstream `combine` - reading `.value` alone
 * without ever collecting would just return the flow's initial seed forever.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BannerAdViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `banner shows for a player who has not purchased Remove Ads`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val viewModel = BannerAdViewModel(billingManager, FakeRemoteConfigRepository(bannerEnabled = true), FakeAnalyticsLogger())
        backgroundScope.launch { viewModel.shouldShowBanner.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.shouldShowBanner.value)
    }

    @Test
    fun `banner stops the instant hasRemovedAds flips to true, with no restart needed`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val viewModel = BannerAdViewModel(billingManager, FakeRemoteConfigRepository(bannerEnabled = true), FakeAnalyticsLogger())
        backgroundScope.launch { viewModel.shouldShowBanner.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.shouldShowBanner.value)

        // The moment a purchase completes, BillingManagerImpl flips this exact shared flow -
        // nothing else needs to happen for the banner to disappear.
        billingManager.setHasRemovedAds(true)
        advanceUntilIdle()

        assertFalse(viewModel.shouldShowBanner.value)
    }

    @Test
    fun `banner never shows again after purchase even if Remote Config re-enables banners`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = true)
        val viewModel = BannerAdViewModel(billingManager, FakeRemoteConfigRepository(bannerEnabled = true), FakeAnalyticsLogger())
        backgroundScope.launch { viewModel.shouldShowBanner.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.shouldShowBanner.value)
    }

    @Test
    fun `Remote Config banner_ads_enabled=false hides the banner independent of purchase state`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val viewModel = BannerAdViewModel(billingManager, FakeRemoteConfigRepository(bannerEnabled = false), FakeAnalyticsLogger())
        backgroundScope.launch { viewModel.shouldShowBanner.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.shouldShowBanner.value)
    }

    @Test
    fun `a Remote Config fetch failure keeps the optimistic default rather than hiding the banner`() = runTest {
        val billingManager = FakeBillingManager(hasRemovedAds = false)
        val viewModel = BannerAdViewModel(billingManager, FailingRemoteConfigRepository(), FakeAnalyticsLogger())
        backgroundScope.launch { viewModel.shouldShowBanner.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.shouldShowBanner.value)
    }
}
