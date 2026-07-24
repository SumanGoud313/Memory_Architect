package com.suman.memoryarchitect.core.ads

import android.app.Activity
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeRewardedAdController(
    var result: RewardedAdResult = RewardedAdResult.Rewarded,
    private val delayMs: Long = 0L,
) : RewardedAdController {
    var loadAndShowCallCount = 0
        private set

    override suspend fun loadAndShow(activity: Activity, feature: String): RewardedAdResult {
        loadAndShowCallCount++
        if (delayMs > 0) delay(delayMs)
        return result
    }
}

private class FakeAnalyticsLogger : AnalyticsLogger {
    data class LoggedEvent(val name: String, val params: Map<String, Any?>)

    val events = mutableListOf<LoggedEvent>()

    override fun logEvent(name: String, params: Map<String, Any?>) {
        events += LoggedEvent(name, params)
    }

    override fun setUserProperty(name: String, value: String?) = Unit
}

/**
 * [RewardedAdFlow] is the single reusable engine now backing Hint/Redo/Rewatch (see
 * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]) - these tests exercise it
 * directly, independent of any one feature, so its generic mechanics (re-entrancy, state
 * transitions, event emission) are covered once rather than only indirectly through each
 * feature's own tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RewardedAdFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private val fakeActivity = object : Activity() {}

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts idle`() {
        val flow = RewardedAdFlow(scope, FakeRewardedAdController())
        assertEquals(RewardedAdUiState.Idle, flow.state.value)
    }

    @Test
    fun `watch transitions through loading then back to idle and calls onRewarded on success`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded, delayMs = 1_000L)
        val flow = RewardedAdFlow(scope, controller)
        var rewardedCalled = false

        flow.watch(fakeActivity) { rewardedCalled = true }
        assertEquals(RewardedAdUiState.Loading, flow.state.value)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(RewardedAdUiState.Idle, flow.state.value)
        assertTrue(rewardedCalled)
    }

    @Test
    fun `emits granted after a rewarded outcome`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val flow = RewardedAdFlow(scope, controller)
        var granted = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { flow.granted.first(); granted = true }
        testDispatcher.scheduler.runCurrent()

        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(granted)
    }

    @Test
    fun `does not call onRewarded and emits cancelled when the ad is dismissed unrewarded`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Cancelled)
        val flow = RewardedAdFlow(scope, controller)
        var rewardedCalled = false
        var cancelled = false
        val collectorScope = CoroutineScope(testDispatcher)
        collectorScope.launch { flow.cancelled.first(); cancelled = true }
        testDispatcher.scheduler.runCurrent()

        flow.watch(fakeActivity) { rewardedCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(rewardedCalled)
        assertTrue(cancelled)
        assertEquals(RewardedAdUiState.Idle, flow.state.value)
    }

    @Test
    fun `settles into failed with the reason when the ad fails`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.NO_INTERNET))
        val flow = RewardedAdFlow(scope, controller)

        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val state = flow.state.value
        assertTrue(state is RewardedAdUiState.Failed)
        assertEquals(RewardedAdFailureReason.NO_INTERNET, (state as RewardedAdUiState.Failed).reason)
    }

    @Test
    fun `ignores a second watch call while the first is still loading`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded, delayMs = 1_000L)
        val flow = RewardedAdFlow(scope, controller)

        flow.watch(fakeActivity) {}
        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, controller.loadAndShowCallCount)
    }

    @Test
    fun `a retry after a failure is allowed to load again`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.AD_UNAVAILABLE))
        val flow = RewardedAdFlow(scope, controller)

        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(flow.state.value is RewardedAdUiState.Failed)

        controller.result = RewardedAdResult.Rewarded
        var rewardedCalled = false
        flow.watch(fakeActivity) { rewardedCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, controller.loadAndShowCallCount)
        assertTrue(rewardedCalled)
        assertEquals(RewardedAdUiState.Idle, flow.state.value)
    }

    @Test
    fun `logs a rewarded_ad_result event tagged with the feature name on success`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val analytics = FakeAnalyticsLogger()
        val flow = RewardedAdFlow(scope, controller, analytics, featureName = "hint")

        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val event = analytics.events.single { it.name == "rewarded_ad_result" }
        assertEquals("hint", event.params["feature"])
        assertEquals("rewarded", event.params["result"])
    }

    @Test
    fun `logs a rewarded_ad_result event with the failure reason on failure`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.NO_INTERNET))
        val analytics = FakeAnalyticsLogger()
        val flow = RewardedAdFlow(scope, controller, analytics, featureName = "redo")

        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val event = analytics.events.single { it.name == "rewarded_ad_result" }
        assertEquals("redo", event.params["feature"])
        assertEquals("failed:NO_INTERNET", event.params["result"])
    }

    @Test
    fun `never logs when no analytics logger is provided`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val flow = RewardedAdFlow(scope, controller)

        flow.watch(fakeActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        // No assertion beyond "did not throw" - this is purely confirming the default null
        // analytics param keeps existing callers (and the tests above it) working unchanged.
        assertEquals(RewardedAdUiState.Idle, flow.state.value)
    }

    // --- onBeforeStart / onAfterResolve (timer pause/resume hook) --------------------------

    @Test
    fun `onBeforeStart fires synchronously before the ad load even begins`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded, delayMs = 1_000L)
        val flow = RewardedAdFlow(scope, controller)
        var beforeStartCalled = false

        flow.watch(fakeActivity, onBeforeStart = { beforeStartCalled = true }) {}

        // True immediately after the call returns, before any coroutine dispatch - matching a
        // caller that needs the timer paused the instant the tap is registered, not once the ad
        // has actually started loading on some later dispatch.
        assertTrue(beforeStartCalled)
    }

    @Test
    fun `onAfterResolve fires exactly once after a rewarded outcome`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded)
        val flow = RewardedAdFlow(scope, controller)
        var afterResolveCount = 0

        flow.watch(fakeActivity, onAfterResolve = { afterResolveCount++ }) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, afterResolveCount)
    }

    @Test
    fun `onAfterResolve fires even when the ad is cancelled unrewarded`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Cancelled)
        val flow = RewardedAdFlow(scope, controller)
        var afterResolveCount = 0

        flow.watch(fakeActivity, onAfterResolve = { afterResolveCount++ }) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, afterResolveCount)
    }

    @Test
    fun `onAfterResolve fires even when the ad fails to load`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Failed(RewardedAdFailureReason.NO_INTERNET))
        val flow = RewardedAdFlow(scope, controller)
        var afterResolveCount = 0

        flow.watch(fakeActivity, onAfterResolve = { afterResolveCount++ }) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, afterResolveCount)
    }

    @Test
    fun `a blocked re-entrant watch call never fires onBeforeStart or onAfterResolve again`() {
        val controller = FakeRewardedAdController(result = RewardedAdResult.Rewarded, delayMs = 1_000L)
        val flow = RewardedAdFlow(scope, controller)
        var beforeStartCount = 0
        var afterResolveCount = 0

        flow.watch(fakeActivity, onBeforeStart = { beforeStartCount++ }, onAfterResolve = { afterResolveCount++ }) {}
        flow.watch(fakeActivity, onBeforeStart = { beforeStartCount++ }, onAfterResolve = { afterResolveCount++ }) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, beforeStartCount)
        assertEquals(1, afterResolveCount)
    }
}
