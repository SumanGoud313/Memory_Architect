package com.suman.memoryarchitect.feature.gameplay

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.ads.RewardedAdController
import com.suman.memoryarchitect.core.ads.RewardedAdFlow
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.FrustrationTracker
import com.suman.memoryarchitect.core.analytics.PerformanceTracer
import com.suman.memoryarchitect.core.analytics.logAchievementUnlocked
import com.suman.memoryarchitect.core.analytics.logFrustrationSignal
import com.suman.memoryarchitect.core.analytics.logHintUsed
import com.suman.memoryarchitect.core.analytics.logInventoryItemConsumed
import com.suman.memoryarchitect.core.analytics.logLevelAbandonedAfterRewardLimit
import com.suman.memoryarchitect.core.analytics.logLevelCompleted
import com.suman.memoryarchitect.core.analytics.logLevelQuitMidway
import com.suman.memoryarchitect.core.analytics.logLevelRestarted
import com.suman.memoryarchitect.core.analytics.logLevelStarted
import com.suman.memoryarchitect.core.analytics.logMemoryJourneyPointsEarned
import com.suman.memoryarchitect.core.analytics.logMemoryJourneyTierReached
import com.suman.memoryarchitect.core.analytics.logMemorizeFinished
import com.suman.memoryarchitect.core.analytics.logMemorizeStarted
import com.suman.memoryarchitect.core.analytics.logRebuildStarted
import com.suman.memoryarchitect.core.analytics.logRedoUsed
import com.suman.memoryarchitect.core.analytics.logRewardLimitReached
import com.suman.memoryarchitect.core.analytics.logRewardedHintUsed
import com.suman.memoryarchitect.core.analytics.logRewardedRedoUsed
import com.suman.memoryarchitect.core.analytics.logRewardedRewatchUsed
import com.suman.memoryarchitect.core.analytics.logRewatchUsed
import com.suman.memoryarchitect.core.analytics.logSessionEnded
import com.suman.memoryarchitect.core.analytics.logStreakMilestoneReached
import com.suman.memoryarchitect.core.analytics.logStreakShieldConsumed
import com.suman.memoryarchitect.core.analytics.logStreakShieldEarned
import com.suman.memoryarchitect.core.analytics.logSubmitPressed
import com.suman.memoryarchitect.core.analytics.logTimeExpired
import com.suman.memoryarchitect.core.analytics.trace
import com.suman.memoryarchitect.core.feedback.FeedbackManager
import com.suman.memoryarchitect.core.feedback.gameplayMusicTrack
import com.suman.memoryarchitect.domain.hint.HintEngine
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.GlobalLeaderboardStats
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PeriodicLeaderboardSubmission
import com.suman.memoryarchitect.domain.model.PlacedObject
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RedoRules
import com.suman.memoryarchitect.domain.model.RewardedAssistLimits
import com.suman.memoryarchitect.domain.model.RewatchRules
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.progression.LevelCampaignEngine
import com.suman.memoryarchitect.domain.scoring.ScoringEngine
import com.suman.memoryarchitect.domain.scoring.StarRatingCalculator
import com.suman.memoryarchitect.domain.security.LeaderboardAntiCheat
import com.suman.memoryarchitect.domain.usecase.ConsumeInventoryItemUseCase
import com.suman.memoryarchitect.domain.usecase.GenerateLevelUseCase
import com.suman.memoryarchitect.domain.usecase.GetHintsUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetInventoryUseCase
import com.suman.memoryarchitect.domain.usecase.GetLevelCampaignProgressUseCase
import com.suman.memoryarchitect.domain.usecase.GetRedosUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetRewardedHintsUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetRewardedRedosUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetRewardedRewatchesUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetRewatchesUsedUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedAchievementsUseCase
import com.suman.memoryarchitect.domain.usecase.GrantBonusHintUseCase
import com.suman.memoryarchitect.domain.usecase.GrantBonusRedoUseCase
import com.suman.memoryarchitect.domain.usecase.RecordHintUsedUseCase
import com.suman.memoryarchitect.domain.usecase.RecordLevelCompletionUseCase
import com.suman.memoryarchitect.domain.usecase.RecordMissionEventUseCase
import com.suman.memoryarchitect.domain.usecase.RecordRedoUsedUseCase
import com.suman.memoryarchitect.domain.usecase.RecordRewardedRewatchUsedUseCase
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
import com.suman.memoryarchitect.feature.gameplay.engine.CountdownTimer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Sequences a single session through Memorize -> Hidden -> Reconstruct -> Finished. */
@HiltViewModel
class GameplayViewModel @Inject constructor(
    private val generateLevel: GenerateLevelUseCase,
    private val submitScore: SubmitScoreUseCase,
    private val recordLevelCompletion: RecordLevelCompletionUseCase,
    private val getCampaignProgress: GetLevelCampaignProgressUseCase,
    private val submitGlobalLeaderboardStats: SubmitGlobalLeaderboardStatsUseCase,
    private val submitDailyLeaderboardScore: SubmitDailyLeaderboardScoreUseCase,
    private val submitWeeklyLeaderboardScore: SubmitWeeklyLeaderboardScoreUseCase,
    private val submitMonthlyLeaderboardScore: SubmitMonthlyLeaderboardScoreUseCase,
    private val getUnlockedAchievements: GetUnlockedAchievementsUseCase,
    private val resetWinStreak: ResetWinStreakUseCase,
    private val getHintsUsed: GetHintsUsedUseCase,
    private val getRewardedHintsUsed: GetRewardedHintsUsedUseCase,
    private val recordHintUsed: RecordHintUsedUseCase,
    private val grantBonusHint: GrantBonusHintUseCase,
    private val resetHintUsage: ResetHintUsageUseCase,
    private val rewardedAdController: RewardedAdController,
    private val getRedosUsed: GetRedosUsedUseCase,
    private val getRewardedRedosUsed: GetRewardedRedosUsedUseCase,
    private val recordRedoUsed: RecordRedoUsedUseCase,
    private val grantBonusRedo: GrantBonusRedoUseCase,
    private val resetRedoUsage: ResetRedoUsageUseCase,
    private val getRewatchesUsed: GetRewatchesUsedUseCase,
    private val getRewardedRewatchesUsed: GetRewardedRewatchesUsedUseCase,
    private val recordRewatchUsed: RecordRewatchUsedUseCase,
    private val recordRewardedRewatchUsed: RecordRewardedRewatchUsedUseCase,
    private val resetRewatchUsage: ResetRewatchUsageUseCase,
    private val recordMissionEvent: RecordMissionEventUseCase,
    private val clock: Clock,
    private val analytics: AnalyticsLogger,
    private val performanceTracer: PerformanceTracer,
    private val frustrationTracker: FrustrationTracker,
    private val feedback: FeedbackManager,
    private val getInventory: GetInventoryUseCase,
    private val consumeInventoryItem: ConsumeInventoryItemUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Navigation-Compose stores a typed Route.Gameplay's fields under these same flat
    // SavedStateHandle keys (its field names) — reading them directly here (rather than via
    // SavedStateHandle.toRoute<Route.Gameplay>(), which internally round-trips through
    // android.os.Bundle and so can't run in a plain JVM unit test without Robolectric) keeps
    // this ViewModel decoupled from the ui.navigation package and easily testable.
    private val mode = savedStateHandle.get<String>(ARG_MODE)
        ?.let { runCatching { GameMode.valueOf(it) }.getOrNull() }
        ?: GameMode.PRACTICE
    private val difficultyTier = savedStateHandle.get<String>(ARG_DIFFICULTY)
        ?.let { runCatching { DifficultyTier.valueOf(it) }.getOrNull() }
        ?: DifficultyTier.MEDIUM
    private val levelNumber = savedStateHandle.get<Int>(ARG_LEVEL_NUMBER) ?: 1

    // Classic never reads [difficultyTier] for generation (its own per-level-number curve drives
    // that instead, via campaignEngine.tierFor - see loadLevel) - this is the single place that
    // resolves whichever tier actually applied to *this* attempt, for analytics only, so
    // level_started/level_completed can report a real difficulty value for every mode instead of
    // only Practice/Daily/Weekly.
    private val analyticsDifficultyTier: DifficultyTier
        get() = if (mode == GameMode.CLASSIC) campaignEngine.tierFor(levelNumber) else difficultyTier

    private val _uiState = MutableStateFlow<GameplayUiState>(GameplayUiState.Loading)
    val uiState: StateFlow<GameplayUiState> = _uiState.asStateFlow()

    /** Fires when the player explicitly taps Submit while objects remain in the tray — the UI
     * disables the button whenever that's true, so this only exists as a defensive fallback
     * (stale state, accessibility tooling, etc.) never as the primary guard. Carries the
     * remaining-object count so the Snackbar can say exactly how many are left. */
    private val _submitRejected = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val submitRejected: SharedFlow<Int> = _submitRejected.asSharedFlow()

    private val _hintState = MutableStateFlow(HintUiState.Initial)
    val hintState: StateFlow<HintUiState> = _hintState.asStateFlow()

    /** Fires when a hint is requested outside the UI's own guard (button disabled at 0
     * remaining, tray taps only matter while armed) - the same defensive-fallback role
     * [submitRejected] plays for Submit. */
    private val _hintDenied = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hintDenied: SharedFlow<Unit> = _hintDenied.asSharedFlow()

    // One RewardedAdFlow per ad-gated feature - the load/show/resolve mechanics (re-entrancy
    // guard, Loading/Failed/Cancelled state, the granted/cancelled events) live once in that
    // class instead of being copy-pasted per feature; only what each reward actually grants
    // differs, and that lives in each watchX method below as an onRewarded callback.
    private val hintAdFlow = RewardedAdFlow(viewModelScope, rewardedAdController, analytics, featureName = "hint")
    val rewardedHintAdState: StateFlow<RewardedAdUiState> = hintAdFlow.state
    val rewardedHintGranted: SharedFlow<Unit> = hintAdFlow.granted
    val rewardedHintAdCancelled: SharedFlow<Unit> = hintAdFlow.cancelled

    private val _redoState = MutableStateFlow(RedoUiState.Initial)
    val redoState: StateFlow<RedoUiState> = _redoState.asStateFlow()

    /** Fires when Redo is requested outside the UI's own guard (button disabled with no budget
     * or nothing placed) - the same defensive-fallback role [hintDenied] plays for Hint. */
    private val _redoDenied = MutableSharedFlow<RedoDenialReason>(extraBufferCapacity = 1)
    val redoDenied: SharedFlow<RedoDenialReason> = _redoDenied.asSharedFlow()

    private val redoAdFlow = RewardedAdFlow(viewModelScope, rewardedAdController, analytics, featureName = "redo")
    val rewardedRedoAdState: StateFlow<RewardedAdUiState> = redoAdFlow.state
    val rewardedRedoGranted: SharedFlow<Unit> = redoAdFlow.granted
    val rewardedRedoAdCancelled: SharedFlow<Unit> = redoAdFlow.cancelled

    private val _rewatchState = MutableStateFlow(RewatchUiState.Initial)
    val rewatchState: StateFlow<RewatchUiState> = _rewatchState.asStateFlow()

    /** Fires when a free Rewatch is requested outside the UI's own guard (button only rendered
     * while [RewatchUiState.remaining] is positive) - the same defensive-fallback role
     * [hintDenied]/[redoDenied] play for their own features. */
    private val _rewatchDenied = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rewatchDenied: SharedFlow<Unit> = _rewatchDenied.asSharedFlow()

    // No "granted" flow wired up for Rewatch's ad path - unlike Hint/Redo, a successful reward
    // here is a visible phase transition (the scene replaying), not an invisible counter bump, so
    // there's nothing a Snackbar would usefully add. See watchRewatchAd for why.
    private val rewatchAdFlow = RewardedAdFlow(viewModelScope, rewardedAdController, analytics, featureName = "rewatch")
    val rewatchAdState: StateFlow<RewardedAdUiState> = rewatchAdFlow.state
    val rewatchAdCancelled: SharedFlow<Unit> = rewatchAdFlow.cancelled

    private val timer = CountdownTimer(viewModelScope)
    private val scoringEngine = ScoringEngine()
    private val campaignEngine = LevelCampaignEngine()
    private val hintEngine = HintEngine()
    private val redoRules = RedoRules.Default
    private val rewatchRules = RewatchRules.Default
    private val rewardedLimits = RewardedAssistLimits.Default
    private var reconstructStartedAt: Instant? = null
    private var hintExpiryJob: Job? = null

    /** True from the moment any of Hint/Redo/Rewatch's rewarded-ad budget is fully spent for this
     * attempt (see [rewardedLimits]) until the next fresh attempt ([loadLevel] resets it) - lets
     * [onCleared] tell "quit after hitting a reward wall" apart from an ordinary quit, without
     * threading that distinction through every one of the three watchX methods that can set it. */
    private var rewardLimitReachedThisAttempt = false

    /** Fires once, the instant Hint/Redo/Rewatch's rewarded-ad budget is fully spent for this
     * attempt - the trigger for the "No more X rewards available for this level" Snackbar (see
     * [GameplayScreen]), distinct from the ordinary [hintDenied]/[redoDenied]/[rewatchDenied]
     * defensive-fallback flows above, which cover a *free*-budget denial instead. */
    private val _hintRewardLimitReached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hintRewardLimitReached: SharedFlow<Unit> = _hintRewardLimitReached.asSharedFlow()

    private val _redoRewardLimitReached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val redoRewardLimitReached: SharedFlow<Unit> = _redoRewardLimitReached.asSharedFlow()

    private val _rewatchRewardLimitReached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rewatchRewardLimitReached: SharedFlow<Unit> = _rewatchRewardLimitReached.asSharedFlow()

    // Domain models (LevelSpec/PlacedObject/GamePhase) stay free of @JsonClass annotations - this
    // adapter is scoped locally to session-snapshot persistence rather than pulling serialization
    // concerns into the domain layer.
    private val snapshotAdapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        .adapter(GameplaySessionSnapshot::class.java)

    /** Wall-clock deadline (via [clock], not a raw remaining-ms count) for whichever timer is
     * currently running (Memorize or Reconstruct) - null whenever the current phase has none
     * (Hidden's brief gap, Practice's untimed Rebuild, Finished). Recomputing remaining time from
     * this on [onResumed] and on process-death restore is what makes the countdown reflect real
     * elapsed time rather than however many ticks the timer's own coroutine happened to run. */
    private var phaseDeadlineEpochMs: Long? = null

    /** True for the entire span from the instant a rewarded ad is requested (Hint, Redo, or
     * Rewatch - see [pauseTimerForAd]) through the instant [resumeTimerAfterAd] finishes restoring
     * the timer, including whatever suspend work the ad's own reward callback does in between.
     * [onResumed] checks this before doing its own wall-clock correction - closing a rewarded ad
     * fires the exact same `ON_RESUME` lifecycle event a real backgrounding would, and without this
     * guard that correction would treat "how long the ad was on screen" as lost time. */
    private var adPauseActive = false

    /** True while the app is genuinely backgrounded (app minimized, phone locked, an incoming
     * call, audio focus lost to another app) - see [onPaused]/[onResumed]. Deliberately a true
     * freeze, the same "no time lost" guarantee [adPauseActive] gives a rewarded ad, rather than
     * [onResumed]'s usual wall-clock catch-up: unlike a full process death (see
     * [restoreFromSnapshot], which never sees this flag - it's in-memory-only state that doesn't
     * survive that), the process stays alive here, so there's a real paused [CountdownTimer] to
     * resume from the exact value it was paused at instead of recomputing elapsed real time. */
    private var backgroundPauseActive = false

    /** Captured the instant a Rewatch replay begins - exactly how much Reconstruct time was left
     * and where every object stood - so [resumeReconstructAfterRewatch] can put the round back
     * exactly as it was rather than starting fresh. */
    private data class PausedReconstruct(
        val placements: Map<String, PlacedObject>,
        val trayObjectIds: List<String>,
        val placementOrder: List<String>,
        val remainingMs: Long?,
    )
    private var pausedReconstruct: PausedReconstruct? = null

    // Captured once, not re-captured across process-death/rotation restarts (a restored session
    // is still the same session from the player's perspective) - session length is measured from
    // when this ViewModel instance was first created, reported once in onCleared().
    private val sessionStartedAt: Instant = clock.instant()

    init {
        viewModelScope.launch {
            timer.remainingMs.collect { remaining ->
                val current = _uiState.value
                if (current is GameplayUiState.InProgress) {
                    _uiState.value = current.copy(remainingMs = remaining)
                    feedback.onTimerTick(remaining, isReconstructPhase = current.phase == GamePhase.RECONSTRUCT)
                }
            }
        }
        val snapshotJson = savedStateHandle.get<String>(SNAPSHOT_KEY)
        val snapshot = snapshotJson?.let { runCatching { snapshotAdapter.fromJson(it) }.getOrNull() }
        if (snapshot != null) restoreFromSnapshot(snapshot) else loadLevel()
    }

    fun retry() = loadLevel()

    /** Called once the tutorial overlay finishes (played through or skipped) — starts the real
     * Memorize timer fresh, so the player's actual time budget is never eaten into by however
     * long the demo took to watch. */
    fun dismissTutorial() {
        val current = _uiState.value
        if (current is GameplayUiState.TutorialPending) {
            startMemorizePhase(current.level)
        }
    }

    fun placeObject(objectId: String, slotIndex: Int) {
        updateReconstructState { state ->
            state.copy(
                placements = state.placements + (objectId to PlacedObject(objectId, slotIndex, rotationDegrees = 0)),
                trayObjectIds = state.trayObjectIds - objectId,
                placementOrder = state.placementOrder - objectId + objectId,
            )
        }
        feedback.onObjectPlace()
    }

    fun rotatePlacedObject(objectId: String) {
        updateReconstructState { state ->
            val placed = state.placements[objectId] ?: return@updateReconstructState state
            val rotated = placed.copy(rotationDegrees = (placed.rotationDegrees + ROTATION_STEP_DEGREES) % 360)
            state.copy(placements = state.placements + (objectId to rotated))
        }
        feedback.onObjectRotate()
    }

    fun pickUpPlacedObject(objectId: String) {
        updateReconstructState { state ->
            if (objectId !in state.placements) return@updateReconstructState state
            state.copy(
                placements = state.placements - objectId,
                trayObjectIds = state.trayObjectIds + objectId,
                placementOrder = state.placementOrder - objectId,
            )
        }
        feedback.onObjectPickup()
    }

    /** Undoes the most recently PLACED object still on the board (by [GameplayUiState.InProgress
     * .placementOrder], not by rotation - rotating a placed object never moves it in that order),
     * returning it to the tray exactly the way manually picking it up already does. Reusing
     * [pickUpPlacedObject] verbatim also means rotation is "restored" for free: a placed object's
     * rotation only ever lives on its [PlacedObject] entry, which pickup discards entirely - the
     * object always re-enters the tray at its default, unrotated appearance, and gets rotationDegrees
     * = 0 again if re-placed, the same as any other tray object. */
    fun redoLastPlacement() {
        val gameplay = _uiState.value
        val redo = _redoState.value
        if (gameplay !is GameplayUiState.InProgress || gameplay.phase != GamePhase.RECONSTRUCT) return
        val lastPlacedObjectId = gameplay.placementOrder.lastOrNull()
        if (lastPlacedObjectId == null) {
            _redoDenied.tryEmit(RedoDenialReason.NOTHING_TO_UNDO)
            feedback.onWarning()
            return
        }
        if (!redo.canRedo) {
            _redoDenied.tryEmit(RedoDenialReason.OUT_OF_USES)
            feedback.onWarning()
            return
        }
        pickUpPlacedObject(lastPlacedObjectId)
        val redosUsed = redo.redosUsed + 1
        _redoState.value = redo.copy(redosUsed = redosUsed)
        analytics.logRedoUsed(mode, levelNumber)
        if (redosUsed == EXCESSIVE_ASSIST_THRESHOLD) {
            analytics.logFrustrationSignal("excessive_redo", mode, levelNumber, detail = redosUsed.toString())
        }
        viewModelScope.launch { recordRedoUsed(levelNumber) }
    }

    /** Refreshes [HintUiState.hasInventoryToken]/[RedoUiState.hasInventoryToken]/
     * [RewatchUiState.hasInventoryToken] against the player's current Inventory - called once
     * after a fresh level load or session restore (see [loadLevel]/[restoreFromSnapshot]) and
     * again after each token redemption below, since Inventory's balance can change between level
     * attempts (a mission claimed, a token just spent) in a way this ViewModel doesn't otherwise
     * observe. */
    private fun refreshInventoryTokenFlags() {
        viewModelScope.launch {
            val inventory = (getInventory() as? Outcome.Success)?.data ?: return@launch
            _hintState.value = _hintState.value.copy(inventoryTokenCount = inventory.quantityOf(InventoryItemKind.HINT_TOKEN))
            _redoState.value = _redoState.value.copy(inventoryTokenCount = inventory.quantityOf(InventoryItemKind.REDO_TOKEN))
            _rewatchState.value = _rewatchState.value.copy(inventoryTokenCount = inventory.quantityOf(InventoryItemKind.REWATCH_TICKET))
        }
    }

    /** Redeems one [InventoryItemKind.HINT_TOKEN] for one free hint - fills the exact same slot a
     * rewarded ad would (see [HintUiState.canUseInventoryToken]), reusing [grantBonusHint] (the
     * same grant [watchRewardedAd] itself calls) rather than a separate token-specific mechanism.
     * No-op unless [HintUiState.canUseInventoryToken] is already true. [HintUiState.isRedeemingToken]
     * is set for the whole redemption and checked up front, so a second rapid tap arriving before
     * this one's state update lands is a no-op too, not a second concurrent [consumeInventoryItem]
     * call - the one exactly-once guarantee this method must hold. */
    fun useInventoryHintToken() {
        val hint = _hintState.value
        if (!hint.canUseInventoryToken || hint.isRedeemingToken) return
        _hintState.value = hint.copy(isRedeemingToken = true)
        viewModelScope.launch {
            try {
                if (consumeInventoryItem(InventoryItemKind.HINT_TOKEN) !is Outcome.Success) return@launch
                grantBonusHint(levelNumber)
                _hintState.value = _hintState.value.copy(hintsUsed = (_hintState.value.hintsUsed - 1).coerceAtLeast(0))
                analytics.logInventoryItemConsumed(InventoryItemKind.HINT_TOKEN.name, 1)
                refreshInventoryTokenFlags()
            } finally {
                _hintState.value = _hintState.value.copy(isRedeemingToken = false)
            }
        }
    }

    /** Redeems one [InventoryItemKind.REDO_TOKEN] for one free redo - see [useInventoryHintToken]'s
     * doc for the same reasoning (including the [RedoUiState.isRedeemingToken] re-entrancy guard),
     * reusing [grantBonusRedo]. */
    fun useInventoryRedoToken() {
        val redo = _redoState.value
        if (!redo.canUseInventoryToken || redo.isRedeemingToken) return
        _redoState.value = redo.copy(isRedeemingToken = true)
        viewModelScope.launch {
            try {
                if (consumeInventoryItem(InventoryItemKind.REDO_TOKEN) !is Outcome.Success) return@launch
                grantBonusRedo(levelNumber)
                _redoState.value = _redoState.value.copy(redosUsed = (_redoState.value.redosUsed - 1).coerceAtLeast(0))
                analytics.logInventoryItemConsumed(InventoryItemKind.REDO_TOKEN.name, 1)
                refreshInventoryTokenFlags()
            } finally {
                _redoState.value = _redoState.value.copy(isRedeemingToken = false)
            }
        }
    }

    /** Redeems one [InventoryItemKind.REWATCH_TICKET] for one free scene replay - mirrors
     * [watchRewatchAd]'s reward callback exactly (same [recordRewardedRewatchUsed] bucket, since
     * there's no separate token-granted counter - a ticket-granted rewatch is bounded by
     * [RewardedAssistLimits.maxRewardedRewatches] the same way an ad-granted one is), just without
     * the ad round-trip. No-op unless [RewatchUiState.canUseInventoryToken] is already true, or
     * the round has since left Reconstruct. Same [RewatchUiState.isRedeemingToken] re-entrancy
     * guard as [useInventoryHintToken]. */
    fun useInventoryRewatchToken() {
        val gameplay = _uiState.value
        if (gameplay !is GameplayUiState.InProgress || gameplay.phase != GamePhase.RECONSTRUCT) return
        val rewatch = _rewatchState.value
        if (!rewatch.canUseInventoryToken || rewatch.isRedeemingToken) return
        _rewatchState.value = rewatch.copy(isRedeemingToken = true)
        viewModelScope.launch {
            try {
                if (consumeInventoryItem(InventoryItemKind.REWATCH_TICKET) !is Outcome.Success) return@launch
                val rewardedUsed = rewatch.rewardedRewatchesUsed + 1
                _rewatchState.value = _rewatchState.value.copy(rewardedRewatchesUsed = rewardedUsed)
                recordRewardedRewatchUsed(levelNumber)
                analytics.logInventoryItemConsumed(InventoryItemKind.REWATCH_TICKET.name, 1)
                logExcessiveRewatchIfNeeded(rewatch.rewatchesUsed + rewardedUsed)
                refreshInventoryTokenFlags()
                replayScene(gameplay)
            } finally {
                _rewatchState.value = _rewatchState.value.copy(isRedeemingToken = false)
            }
        }
    }

    /** Arms/disarms hint-selection mode - only meaningful during Reconstruct, and only when at
     * least one hint remains. Requesting arm with none remaining is the defensive-denial path
     * ([hintDenied]); the UI's Hint control should already be disabled at that point. */
    fun toggleHintArmed() {
        val gameplay = _uiState.value
        val hint = _hintState.value
        if (gameplay !is GameplayUiState.InProgress || gameplay.phase != GamePhase.RECONSTRUCT) return
        if (!hint.isArmed && !hint.canUseHint) {
            _hintDenied.tryEmit(Unit)
            feedback.onWarning()
            return
        }
        _hintState.value = hint.copy(isArmed = !hint.isArmed)
    }

    /** Called when the player taps a tray object while hint mode is armed. Always disarms and
     * always consumes a hint on a valid, still-in-tray object - even when that object turns out
     * to be a distractor with nothing to reveal - so probing the tray for distractors is never
     * free. [HintEngine.revealFor] returning null (distractor, or a stale/invalid id) is a silent
     * no-op instead: the tap simply does nothing, exactly as it always has outside hint mode. */
    fun requestHint(objectId: String) {
        val gameplay = _uiState.value
        val hint = _hintState.value
        if (gameplay !is GameplayUiState.InProgress || gameplay.phase != GamePhase.RECONSTRUCT) return
        if (!hint.isArmed || !hint.canUseHint) {
            _hintDenied.tryEmit(Unit)
            feedback.onWarning()
            return
        }
        if (objectId !in gameplay.trayObjectIds) return
        val reveal = hintEngine.revealFor(objectId, gameplay.level) ?: return

        hintExpiryJob?.cancel()
        val hintsUsed = hint.hintsUsed + 1
        _hintState.value = hint.copy(isArmed = false, hintsUsed = hintsUsed, activeReveal = reveal)
        analytics.logHintUsed(mode, levelNumber)
        if (hintsUsed == EXCESSIVE_ASSIST_THRESHOLD) {
            analytics.logFrustrationSignal("excessive_hints", mode, levelNumber, detail = hintsUsed.toString())
        }
        viewModelScope.launch { recordHintUsed(levelNumber) }
        hintExpiryJob = viewModelScope.launch {
            delay(HINT_REVEAL_DURATION_MS)
            _hintState.value = _hintState.value.copy(activeReveal = null)
        }
    }

    /** Only reachable once [HintUiState.remaining] is already zero - the UI swaps [HintButton]
     * for this entirely at that point, so there's no arm/select step here, just load-and-show.
     * [activity] is only ever passed through to [RewardedAdController.loadAndShow] (via
     * [hintAdFlow]) for the duration of that single call, never retained on this ViewModel.
     * [pauseTimerForAd]/[resumeTimerAfterAd] freeze/restore the Reconstruct countdown around the
     * whole ad lifecycle - see their own docs for why.
     *
     * The pre-check below (bail before ever starting the ad) and the re-check inside the reward
     * callback (bail before granting) are both needed for the same reason [watchRewatchAd]'s
     * doc explains: the ad's own load/show round-trip can outlast the cap being hit some other
     * way in the meantime, so re-validating only once wouldn't be airtight. Once
     * [HintUiState.rewardedRemaining] hits zero, no further ad can ever grant another hint for
     * this level attempt - see [RewardedAssistLimits]. */
    fun watchRewardedAd(activity: Activity) {
        if (!_hintState.value.canWatchRewardedAd) return
        hintAdFlow.watch(activity, onBeforeStart = ::pauseTimerForAd, onAfterResolve = ::resumeTimerAfterAd) {
            val hint = _hintState.value
            if (!hint.canWatchRewardedAd) return@watch
            grantBonusHint(levelNumber)
            val rewardedUsed = hint.rewardedHintsUsed + 1
            _hintState.value = hint.copy(
                hintsUsed = (hint.hintsUsed - 1).coerceAtLeast(0),
                rewardedHintsUsed = rewardedUsed,
            )
            analytics.logRewardedHintUsed(mode, levelNumber, rewardedUsed)
            recordMissionEvent(MissionEvent.RewardedAdWatched)
            if (rewardedUsed >= rewardedLimits.maxRewardedHints) {
                rewardLimitReachedThisAttempt = true
                analytics.logRewardLimitReached("hint", mode, levelNumber)
                _hintRewardLimitReached.tryEmit(Unit)
            }
        }
    }

    /** Only reachable once [RedoUiState.remaining] is already zero - the UI swaps [RedoButton]
     * for this entirely at that point, mirroring [watchRewardedAd]'s relationship to
     * [HintButton]. Reuses the exact same [RewardedAdController] as the Hint flow - watching an
     * ad is watching an ad, regardless of which resource the reward ultimately grants - but is
     * tracked by its own [redoAdFlow] instance so the two ad flows never interfere with each
     * other. Capped the same way [watchRewardedAd] is - see its doc for why both the pre-check and
     * the re-check inside the reward callback are needed. */
    fun watchRewardedRedoAd(activity: Activity) {
        if (!_redoState.value.canWatchRewardedAd) return
        redoAdFlow.watch(activity, onBeforeStart = ::pauseTimerForAd, onAfterResolve = ::resumeTimerAfterAd) {
            val redo = _redoState.value
            if (!redo.canWatchRewardedAd) return@watch
            grantBonusRedo(levelNumber)
            val rewardedUsed = redo.rewardedRedosUsed + 1
            _redoState.value = redo.copy(
                redosUsed = (redo.redosUsed - 1).coerceAtLeast(0),
                rewardedRedosUsed = rewardedUsed,
            )
            analytics.logRewardedRedoUsed(mode, levelNumber, rewardedUsed)
            recordMissionEvent(MissionEvent.RewardedAdWatched)
            if (rewardedUsed >= rewardedLimits.maxRewardedRedos) {
                rewardLimitReachedThisAttempt = true
                analytics.logRewardLimitReached("redo", mode, levelNumber)
                _redoRewardLimitReached.tryEmit(Unit)
            }
        }
    }

    /** Free-tier counterpart to [watchRewatchAd] - only reachable while [RewatchUiState.remaining]
     * is positive. [RewatchRules.Default] currently grants zero free rewatches at every level
     * (Rewatch is ad-only end to end), so this is effectively dead in normal play today; left in
     * place rather than deleted since the exact same "swap to the ad fallback whenever the free
     * budget is zero" mechanism in [GameplayToolbar] already has to handle a zero budget
     * correctly regardless, and a future free-tier reintroduction would need this back anyway. */
    fun watchRewatchFree() {
        val gameplay = _uiState.value
        val rewatch = _rewatchState.value
        if (gameplay !is GameplayUiState.InProgress || gameplay.phase != GamePhase.RECONSTRUCT) return
        if (!rewatch.canRewatchFree) {
            _rewatchDenied.tryEmit(Unit)
            feedback.onWarning()
            return
        }
        val rewatchesUsed = rewatch.rewatchesUsed + 1
        _rewatchState.value = rewatch.copy(rewatchesUsed = rewatchesUsed)
        analytics.logRewatchUsed(mode, levelNumber)
        logExcessiveRewatchIfNeeded(rewatchesUsed)
        viewModelScope.launch { recordRewatchUsed(levelNumber) }
        replayScene(gameplay)
    }

    /** Always ad-gated once the free tier (if any) is spent - reuses the exact same [LevelSpec]
     * already loaded for this attempt - [replayScene] never calls [generateLevel] again - so
     * objects, lighting, positions, rotations, and furniture (all seed/level-derived, see
     * [GameplayScenePanel]) replay identically, never a new scene.
     *
     * The pre-check below (bail before ever starting the ad) and the re-check inside
     * [onRewarded] (bail before acting on it) are deliberately both needed: the ad itself
     * (load + show) can take long enough that a short Reconstruct countdown auto-submits while
     * it's still showing, so the round can go stale either before the ad starts or while it's in
     * flight - re-validating only once wouldn't cover both windows. A reward that resolves too
     * late to matter can never resurrect an already-Finished round.
     *
     * Also capped by [RewatchUiState.rewardedRemaining] (see [RewardedAssistLimits]) the same way
     * [watchRewardedAd]/[watchRewardedRedoAd] are - checked both before the ad ever starts and
     * again inside the reward callback, for the same "the round can go stale either before or
     * during the ad" reason already covered above. */
    fun watchRewatchAd(activity: Activity) {
        val gameplay = _uiState.value
        if (gameplay !is GameplayUiState.InProgress || gameplay.phase != GamePhase.RECONSTRUCT) return
        if (!_rewatchState.value.canWatchRewardedAd) return
        rewatchAdFlow.watch(activity, onBeforeStart = ::pauseTimerForAd, onAfterResolve = ::resumeTimerAfterAd) {
            val current = _uiState.value
            val rewatch = _rewatchState.value
            if (current is GameplayUiState.InProgress && current.phase == GamePhase.RECONSTRUCT && rewatch.canWatchRewardedAd) {
                val rewardedUsed = rewatch.rewardedRewatchesUsed + 1
                _rewatchState.value = rewatch.copy(rewardedRewatchesUsed = rewardedUsed)
                analytics.logRewardedRewatchUsed(mode, levelNumber, rewardedUsed)
                logExcessiveRewatchIfNeeded(rewatch.rewatchesUsed + rewardedUsed)
                recordRewardedRewatchUsed(levelNumber)
                recordMissionEvent(MissionEvent.RewardedAdWatched)
                if (rewardedUsed >= rewardedLimits.maxRewardedRewatches) {
                    rewardLimitReachedThisAttempt = true
                    analytics.logRewardLimitReached("rewatch", mode, levelNumber)
                    _rewatchRewardLimitReached.tryEmit(Unit)
                }
                replayScene(current)
            }
        }
    }

    /** Freezes whichever phase timer is currently running (Memorize or Reconstruct) the instant
     * any rewarded ad is requested - [CountdownTimer.pause] cancels its own ticking coroutine
     * without touching [CountdownTimer.remainingMs], so no gameplay time is lost while a
     * full-screen ad (loading or showing) covers the game. Also re-anchors [phaseDeadlineEpochMs]
     * to right now, so even a process death while the ad is up restores close to the correct
     * remaining time via [persistSnapshot] rather than [restoreFromSnapshot] recomputing against
     * a deadline that assumed the timer kept running through the whole ad. Also pauses whatever
     * music is currently playing (the gameplay bed, and the countdown overlay if the round was
     * already in its final 10 seconds) in place via [FeedbackManager.pauseMusic] - loading,
     * showing, and the reward animation all happen with the mix frozen exactly where it was. */
    private fun pauseTimerForAd() {
        adPauseActive = true
        timer.pause()
        val remaining = timer.remainingMs.value
        if (remaining > 0) phaseDeadlineEpochMs = clock.millis() + remaining
        feedback.pauseMusic()
        persistSnapshot()
    }

    /** Resumes exactly where [pauseTimerForAd] left off - [CountdownTimer.resume] restarts
     * ticking from whatever [CountdownTimer.remainingMs] already holds, completely unaffected by
     * how long the ad actually took to load/show/resolve, so a countdown paused at 17 seconds is
     * always resumed at 17 seconds. Runs after every ad resolution (rewarded, cancelled, or
     * failed) via [RewardedAdFlow]'s `finally` block - regardless of outcome, gameplay always
     * continues from here. Safe to call even when [replayScene] (Rewatch's successful-reward
     * path) has already started a brand new timer of its own by this point:
     * [CountdownTimer.resume] is a no-op once a job is already running. Resumes music
     * unconditionally, even along the early-return paths below - [MusicManager.resume] only ever
     * restarts what [MusicManager.pause] actually found playing, so this is always safe, and the
     * mix should never stay frozen just because the timer itself has nothing left to resume. */
    private fun resumeTimerAfterAd() {
        adPauseActive = false
        feedback.resumeMusic()
        val gameplay = _uiState.value
        if (gameplay !is GameplayUiState.InProgress) return
        if (gameplay.phase != GamePhase.MEMORIZE && gameplay.phase != GamePhase.RECONSTRUCT) return
        val remaining = timer.remainingMs.value
        if (remaining <= 0) return
        phaseDeadlineEpochMs = clock.millis() + remaining
        timer.resume()
        persistSnapshot()
    }

    private fun logExcessiveRewatchIfNeeded(rewatchesUsed: Int) {
        if (rewatchesUsed == EXCESSIVE_ASSIST_THRESHOLD) {
            analytics.logFrustrationSignal("excessive_rewatch", mode, levelNumber, detail = rewatchesUsed.toString())
        }
    }

    /** Re-enters Memorize with the SAME [LevelSpec] captured from the live Reconstruct state -
     * never regenerated - while holding onto the in-progress round (placements/tray/order/
     * remaining Reconstruct time) in [pausedReconstruct] so [resumeReconstructAfterRewatch] can
     * restore it once the replay finishes. [CountdownTimer.start] cancels whatever Reconstruct
     * countdown was running before starting the replay's own, so no explicit pause call is
     * needed here. */
    private fun replayScene(reconstruct: GameplayUiState.InProgress) {
        pausedReconstruct = PausedReconstruct(
            placements = reconstruct.placements,
            trayObjectIds = reconstruct.trayObjectIds,
            placementOrder = reconstruct.placementOrder,
            remainingMs = reconstruct.remainingMs,
        )
        val level = reconstruct.level
        phaseDeadlineEpochMs = clock.millis() + level.memorizeDurationMs
        _uiState.value = GameplayUiState.InProgress(
            phase = GamePhase.MEMORIZE,
            level = level,
            remainingMs = level.memorizeDurationMs,
            placements = reconstruct.placements,
            trayObjectIds = reconstruct.trayObjectIds,
            placementOrder = reconstruct.placementOrder,
        )
        timer.start(level.memorizeDurationMs) { onRewatchMemorizeFinished(level) }
        feedback.onTimerPhaseStarted()
        persistSnapshot()
    }

    private fun onRewatchMemorizeFinished(level: LevelSpec) {
        val paused = pausedReconstruct
        phaseDeadlineEpochMs = null
        _uiState.value = GameplayUiState.InProgress(
            phase = GamePhase.HIDDEN,
            level = level,
            remainingMs = null,
            placements = paused?.placements.orEmpty(),
            trayObjectIds = paused?.trayObjectIds.orEmpty(),
            placementOrder = paused?.placementOrder.orEmpty(),
        )
        persistSnapshot()
        viewModelScope.launch {
            delay(HIDDEN_PHASE_DURATION_MS)
            resumeReconstructAfterRewatch(level)
        }
    }

    /** The step that actually satisfies "return to reconstruction mode": restores phase, board
     * state, and the Reconstruct clock exactly where the rewatch interrupted it - a resumed
     * countdown, never a fresh full one. [PausedReconstruct.remainingMs] is already null for
     * Practice (which never has a Reconstruct timer to begin with), so nothing gets started for
     * it, matching how the original round behaved. */
    private fun resumeReconstructAfterRewatch(level: LevelSpec) {
        val paused = pausedReconstruct
        pausedReconstruct = null
        phaseDeadlineEpochMs = paused?.remainingMs?.let { clock.millis() + it }
        _uiState.value = GameplayUiState.InProgress(
            phase = GamePhase.RECONSTRUCT,
            level = level,
            remainingMs = paused?.remainingMs,
            placements = paused?.placements.orEmpty(),
            trayObjectIds = paused?.trayObjectIds.orEmpty(),
            placementOrder = paused?.placementOrder.orEmpty(),
        )
        paused?.remainingMs?.let { remaining ->
            timer.start(remaining) { submitReconstruction(isAutoSubmit = true) }
            feedback.onTimerPhaseStarted()
        }
        persistSnapshot()
    }

    /** [isAutoSubmit] is true only when the Reconstruct timer itself expires — that always
     * forces evaluation of whatever was placed (the "beat the clock" mechanic), so it's the one
     * caller allowed to bypass the completeness guard below. Every explicit player tap goes
     * through the default `false` path and is rejected outright if the tray isn't empty yet. */
    fun submitReconstruction(isAutoSubmit: Boolean = false) {
        val current = _uiState.value
        if (current !is GameplayUiState.InProgress || current.phase != GamePhase.RECONSTRUCT) return
        if (!isAutoSubmit && current.trayObjectIds.isNotEmpty()) {
            _submitRejected.tryEmit(current.trayObjectIds.size)
            feedback.onWarning()
            return
        }
        val level = current.level
        val objectsPlaced = current.placements.size
        if (isAutoSubmit) {
            analytics.logTimeExpired(mode, levelNumber, objectsPlaced)
        } else {
            analytics.logSubmitPressed(mode, levelNumber, objectsPlaced)
        }
        val result = scoringEngine.score(level, current.placements.values.toList(), current.placementOrder, current.remainingMs)
        val stars = StarRatingCalculator.calculate(result, level.objects.size)
        val timeTakenMs = reconstructStartedAt?.let { Duration.between(it, clock.instant()).toMillis() } ?: 0L
        val passedThisLevel = campaignEngine.passed(result.sceneAccuracy)
        val mistakesCount = result.objectScores.count { it.positionAccuracy < 1f }
        timer.cancel()
        feedback.stopTimerAudio()
        phaseDeadlineEpochMs = null
        savedStateHandle.remove<String>(SNAPSHOT_KEY)

        level.timeLimitMs?.let { limitMs ->
            if (timeTakenMs > (limitMs * LONG_DURATION_RATIO_OF_LIMIT)) {
                analytics.logFrustrationSignal("long_duration", mode, levelNumber, detail = timeTakenMs.toString())
            }
        }
        if (mode == GameMode.CLASSIC) {
            val consecutiveFails = frustrationTracker.recordAttempt(levelNumber, passedThisLevel)
            if (consecutiveFails == REPEATED_FAILURES_THRESHOLD) {
                analytics.logFrustrationSignal("repeated_failures", mode, levelNumber, detail = consecutiveFails.toString())
            }
        }

        fun logCompleted(xpAwarded: Long?, coinsAwarded: Long?) = analytics.logLevelCompleted(
            mode = mode,
            levelNumber = levelNumber,
            level = level,
            difficultyTier = analyticsDifficultyTier,
            stars = stars,
            accuracy = result.sceneAccuracy,
            passed = if (mode == GameMode.CLASSIC) passedThisLevel else null,
            timeTakenMs = timeTakenMs,
            mistakesCount = mistakesCount,
            // Total assist uses this attempt (free + rewarded-ad-granted) - each grant nets to
            // zero-sum with the extra use it enables (see grantBonusHint/grantBonusRedo's "refund"
            // model), so summing the two counters back out is what recovers the true total.
            hintsUsed = _hintState.value.hintsUsed + _hintState.value.rewardedHintsUsed,
            redosUsed = _redoState.value.redosUsed + _redoState.value.rewardedRedosUsed,
            rewatchesUsed = _rewatchState.value.rewatchesUsed + _rewatchState.value.rewardedRewatchesUsed,
            xpAwarded = xpAwarded,
            coinsAwarded = coinsAwarded,
        )

        if (mode == GameMode.PRACTICE) {
            // Practice never awards XP/coins, and there's no async submission step to wait on -
            // every value this event will ever have is already known right here.
            logCompleted(xpAwarded = null, coinsAwarded = null)
            if (passedThisLevel) {
                viewModelScope.launch { recordCompletionMissionEvents(MissionEvent.PracticeRoundCompleted, stars, result, coinsAwarded = null) }
            }
            _uiState.value = GameplayUiState.Finished(result, stars, passed = passedThisLevel)
            return
        }

        _uiState.value = GameplayUiState.Finished(result, stars, passed = passedThisLevel, isSubmitting = true)
        // Minted once per finished round, not per network attempt - reused across any retry of
        // *this* result so a duplicated/replayed submission (a retried request after a timeout
        // whose first attempt actually landed) can never grant XP/coins twice. See
        // FirestoreProgressionRemoteSource.submitScore.
        val submissionNonce = UUID.randomUUID().toString()
        viewModelScope.launch {
            val playedOnEpochDay = LocalDate.now(clock).toEpochDay()
            // Campaign progress (best-stars-by-level, including this attempt) is recorded first so
            // achievement evaluation inside submitScore - e.g. "beat level 100," "first 3-star
            // clear" - sees this attempt's result rather than stale pre-submission data.
            val levelOutcome = if (mode == GameMode.CLASSIC) {
                recordLevelCompletion(levelNumber, timeTakenMs, passedThisLevel, stars)
            } else {
                null
            }
            // XP/coins/statistics/achievements/rewards are only ever earned by actually clearing
            // the round - passedThisLevel uses the same 0.7 accuracy threshold as challenge "win"
            // (see ProgressionRules.challengeWinAccuracyThreshold's doc), so this one check is
            // correct for Classic and both Challenge modes alike. A failed attempt intentionally
            // skips submission entirely rather than awarding partial credit for it.
            // Classic only - a level's genuine first clear (or any Daily/Weekly Challenge win,
            // which is already naturally rate-limited to once per day/week) awards XP normally; a
            // repeat clear of an already-completed Classic level doesn't - XP rewards campaign
            // progress, not farming the same level over and over. Coins/stars/leaderboard rank/
            // streak/achievements are all entirely unaffected either way.
            val awardXp = mode != GameMode.CLASSIC || levelOutcome?.isFirstCompletion == true
            val submission = if (passedThisLevel) {
                when (val outcome = submitScore(mode, level.seed, result, playedOnEpochDay, timeTakenMs, submissionNonce, awardXp)) {
                    is Outcome.Success -> outcome.data
                    is Outcome.Error -> null
                }
            } else {
                resetWinStreak()
                null
            }
            submission?.newlyUnlockedAchievements?.forEach { analytics.logAchievementUnlocked(it.name) }
            if (submission != null) {
                // Fire-and-forget, in its own coroutine rather than awaited inline here - a slow
                // or unreachable Firestore write must never delay the results screen from showing.
                // See pushToLeaderboards' doc for what gets validated/sent.
                viewModelScope.launch { pushToLeaderboards(level, result, submission.statistics, submission.profile.xp, timeTakenMs) }
            }
            // Logged here, after the submission resolves (success or not), rather than
            // synchronously above - this is the earliest point xpAwarded/coinsAwarded are actually
            // known, and moving only the analytics call has no effect on the UI state transitions
            // above, which already run at the same point they always have.
            logCompleted(xpAwarded = submission?.xpAwarded, coinsAwarded = submission?.coinsAwarded)
            if (submission != null) {
                val modeEvent = when (mode) {
                    GameMode.CLASSIC -> MissionEvent.LevelCompleted
                    GameMode.DAILY_CHALLENGE -> MissionEvent.DailyChallengeWon
                    GameMode.WEEKLY_CHALLENGE -> MissionEvent.WeeklyChallengeWon
                    GameMode.PRACTICE -> null // unreachable - Practice returns early above
                }
                modeEvent?.let { recordCompletionMissionEvents(it, stars, result, submission.coinsAwarded) }
            }
            if (submission != null) {
                val shields = submission.profile.streakShields
                submission.streakMilestoneReached?.let { analytics.logStreakMilestoneReached(it, shields) }
                if (submission.streakShieldGranted) analytics.logStreakShieldEarned(requireNotNull(submission.streakMilestoneReached), shields)
                if (submission.streakShieldConsumed) analytics.logStreakShieldConsumed(shields)
                if (submission.journeyPointsAwarded > 0) analytics.logMemoryJourneyPointsEarned(source = "level_completed", points = submission.journeyPointsAwarded)
                submission.journeyTierReached?.let { analytics.logMemoryJourneyTierReached(it.name) }
            }
            _uiState.value = GameplayUiState.Finished(
                result = result,
                stars = stars,
                passed = passedThisLevel,
                isSubmitting = false,
                submission = submission,
                levelOutcome = levelOutcome,
            )
        }
    }

    /** Reuses this round's already-computed completion signals to advance any currently-active
     * mission whose requirement matches, rather than threading new instrumentation through
     * gameplay code (see the retention plan's Phase 1 doc) - only ever called once a round is
     * confirmed passed, mirroring [submitReconstruction]'s own "no partial credit for a failed
     * attempt" rule for XP/coins/statistics/achievements. [coinsAwarded] is `null` for Practice,
     * which never earns coins. */
    private suspend fun recordCompletionMissionEvents(completionEvent: MissionEvent, stars: Int, result: ScoreResult, coinsAwarded: Long?) {
        recordMissionEvent(completionEvent)
        recordMissionEvent(MissionEvent.StarsEarned(stars))
        if (coinsAwarded != null && coinsAwarded > 0) recordMissionEvent(MissionEvent.CoinsEarned(coinsAwarded))
        if (_hintState.value.hintsUsed + _hintState.value.rewardedHintsUsed == 0) recordMissionEvent(MissionEvent.ZeroHintLevelClear)
        if (result.sceneAccuracy >= HIGH_ACCURACY_MISSION_THRESHOLD) recordMissionEvent(MissionEvent.HighAccuracyClear)
    }

    /** Best-effort push to the shared Firestore leaderboards after a scored round resolves -
     * never awaited by [submitReconstruction]'s own flow (see its call site) so a slow/offline/
     * not-yet-configured Firestore (see [com.suman.memoryarchitect.domain.repository.LeaderboardRepository])
     * can never delay or affect the results screen. [LeaderboardAntiCheat.isPlausible] runs first
     * - a corrupted/tampered local result never even attempts to reach Firestore, the client-side
     * half of this app's leaderboard integrity strategy (see `firestore.rules`/`functions/` for
     * the server-side half). Global stats are pushed for every scored mode; the Daily/Weekly
     * periodic boards only for their own matching [GameMode]. */
    private suspend fun pushToLeaderboards(level: LevelSpec, result: ScoreResult, statistics: PlayerStatistics, xp: Long, timeTakenMs: Long) {
        if (!LeaderboardAntiCheat.isPlausible(result, level, timeTakenMs)) return
        val campaignProgress = getCampaignProgress()
        submitGlobalLeaderboardStats(
            GlobalLeaderboardStats(
                highestLevel = campaignProgress.maxUnlockedLevel,
                totalStars = campaignProgress.bestStars.values.sum(),
                bestScore = statistics.bestScore.toLong(),
                perfectLevels = statistics.perfectGames,
                averageAccuracy = statistics.averageAccuracy,
                averageCompletionTimeMs = statistics.averageCompletionTimeMs,
                totalObjectsMemorized = statistics.objectsMemorized,
                totalLevelsCompleted = statistics.gamesPlayed,
                totalPlayTimeMs = statistics.totalTimeTakenMs,
                xp = xp,
                winStreak = statistics.currentWinStreak,
                longestWinStreak = statistics.longestWinStreak,
                achievementCount = getUnlockedAchievements().size,
            ),
        )
        val periodicSubmission = PeriodicLeaderboardSubmission(
            score = result.finalScore.toLong(),
            accuracy = result.sceneAccuracy,
            completionTimeMs = timeTakenMs,
            isPerfectRun = result.sceneAccuracy >= 1f,
        )
        when (mode) {
            GameMode.DAILY_CHALLENGE -> submitDailyLeaderboardScore(periodicSubmission)
            GameMode.WEEKLY_CHALLENGE -> submitWeeklyLeaderboardScore(periodicSubmission)
            else -> Unit
        }
        // Every scored mode (not just the Challenge modes) contributes to the Monthly board,
        // mirroring how the Global board already aggregates across all modes - Monthly sits
        // between "one round's Daily/Weekly result" and "lifetime Global total" in scope.
        submitMonthlyLeaderboardScore(periodicSubmission)
    }

    private inline fun updateReconstructState(transform: (GameplayUiState.InProgress) -> GameplayUiState.InProgress) {
        val current = _uiState.value
        if (current is GameplayUiState.InProgress && current.phase == GamePhase.RECONSTRUCT) {
            _uiState.value = transform(current)
            persistSnapshot()
        }
    }

    /** Snapshots the live round (or clears any previously-saved one once the round ends) into
     * [SavedStateHandle] so it survives process death — the one persistence gap standard
     * ViewModel scoping doesn't already close for free. Called after every structural change to
     * the board or phase; deliberately NOT called on every 100ms timer tick ([phaseDeadlineEpochMs]
     * is a fixed deadline set once per phase, so the countdown ticking alone never needs a rewrite). */
    private fun persistSnapshot() {
        val current = _uiState.value
        if (current !is GameplayUiState.InProgress) {
            savedStateHandle.remove<String>(SNAPSHOT_KEY)
            return
        }
        val paused = pausedReconstruct
        val snapshot = GameplaySessionSnapshot(
            phase = current.phase,
            level = current.level,
            placements = current.placements,
            trayObjectIds = current.trayObjectIds,
            placementOrder = current.placementOrder,
            phaseDeadlineEpochMs = phaseDeadlineEpochMs,
            reconstructStartedAtEpochMs = reconstructStartedAt?.toEpochMilli(),
            pausedPlacements = paused?.placements,
            pausedTrayObjectIds = paused?.trayObjectIds,
            pausedPlacementOrder = paused?.placementOrder,
            pausedRemainingMs = paused?.remainingMs,
        )
        savedStateHandle[SNAPSHOT_KEY] = snapshotAdapter.toJson(snapshot)
    }

    /** Rebuilds the live round from a snapshot restored after process death - never regenerates
     * the [LevelSpec] (same "never a new scene" guarantee [watchRewatchAd] already gives a live
     * session), and recomputes remaining time from [GameplaySessionSnapshot.phaseDeadlineEpochMs]
     * against the current wall clock rather than trusting whatever was left when the process
     * died. A phase found to have already fully elapsed while the app was away is resolved
     * immediately (Memorize -> cascades on to Hidden/Reconstruct, Reconstruct -> auto-submits),
     * exactly as if its timer had simply finished on schedule. */
    private fun restoreFromSnapshot(snapshot: GameplaySessionSnapshot) {
        val level = snapshot.level
        reconstructStartedAt = snapshot.reconstructStartedAtEpochMs?.let(Instant::ofEpochMilli)
        pausedReconstruct = snapshot.pausedPlacements?.let { placements ->
            PausedReconstruct(
                placements = placements,
                trayObjectIds = snapshot.pausedTrayObjectIds.orEmpty(),
                placementOrder = snapshot.pausedPlacementOrder.orEmpty(),
                remainingMs = snapshot.pausedRemainingMs,
            )
        }
        val isRewatchPaused = pausedReconstruct != null
        // A process-death restore starts with no ExoPlayer instance at all (that state is
        // in-memory only, unlike this snapshot) - re-set the gameplay bed so the round doesn't
        // come back silent. Position can't be restored across a real process death the way a
        // same-process pause/resume can (see onPaused/onResumed below); starting the bed fresh is
        // the same tradeoff every audio app makes here, not a gap specific to this one.
        feedback.setMusicTrack(mode.gameplayMusicTrack())

        viewModelScope.launch {
            _hintState.value = HintUiState(
                maxHints = hintEngine.maxHintsForLevel(levelNumber),
                hintsUsed = getHintsUsed(levelNumber),
                maxRewardedHints = rewardedLimits.maxRewardedHints,
                rewardedHintsUsed = getRewardedHintsUsed(levelNumber),
            )
            _redoState.value = RedoUiState(
                maxRedos = redoRules.redosAllowedForLevel(levelNumber),
                redosUsed = getRedosUsed(levelNumber),
                maxRewardedRedos = rewardedLimits.maxRewardedRedos,
                rewardedRedosUsed = getRewardedRedosUsed(levelNumber),
            )
            _rewatchState.value = RewatchUiState(
                maxRewatches = rewatchRules.rewatchesAllowedForLevel(levelNumber),
                rewatchesUsed = getRewatchesUsed(levelNumber),
                maxRewardedRewatches = rewardedLimits.maxRewardedRewatches,
                rewardedRewatchesUsed = getRewardedRewatchesUsed(levelNumber),
            )
            refreshInventoryTokenFlags()
            when (snapshot.phase) {
                GamePhase.MEMORIZE -> {
                    val deadline = snapshot.phaseDeadlineEpochMs
                    val remaining = deadline?.let { (it - clock.millis()).coerceAtLeast(0) } ?: 0L
                    val onFinished: () -> Unit =
                        if (isRewatchPaused) ({ onRewatchMemorizeFinished(level) }) else ({ onMemorizeFinished(level) })
                    if (remaining <= 0) {
                        onFinished()
                    } else {
                        phaseDeadlineEpochMs = deadline
                        _uiState.value = GameplayUiState.InProgress(
                            phase = GamePhase.MEMORIZE,
                            level = level,
                            remainingMs = remaining,
                            placements = snapshot.placements,
                            trayObjectIds = snapshot.trayObjectIds,
                            placementOrder = snapshot.placementOrder,
                        )
                        timer.start(remaining, onFinished)
                        feedback.onTimerPhaseStarted()
                    }
                }
                GamePhase.HIDDEN -> {
                    if (isRewatchPaused) resumeReconstructAfterRewatch(level) else startReconstructPhase(level)
                }
                GamePhase.RECONSTRUCT -> {
                    val deadline = snapshot.phaseDeadlineEpochMs
                    val remaining = deadline?.let { (it - clock.millis()).coerceAtLeast(0) }
                    phaseDeadlineEpochMs = deadline
                    _uiState.value = GameplayUiState.InProgress(
                        phase = GamePhase.RECONSTRUCT,
                        level = level,
                        remainingMs = remaining,
                        placements = snapshot.placements,
                        trayObjectIds = snapshot.trayObjectIds,
                        placementOrder = snapshot.placementOrder,
                    )
                    if (deadline != null && remaining != null && remaining <= 0) {
                        submitReconstruction(isAutoSubmit = true)
                    } else if (remaining != null) {
                        timer.start(remaining) { submitReconstruction(isAutoSubmit = true) }
                        feedback.onTimerPhaseStarted()
                    }
                }
                GamePhase.FINISHED -> loadLevel()
            }
        }
    }

    /** Corrects the live countdown against real elapsed wall-clock time whenever the app returns
     * to the foreground - a plain [CountdownTimer] alone only knows how many of its own 100ms
     * ticks actually ran, which can drift behind real elapsed time if the process was frozen or
     * throttled in the background for a while. A phase found to have already fully elapsed is
     * resolved immediately, exactly as if the timer had finished on schedule. No-ops whenever
     * there's no active phase timer to correct (Hidden, Practice's untimed Rebuild, Finished).
     *
     * Also no-ops for as long as [adPauseActive] is true: closing a rewarded ad (Hint, Redo, or
     * Rewatch) fires this exact same `ON_RESUME` lifecycle event, and without this guard the
     * wall-clock correction below would treat however long the ad was on screen as time lost to
     * backgrounding and either fast-forward the countdown or auto-submit the round the instant
     * the player returns - [resumeTimerAfterAd] is the one that correctly restores the timer for
     * that case, from the exact value it was paused at. This guard alone isn't airtight, though:
     * there's no ordering guarantee between the ad SDK's own callback chain (which is what
     * eventually flips [adPauseActive] back to `false`, via [resumeTimerAfterAd]) and Android
     * dispatching the real `ON_RESUME` lifecycle event for the ad closing - if this fires *after*
     * that flip, the guard above has nothing left to catch. That's why the MEMORIZE branch below
     * mirrors [restoreFromSnapshot]'s `isRewatchPaused` check rather than assuming every MEMORIZE
     * phase is the original one: a Rewatch replay's Memorize is also phase MEMORIZE, and routing
     * its completion through the plain [onMemorizeFinished] (which knows nothing about
     * [pausedReconstruct]) would wipe the player's placements and hand back a full-duration timer
     * instead of resuming the interrupted Reconstruct round - the exact bug this guards against.
     * This whole branch runs immediately, exactly as it always has - it only ever fires moments
     * after a process-death restore (see [restoreFromSnapshot], which does its own reconciliation
     * and already starts the timer running before this ever gets a chance to run), refining
     * against the tiny extra gap since then. Nothing was ever visibly "away" for this case (the
     * process was dead, not merely backgrounded), so it deliberately does **not** go through the
     * Anti-Cheat Privacy Shield below - only [backgroundPauseActive] does.
     *
     * [backgroundPauseActive] is the one case that *is* gated behind the shield: a real
     * backgrounding (see [onPaused]) is a true freeze, not lost time, so instead of resuming
     * anything directly, that branch just raises [PrivacyShieldPhase.READY_TO_CONTINUE] -
     * [onPrivacyShieldContinue] is the one and only place gameplay/timer/music actually resume
     * from that point on, once the player explicitly confirms they're back. */
    fun onResumed() {
        if (adPauseActive) return
        if (backgroundPauseActive) {
            val current = _uiState.value
            if (current !is GameplayUiState.InProgress) return
            if (current.phase != GamePhase.MEMORIZE && current.phase != GamePhase.RECONSTRUCT) return
            _uiState.value = current.copy(privacyShield = PrivacyShieldPhase.READY_TO_CONTINUE)
            return
        }
        val current = _uiState.value
        val deadline = phaseDeadlineEpochMs
        if (current !is GameplayUiState.InProgress || deadline == null) return
        if (current.phase != GamePhase.MEMORIZE && current.phase != GamePhase.RECONSTRUCT) return
        val level = current.level
        val onFinished: () -> Unit = if (current.phase == GamePhase.MEMORIZE) {
            if (pausedReconstruct != null) ({ onRewatchMemorizeFinished(level) }) else ({ onMemorizeFinished(level) })
        } else {
            { submitReconstruction(isAutoSubmit = true) }
        }
        val actualRemaining = deadline - clock.millis()
        if (actualRemaining <= 0) {
            onFinished()
            return
        }
        timer.start(actualRemaining, onFinished)
        _uiState.value = current.copy(remainingMs = actualRemaining)
    }

    /** The background-pause counterpart to [onResumed] - called on `ON_PAUSE` (app minimized,
     * phone locked, Recent Apps/App Switcher opened, an incoming call, audio focus lost) rather
     * than a rewarded ad, so it freezes the timer itself here instead of relying on a
     * `RewardedAdFlow`'s `onBeforeStart`. A true freeze (see [backgroundPauseActive]'s doc) - no
     * time is lost while backgrounded, the same guarantee a rewarded ad already gets.
     *
     * Also raises the Anti-Cheat Privacy Shield ([PrivacyShieldPhase.HIDDEN_AWAY]) and suppresses
     * the generic whole-app music resume (see [FeedbackManager.setMusicResumeSuppressed]) so
     * returning to the app can never silently un-freeze anything before [onPrivacyShieldContinue].
     * Together with [android.view.WindowManager.LayoutParams.FLAG_SECURE] (set for the whole time
     * Gameplay is on screen - see [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScreen])
     * this is what makes both halves of the exploit impossible: the OS itself never captures real
     * gameplay pixels for Recent Apps/a screenshot/screen recording while backgrounded, and even
     * once the Activity is visible again, nothing resumes until the player deliberately taps
     * Continue.
     *
     * No-ops if an ad is already handling its own pause, or if there's no active phase timer to
     * freeze in the first place (Hidden, Practice's untimed Rebuild, Finished, or already
     * backgrounded). */
    fun onPaused() {
        if (adPauseActive || backgroundPauseActive) return
        val current = _uiState.value
        if (current !is GameplayUiState.InProgress) return
        if (current.phase != GamePhase.MEMORIZE && current.phase != GamePhase.RECONSTRUCT) return
        backgroundPauseActive = true
        timer.pause()
        val remaining = timer.remainingMs.value
        if (remaining > 0) phaseDeadlineEpochMs = clock.millis() + remaining
        feedback.pauseMusic()
        feedback.setMusicResumeSuppressed(true)
        persistSnapshot()
        _uiState.value = current.copy(privacyShield = PrivacyShieldPhase.HIDDEN_AWAY)
    }

    /** The one place gameplay actually resumes after a real backgrounding - called only when the
     * player taps Continue on the Privacy Shield's [PrivacyShieldPhase.READY_TO_CONTINUE] gate,
     * never automatically. Does exactly what [onResumed]'s `backgroundPauseActive` branch did
     * before the shield existed: resumes the timer from its exact frozen value, with no time lost. */
    fun onPrivacyShieldContinue() {
        val current = _uiState.value
        if (current !is GameplayUiState.InProgress || current.privacyShield != PrivacyShieldPhase.READY_TO_CONTINUE) return
        _uiState.value = current.copy(privacyShield = PrivacyShieldPhase.NONE)
        feedback.setMusicResumeSuppressed(false)
        if (!backgroundPauseActive) return
        backgroundPauseActive = false
        feedback.resumeMusic()
        val gameplay = _uiState.value
        if (gameplay !is GameplayUiState.InProgress) return
        val remaining = timer.remainingMs.value
        if (remaining <= 0) return
        phaseDeadlineEpochMs = clock.millis() + remaining
        timer.resume()
        persistSnapshot()
    }

    private fun loadLevel() {
        _uiState.value = GameplayUiState.Loading
        viewModelScope.launch {
            val outcome = performanceTracer.trace("level_load") {
                if (mode == GameMode.CLASSIC) {
                    generateLevel(mode, campaignEngine.tierFor(levelNumber), campaignEngine.streakFor(levelNumber))
                } else {
                    generateLevel(mode, difficultyTier)
                }
            }
            when (outcome) {
                is Outcome.Success -> {
                    val level = outcome.data
                    hintExpiryJob?.cancel()
                    // A fresh attempt at this level (first time ever, a retry, or a replay) always
                    // gets a full assist budget - only a live in-progress session resumed via
                    // restoreFromSnapshot carries usage forward, never a brand new loadLevel call.
                    resetHintUsage(levelNumber)
                    resetRedoUsage(levelNumber)
                    resetRewatchUsage(levelNumber)
                    // A fresh attempt also gets a full rewarded-ad budget for each helper (see
                    // RewardedAssistLimits) - resetHintUsage/resetRedoUsage/resetRewatchUsage above
                    // each clear their whole persisted row, including the rewarded counter column,
                    // so this flag is the only piece of in-memory state that needs its own reset.
                    rewardLimitReachedThisAttempt = false
                    _hintState.value = HintUiState(
                        maxHints = hintEngine.maxHintsForLevel(levelNumber),
                        hintsUsed = 0,
                        maxRewardedHints = rewardedLimits.maxRewardedHints,
                        rewardedHintsUsed = 0,
                    )
                    _redoState.value = RedoUiState(
                        maxRedos = redoRules.redosAllowedForLevel(levelNumber),
                        redosUsed = 0,
                        maxRewardedRedos = rewardedLimits.maxRewardedRedos,
                        rewardedRedosUsed = 0,
                    )
                    _rewatchState.value = RewatchUiState(
                        maxRewatches = rewatchRules.rewatchesAllowedForLevel(levelNumber),
                        rewatchesUsed = 0,
                        maxRewardedRewatches = rewardedLimits.maxRewardedRewatches,
                        rewardedRewatchesUsed = 0,
                    )
                    refreshInventoryTokenFlags()
                    val mechanic = pendingMechanicIntro()
                    when {
                        shouldShowTutorial() -> _uiState.value = GameplayUiState.TutorialPending(level)
                        mechanic != null -> _uiState.value = GameplayUiState.MechanicIntroPending(level, mechanic)
                        else -> startMemorizePhase(level)
                    }
                }
                is Outcome.Error -> _uiState.value = GameplayUiState.Error(outcome.error)
            }
        }
    }

    /** Only ever Classic's Level 1, and only until it's actually been passed (not merely
     * attempted) — [com.suman.memoryarchitect.domain.repository.LevelCampaignRepository]'s
     * unlock frontier only advances past 1 once the player has cleared it, so this doubles as
     * "first launch" (a fresh install starts at 1) without a separate persisted flag to drift out
     * of sync with real progress. */
    private suspend fun shouldShowTutorial(): Boolean {
        if (mode != GameMode.CLASSIC || levelNumber != 1) return false
        return getCampaignProgress().maxUnlockedLevel <= 1
    }

    /** Same "explain before the clock starts, only once ever" reasoning as [shouldShowTutorial],
     * for the two mechanics that debut mid-campaign instead of at level 1: true only the very
     * first time the frontier reaches level 30 (rotation) or 55 (order mode) — a replay of either
     * level after the player has already moved past it (frontier > that level number) never
     * re-shows it. */
    private suspend fun pendingMechanicIntro(): NewMechanic? {
        if (mode != GameMode.CLASSIC) return null
        if (getCampaignProgress().maxUnlockedLevel > levelNumber) return null
        return when {
            campaignEngine.isRotationDebutLevel(levelNumber) -> NewMechanic.ROTATION
            campaignEngine.isOrderModeDebutLevel(levelNumber) -> NewMechanic.ORDER_MODE
            else -> null
        }
    }

    /** Mirrors [dismissTutorial] exactly - starts the real Memorize timer fresh only once the
     * callout is dismissed, so reading it never eats into the player's actual time budget. */
    fun dismissMechanicIntro() {
        val current = _uiState.value
        if (current is GameplayUiState.MechanicIntroPending) {
            startMemorizePhase(current.level)
        }
    }

    private fun startMemorizePhase(level: LevelSpec) {
        phaseDeadlineEpochMs = clock.millis() + level.memorizeDurationMs
        _uiState.value = GameplayUiState.InProgress(GamePhase.MEMORIZE, level, level.memorizeDurationMs)
        timer.start(level.memorizeDurationMs) { onMemorizeFinished(level) }
        feedback.onTimerPhaseStarted()
        feedback.setMusicTrack(mode.gameplayMusicTrack())
        analytics.logLevelStarted(mode, levelNumber, level, analyticsDifficultyTier)
        analytics.logMemorizeStarted(mode, levelNumber, level, analyticsDifficultyTier)
        val startCount = frustrationTracker.recordLevelStart(levelNumber)
        if (startCount > 1) {
            analytics.logLevelRestarted(mode, levelNumber, attemptNumber = startCount)
            if (startCount >= REPEATED_RETRY_THRESHOLD) {
                analytics.logFrustrationSignal("repeated_retry", mode, levelNumber, detail = startCount.toString())
            }
        }
        persistSnapshot()
    }

    private fun onMemorizeFinished(level: LevelSpec) {
        phaseDeadlineEpochMs = null
        analytics.logMemorizeFinished(mode, levelNumber)
        _uiState.value = GameplayUiState.InProgress(GamePhase.HIDDEN, level, null)
        persistSnapshot()
        viewModelScope.launch {
            delay(HIDDEN_PHASE_DURATION_MS)
            startReconstructPhase(level)
        }
    }

    private fun startReconstructPhase(level: LevelSpec) {
        reconstructStartedAt = clock.instant()
        phaseDeadlineEpochMs = level.timeLimitMs?.let { clock.millis() + it }
        _uiState.value = GameplayUiState.InProgress(
            phase = GamePhase.RECONSTRUCT,
            level = level,
            remainingMs = level.timeLimitMs,
            trayObjectIds = level.objects.map { it.objectId }.shuffled(),
        )
        analytics.logRebuildStarted(mode, levelNumber)
        level.timeLimitMs?.let { limit ->
            timer.start(limit) { submitReconstruction(isAutoSubmit = true) }
            feedback.onTimerPhaseStarted()
        }
        feedback.setMusicTrack(mode.gameplayMusicTrack())
        persistSnapshot()
    }

    override fun onCleared() {
        super.onCleared()
        timer.cancel()
        feedback.stopTimerAudio()
        hintExpiryJob?.cancel()
        val sessionDurationMs = Duration.between(sessionStartedAt, clock.instant()).toMillis()
        val current = _uiState.value
        if (current is GameplayUiState.InProgress && current.phase != GamePhase.FINISHED) {
            val phaseName = current.phase.name.lowercase()
            analytics.logLevelQuitMidway(mode, levelNumber, phaseName, sessionDurationMs)
            val signalType = when (current.phase) {
                GamePhase.MEMORIZE -> "quit_during_memorize"
                GamePhase.RECONSTRUCT -> "quit_during_reconstruct"
                else -> null
            }
            signalType?.let { analytics.logFrustrationSignal(it, mode, levelNumber) }
            if (rewardLimitReachedThisAttempt) {
                analytics.logLevelAbandonedAfterRewardLimit(mode, levelNumber)
            }
        }
        analytics.logSessionEnded(mode, sessionDurationMs)
    }

    companion object {
        /** Must match [com.suman.memoryarchitect.ui.navigation.Route.Gameplay]'s field names —
         * that's the flat key format Navigation-Compose populates a typed route's args under. */
        const val ARG_MODE = "mode"
        const val ARG_DIFFICULTY = "difficulty"
        const val ARG_LEVEL_NUMBER = "levelNumber"
        private const val SNAPSHOT_KEY = "gameplaySessionSnapshot"
        private const val HIDDEN_PHASE_DURATION_MS = 500L
        private const val ROTATION_STEP_DEGREES = 90
        // Long enough to actually read both pieces of information a hint now shows - the target
        // slot and the rotated ghost of the object - not just glimpse a flash before it fades.
        private const val HINT_REVEAL_DURATION_MS = 3000L

        // Frustration-signal thresholds - deliberately conservative (fire once per level per
        // signal, at the point real difficulty starts to look like frustration rather than normal
        // play) rather than firing on every single use, which would just be noise on top of the
        // hint_used/redo_used/rewatch_used events that already log every individual use.
        private const val EXCESSIVE_ASSIST_THRESHOLD = 3
        private const val REPEATED_RETRY_THRESHOLD = 3
        private const val REPEATED_FAILURES_THRESHOLD = 3
        private const val LONG_DURATION_RATIO_OF_LIMIT = 0.9

        // Deliberately stricter than ProgressionRules.challengeWinAccuracyThreshold (0.7, "did
        // this round count at all") - MissionId.HIGH_ACCURACY_CLEAR/FIVE_ZERO_HINT_CLEARS reward a
        // notably clean clear, not merely a passing one.
        private const val HIGH_ACCURACY_MISSION_THRESHOLD = 0.95f
    }
}
