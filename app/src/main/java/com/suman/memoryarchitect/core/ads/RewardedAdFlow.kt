package com.suman.memoryarchitect.core.ads

import android.app.Activity
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logRewardedAdResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the load -> show -> resolve lifecycle for a single rewarded-ad-gated feature - the one
 * piece of logic that used to be copy-pasted three times (Hint/Redo/Rewatch), differing only in
 * what a successful reward actually grants. A ViewModel constructs one instance per ad-gated
 * feature, passing its own [scope] (typically `viewModelScope`) so every launched coroutine is
 * cancelled for free when the ViewModel clears - the same ownership pattern already used for
 * [com.suman.memoryarchitect.feature.gameplay.engine.CountdownTimer] - rather than a singleton
 * manager juggling every feature's state in one place.
 *
 * [watch] is the single entry point and handles every generic edge case once: re-entrancy (a
 * second tap while already [RewardedAdUiState.Loading] is a no-op - the underlying
 * [RewardedAdController] only ever has one ad in flight per call anyway, but this keeps a caller
 * from firing a second redundant load), and all three [RewardedAdResult] outcomes. [onRewarded]
 * is the one thing that's genuinely feature-specific and runs only after the ad has fully
 * resolved; it's a suspend callback specifically so a caller can re-validate against current app
 * state before acting on it (watching an ad can take long enough for a game round to have moved
 * on in the meantime - see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]'s use
 * for Rewatch) rather than trusting a snapshot captured before the ad ever started.
 *
 * [analytics]/[featureName] are optional - a caller that doesn't care about instrumentation can
 * omit both and this behaves exactly as before. When provided, every resolved attempt logs one
 * `rewarded_ad_result` event tagged with [featureName] ("hint"/"redo"/"rewatch"/...), so the three
 * features share one logging call site instead of each needing its own.
 *
 * [onBeforeStart]/[onAfterResolve] are the one other generic hook every ad-gated feature in this
 * app needs: pausing/resuming gameplay's own timer around the ad, so a 30-second ad never silently
 * burns 30 seconds of a countdown the player can't see while it's covered by a full-screen ad (see
 * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.pauseTimerForAd]/
 * `resumeTimerAfterAd`). [onAfterResolve] runs in a `finally` block so it fires exactly once no
 * matter which of the three outcomes below resolves - gameplay always resumes once the ad flow is
 * over, regardless of whether the reward was actually earned.
 */
class RewardedAdFlow(
    private val scope: CoroutineScope,
    private val controller: RewardedAdController,
    private val analytics: AnalyticsLogger? = null,
    private val featureName: String = "unknown",
) {
    private val _state = MutableStateFlow<RewardedAdUiState>(RewardedAdUiState.Idle)
    val state: StateFlow<RewardedAdUiState> = _state.asStateFlow()

    private val _granted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val granted: SharedFlow<Unit> = _granted.asSharedFlow()

    private val _cancelled = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cancelled: SharedFlow<Unit> = _cancelled.asSharedFlow()

    fun watch(
        activity: Activity,
        onBeforeStart: () -> Unit = {},
        onAfterResolve: () -> Unit = {},
        onRewarded: suspend () -> Unit,
    ) {
        if (_state.value is RewardedAdUiState.Loading) return
        onBeforeStart()
        _state.value = RewardedAdUiState.Loading
        scope.launch {
            try {
                when (val result = controller.loadAndShow(activity, featureName)) {
                    is RewardedAdResult.Rewarded -> {
                        _state.value = RewardedAdUiState.Idle
                        onRewarded()
                        _granted.tryEmit(Unit)
                        analytics?.logRewardedAdResult(featureName, "rewarded")
                    }
                    is RewardedAdResult.Cancelled -> {
                        _state.value = RewardedAdUiState.Idle
                        _cancelled.tryEmit(Unit)
                        analytics?.logRewardedAdResult(featureName, "cancelled")
                    }
                    is RewardedAdResult.Failed -> {
                        _state.value = RewardedAdUiState.Failed(result.reason)
                        analytics?.logRewardedAdResult(featureName, "failed:${result.reason.name}")
                    }
                }
            } finally {
                onAfterResolve()
            }
        }
    }
}
