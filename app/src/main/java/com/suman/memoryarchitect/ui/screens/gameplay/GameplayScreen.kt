package com.suman.memoryarchitect.ui.screens.gameplay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.domain.generation.LevelMirrorPolicy
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import com.suman.memoryarchitect.feature.gameplay.GameplayUiState
import com.suman.memoryarchitect.feature.gameplay.GameplayViewModel
import com.suman.memoryarchitect.feature.gameplay.HintUiState
import com.suman.memoryarchitect.feature.gameplay.PrivacyShieldPhase
import com.suman.memoryarchitect.feature.gameplay.RedoDenialReason
import com.suman.memoryarchitect.feature.gameplay.RedoUiState
import com.suman.memoryarchitect.feature.gameplay.RewatchUiState
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.shakeOnTrigger
import com.suman.memoryarchitect.ui.illustration.RoomArt
import com.suman.memoryarchitect.ui.illustration.RoomArtRegistry
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the Processing state lingers before the real submission runs — long enough to read
 * as a deliberate step (not an instant flicker), short enough to never feel like a stall. */
private const val SUBMIT_PROCESSING_DELAY_MS = 500L

/** The Privacy Shield's Continue countdown - "3, 2, 1" at [PRIVACY_SHIELD_COUNTDOWN_STEP_MS] per
 * number, brisk enough to never feel like a stall on top of an already-frozen round. */
private const val PRIVACY_SHIELD_COUNTDOWN_FROM = 3
private const val PRIVACY_SHIELD_COUNTDOWN_STEP_MS = 700L

/**
 * Top-level Gameplay screen. Owns the state that doesn't belong in [GameplayViewModel] (it's a
 * pure rendering/gesture concern): which tray object is mid-drag, where, and whether it's
 * currently magnetically snapped to an empty slot — so [GameplayScenePanel] and [GameplayTray]
 * stay independent while a single floating "ghost" renders the premium drag feel on top of both.
 */
@Composable
fun GameplayScreen(
    onBack: () -> Unit,
    onReplayLevel: (mode: GameMode, levelNumber: Int) -> Unit,
    viewModel: GameplayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Purely decorative (Timer Style/Victory Animation/Confetti Effect) - read from the app-wide
    // CompositionLocal (see LocalEquippedCosmetics's doc), deliberately not part of
    // GameplayViewModel's own state.
    val equippedCosmetics = LocalEquippedCosmetics.current
    val hintState by viewModel.hintState.collectAsStateWithLifecycle()
    val rewardedHintAdState by viewModel.rewardedHintAdState.collectAsStateWithLifecycle()
    val redoState by viewModel.redoState.collectAsStateWithLifecycle()
    val rewardedRedoAdState by viewModel.rewardedRedoAdState.collectAsStateWithLifecycle()
    val rewatchState by viewModel.rewatchState.collectAsStateWithLifecycle()
    val rewatchAdState by viewModel.rewatchAdState.collectAsStateWithLifecycle()
    val ambientParticles = rememberParticleFieldState()
    val tick = rememberHapticsTick()
    val feedback = rememberFeedback()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Rewarded ads need a real Activity to present on; the ViewModel never holds one itself
    // (leak risk), so it's resolved here, in the UI layer, and passed through per-call.
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }
    // Every assist-feature collector below needs "replace whatever's currently showing with
    // this new message" - one place for that two-step pattern instead of it repeated at every
    // one of the ~9 call sites below.
    suspend fun replaceSnackbar(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
    }

    // A plain in-memory countdown alone only knows how many of its own ticks actually ran, which
    // can drift behind real elapsed time if the process was frozen/throttled while backgrounded —
    // re-anchoring against the wall clock on every real return to the foreground (not just once,
    // on first composition) is what makes "restore after background" actually correct.
    val currentOnResumed by rememberUpdatedState(viewModel::onResumed)
    val currentOnPaused by rememberUpdatedState(viewModel::onPaused)
    // Deliberately the real Activity's lifecycle, not LocalLifecycleOwner.current - inside a
    // NavHost composable{} destination, that resolves to this entry's own NavBackStackEntry
    // lifecycle, which Navigation Compose drops to STARTED (not RESUMED) for the entire duration
    // of an in-progress predictive back gesture (or a held back-button press on devices that
    // animate one the same way) - even though the Activity itself, and every pixel of this
    // screen, are still fully on screen the whole time the gesture is held. onPaused() below is a
    // TRUE freeze (see its doc): wiring it to that per-entry signal meant holding the gesture
    // froze the timer indefinitely while the scene stayed completely readable, then un-froze for
    // free the instant the gesture was released or cancelled - unlimited thinking time on demand.
    // The Activity's own lifecycle only ever leaves RESUMED for a genuine interruption (Home
    // button, Recents, notification shade, an incoming call, screen lock, another app/dialog
    // taking real focus), which is exactly the set of events this freeze is meant to cover.
    val activityLifecycleOwner = remember(activity) { activity as? LifecycleOwner }
    DisposableEffect(activityLifecycleOwner) {
        val owner = activityLifecycleOwner ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> currentOnResumed()
                // ON_PAUSE, not ON_STOP - the timer/music freeze should engage the instant the
                // screen stops being interactive (app minimized, phone locked, an incoming call),
                // matching how quickly a rewarded ad's own onBeforeStart already freezes things,
                // not only once the Activity is fully stopped.
                Lifecycle.Event.ON_PAUSE -> currentOnPaused()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // Anti-Cheat Privacy Shield, part 1: FLAG_SECURE is what actually, permanently keeps the real
    // Memorize/Rebuild scene out of Recent Apps' task thumbnail, a screenshot, or screen recording/
    // casting - the system's task snapshot is captured by the window compositor itself, outside
    // Compose's control, so covering the content a frame late with an overlay could never reliably
    // race it. Scoped to exactly this screen's composition lifetime (set on entry, cleared on
    // exit) so no other screen is affected; toggling it never recreates the window/Activity and has
    // no visible effect on the player's own live view, only on what a *capture* of the screen sees.
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    // Anti-Cheat Privacy Shield, part 2: the in-app "Gameplay Hidden" / "Ready to Continue?" cover
    // - see PrivacyShieldOverlay.kt and GameplayViewModel.onPaused/onPrivacyShieldContinue. The
    // countdown is purely a local presentation concern (same delay-then-call-the-ViewModel shape
    // already used for Submit's processing beat below), so the ViewModel only ever needs to know
    // "resume now," never anything about the 3-2-1 animation itself.
    val privacyShield = (uiState as? GameplayUiState.InProgress)?.privacyShield ?: PrivacyShieldPhase.NONE
    var shieldCountdown by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(privacyShield) {
        if (privacyShield != PrivacyShieldPhase.READY_TO_CONTINUE) shieldCountdown = null
    }
    val onShieldContinue: () -> Unit = onShieldContinue@{
        if (shieldCountdown != null) return@onShieldContinue
        scope.launch {
            for (n in PRIVACY_SHIELD_COUNTDOWN_FROM downTo 1) {
                shieldCountdown = n
                tick()
                delay(PRIVACY_SHIELD_COUNTDOWN_STEP_MS)
            }
            shieldCountdown = null
            viewModel.onPrivacyShieldContinue()
        }
    }

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var scenePanelCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var isSnappingBack by remember { mutableStateOf(false) }
    // Flips true the instant Submit is tapped (so the button snaps to Processing before the real
    // scoring call even starts) and only ever flips back on an outright rejection — on success the
    // phase moves to Finished and this whole composable subtree is torn down anyway.
    var isSubmitting by remember { mutableStateOf(false) }

    // The completeness guard is enforced in the ViewModel, not just the button's enabled state —
    // this only fires if that's somehow defeated (stale state, accessibility tooling), so treat it
    // as a rare defensive path: reset Processing back to normal and surface exactly why.
    LaunchedEffect(Unit) {
        viewModel.submitRejected.collect { remaining ->
            isSubmitting = false
            val message = context.resources.getQuantityString(
                R.plurals.gameplay_submit_incomplete_snackbar, remaining, remaining,
            )
            replaceSnackbar(message)
        }
    }

    // Same defensive-fallback role as the collector above — the Hint control and the tray's
    // tap-detector both already gate on remaining count, so this should only ever fire from stale
    // state or accessibility tooling bypassing those checks.
    LaunchedEffect(Unit) {
        viewModel.hintDenied.collect {
            val message = context.getString(R.string.gameplay_hint_none_remaining_snackbar)
            replaceSnackbar(message)
        }
    }

    // The grant itself always lands (it persists per-level, so it's still worth having on a
    // future attempt even if this one just ended) - but a watched ad can resolve just after the
    // round auto-submits, and celebrating a bonus hint over the results screen would only
    // confuse, not help, so the confetti/Snackbar only plays while there's still a live round to
    // spend it in.
    LaunchedEffect(Unit) {
        viewModel.rewardedHintGranted.collect {
            if (uiState !is GameplayUiState.InProgress) return@collect
            tick()
            ambientParticles.confettiBurst(Offset(400f, 300f), count = 14)
            val message = context.getString(R.string.gameplay_rewarded_hint_granted_snackbar)
            replaceSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.rewardedHintAdCancelled.collect {
            val message = context.getString(R.string.gameplay_rewarded_hint_cancelled_snackbar)
            replaceSnackbar(message)
        }
    }

    // Same defensive-fallback role as the Hint/Submit collectors above — the Redo control already
    // disables itself at zero remaining or with nothing placed, so this should only ever fire
    // from stale state or accessibility tooling.
    LaunchedEffect(Unit) {
        viewModel.redoDenied.collect { reason ->
            val messageRes = when (reason) {
                RedoDenialReason.NOTHING_TO_UNDO -> R.string.gameplay_redo_nothing_to_undo_snackbar
                RedoDenialReason.OUT_OF_USES -> R.string.gameplay_redo_out_of_uses_snackbar
            }
            val message = context.getString(messageRes)
            replaceSnackbar(message)
        }
    }

    // Same reasoning as the Hint collector above - the grant always lands, only the celebration
    // is gated on the round still being live.
    LaunchedEffect(Unit) {
        viewModel.rewardedRedoGranted.collect {
            if (uiState !is GameplayUiState.InProgress) return@collect
            tick()
            ambientParticles.confettiBurst(Offset(400f, 300f), count = 14)
            val message = context.getString(R.string.gameplay_rewarded_redo_granted_snackbar)
            replaceSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.rewardedRedoAdCancelled.collect {
            val message = context.getString(R.string.gameplay_rewarded_redo_cancelled_snackbar)
            replaceSnackbar(message)
        }
    }

    // Same defensive-fallback role as the Hint/Redo collectors above - the free Rewatch control
    // is only ever rendered while a budget remains, so this should only ever fire from stale
    // state or accessibility tooling.
    LaunchedEffect(Unit) {
        viewModel.rewatchDenied.collect {
            val message = context.getString(R.string.gameplay_rewatch_free_denied_snackbar)
            replaceSnackbar(message)
        }
    }

    // No "granted" collector here, unlike Hint/Redo - a successful rewatch reward (free or ad) is
    // the scene replaying, which is its own unmistakable visual feedback; only "nothing happened"
    // (a cancelled ad) needs a Snackbar to confirm.
    LaunchedEffect(Unit) {
        viewModel.rewatchAdCancelled.collect {
            val message = context.getString(R.string.gameplay_rewatch_cancelled_snackbar)
            replaceSnackbar(message)
        }
    }
    val snapBackOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // Continuously spring-chases whatever the current drag target is (raw finger position, or
    // the snapped slot's exact position once magnetically locked) — this is what turns "welded
    // to the finger" into "floating"/"gliding into place" rather than an instant teleport.
    val floatingOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val floatSpring = spring<Offset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    fun occupiedSlots(): Set<Int> = (uiState as? GameplayUiState.InProgress)?.placements?.values?.map { it.slotIndex }?.toSet().orEmpty()
    // Mirrored the same way, for the same seed, as GameplayScenePanel's own roomArt — these two
    // must never disagree, or drag hit-testing and hint dots would target different geometry
    // than what's actually drawn on screen.
    fun currentRoomArt(): RoomArt? {
        val level = (uiState as? GameplayUiState.InProgress)?.level ?: return null
        return RoomArtRegistry.get(level.sceneType).mirrored(LevelMirrorPolicy.isMirrored(level.seed))
    }
    fun slotRootPosition(slotIndex: Int): Offset? {
        val roomArt = currentRoomArt() ?: return null
        val scenePanel = scenePanelCoordinates ?: return null
        val root = rootCoordinates ?: return null
        val slot = roomArt.slotAt(slotIndex)
        val localPx = Offset(slot.xFraction * scenePanel.size.width, slot.yFraction * scenePanel.size.height)
        return root.localPositionOf(scenePanel, localPx)
    }

    AmbientBackground(
        nearParticles = ambientParticles,
        modifier = Modifier.onGloballyPositioned { rootCoordinates = it },
    ) {
        // Fades between Loading/Error/InProgress/Finished (a real phase-category change) without
        // ever fading on every InProgress mutation (timer ticks, drag state, placements) — keyed
        // on the coarse category, not the state value itself, so gameplay updates recompose in
        // place while entering/leaving Loading, Error, or the results screen dissolves smoothly.
        AnimatedContent(
            targetState = uiState,
            contentKey = { s ->
                when (s) {
                    is GameplayUiState.Loading -> 0
                    is GameplayUiState.Error -> 1
                    is GameplayUiState.TutorialPending -> 2
                    is GameplayUiState.MechanicIntroPending -> 5
                    is GameplayUiState.InProgress -> 3
                    is GameplayUiState.Finished -> 4
                }
            },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "gameplayPhase",
        ) { state ->
            when (state) {
            is GameplayUiState.Loading -> StatusMessage(stringResource(R.string.gameplay_loading))
            is GameplayUiState.Error -> ErrorMessage(state, onRetry = viewModel::retry)
            is GameplayUiState.TutorialPending -> GameplayTutorialOverlay(
                level = state.level,
                onDismiss = viewModel::dismissTutorial,
                modifier = Modifier.fillMaxSize(),
            )
            is GameplayUiState.MechanicIntroPending -> GameplayMechanicIntroOverlay(
                level = state.level,
                mechanic = state.mechanic,
                onDismiss = viewModel::dismissMechanicIntro,
                modifier = Modifier.fillMaxSize(),
            )
            is GameplayUiState.InProgress -> InProgressContent(
                state = state,
                equippedTimerStyleId = equippedCosmetics[CosmeticCategory.TIMER_STYLE],
                onRotate = viewModel::rotatePlacedObject,
                onPickUp = viewModel::pickUpPlacedObject,
                hintState = hintState,
                onToggleHint = viewModel::toggleHintArmed,
                onHintTargetSelected = viewModel::requestHint,
                rewardedHintAdState = rewardedHintAdState,
                onWatchAd = { activity?.let { viewModel.watchRewardedAd(it) } },
                redoState = redoState,
                onRedo = viewModel::redoLastPlacement,
                rewardedRedoAdState = rewardedRedoAdState,
                onWatchRedoAd = { activity?.let { viewModel.watchRewardedRedoAd(it) } },
                rewatchState = rewatchState,
                onRewatchFree = viewModel::watchRewatchFree,
                rewatchAdState = rewatchAdState,
                onWatchRewatchAd = { activity?.let { viewModel.watchRewatchAd(it) } },
                isSubmitting = isSubmitting,
                onSubmit = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        scope.launch {
                            delay(SUBMIT_PROCESSING_DELAY_MS)
                            tick()
                            ambientParticles.confettiBurst(Offset(400f, 300f), count = 20)
                            viewModel.submitReconstruction()
                        }
                    }
                },
                dragState = dragState,
                onDragStart = { objectId, positionInRoot, trayOriginInRoot ->
                    feedback.onObjectPickup()
                    dragState = DragState(objectId, positionInRoot, trayOriginInRoot)
                    scope.launch { floatingOffset.snapTo(positionInRoot) }
                },
                onDrag = { amount ->
                    val current = dragState ?: return@InProgressContent
                    val newPosition = current.positionInRoot + amount
                    val scenePanel = scenePanelCoordinates
                    val root = rootCoordinates
                    val roomArt = currentRoomArt()
                    val newSnap = if (scenePanel != null && root != null && roomArt != null) {
                        val localPx = scenePanel.localPositionOf(root, newPosition)
                        val panelSize = scenePanel.size
                        val fraction = if (panelSize.width > 0 && panelSize.height > 0) {
                            Offset(localPx.x / panelSize.width, localPx.y / panelSize.height)
                        } else {
                            Offset(-1f, -1f)
                        }
                        SlotHitTesting.nearestEmptySlotIndex(fraction, roomArt.slots, occupiedSlots())
                    } else {
                        null
                    }
                    if (newSnap != current.snappedSlotIndex && newSnap != null) tick()
                    dragState = current.copy(positionInRoot = newPosition, snappedSlotIndex = newSnap)
                    val target = newSnap?.let { slotRootPosition(it) } ?: newPosition
                    scope.launch { floatingOffset.animateTo(target, floatSpring) }
                },
                onDragEnd = {
                    val drag = dragState
                    if (drag?.snappedSlotIndex != null) {
                        // No separate tick() here - viewModel.placeObject() below already fires
                        // FeedbackManager.onObjectPlace() (its own sound + haptic); adding a
                        // second, different vibration pattern on top of it for the same single
                        // drop is exactly the "double buzz" that read as odd/off.
                        val dropPosition = slotRootPosition(drag.snappedSlotIndex) ?: floatingOffset.value
                        ambientParticles.confettiBurst(dropPosition, count = 10)
                        viewModel.placeObject(drag.objectId, drag.snappedSlotIndex)
                        dragState = null
                    } else if (drag != null) {
                        // "Wrong placement" here only ever means "no empty slot was near the
                        // release point" - never a scoring judgment, which stays hidden until
                        // Submit. The muted dust puff (vs. success's warm confetti rects) plus the
                        // shake give that unambiguous "didn't land" feedback without saying why.
                        feedback.onWarning()
                        ambientParticles.burst(
                            origin = drag.positionInRoot,
                            colors = listOf(MemoryArchitectColors.textTertiary, MemoryArchitectColors.textSecondary),
                            count = 8,
                        )
                        isSnappingBack = true
                        scope.launch {
                            snapBackOffset.snapTo(drag.positionInRoot)
                            snapBackOffset.animateTo(drag.trayOriginInRoot, tween(220))
                            isSnappingBack = false
                            dragState = null
                        }
                    }
                },
                onDragCancel = { dragState = null },
                onScenePanelPositioned = { scenePanelCoordinates = it },
            )
            is GameplayUiState.Finished -> GameplayResultsPanel(
                state = state,
                onContinue = onBack,
                onPlayAgain = { state.levelOutcome?.let { onReplayLevel(GameMode.CLASSIC, it.levelNumber) } },
                onNextLevel = { state.levelOutcome?.let { onReplayLevel(GameMode.CLASSIC, it.levelNumber + 1) } },
                onBackToLevels = onBack,
                equippedConfettiId = equippedCosmetics[CosmeticCategory.CONFETTI_EFFECT],
                equippedVictoryAnimationId = equippedCosmetics[CosmeticCategory.VICTORY_ANIMATION],
            )
            }
        }

        // Premium floating ghost — while actively dragged it spring-chases the finger (or the
        // snapped slot's exact position once magnetically locked), giving it a soft floating/
        // gliding feel instead of being welded to raw pixels; once released with no snap target
        // it animates smoothly back to the tray (with a gentle non-punishing shake) instead of
        // vanishing.
        val ghostDrag = dragState
        if (ghostDrag != null) {
            val isSnapped = !isSnappingBack && ghostDrag.snappedSlotIndex != null
            val renderPosition = if (isSnappingBack) snapBackOffset.value else floatingOffset.value
            PremiumDragGhost(
                objectId = ghostDrag.objectId,
                isSnapped = isSnapped,
                modifier = Modifier
                    .offset {
                        IntOffset((renderPosition.x - 34.dp.toPx()).toInt(), (renderPosition.y - 34.dp.toPx()).toInt())
                    }
                    .shakeOnTrigger(isSnappingBack),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        ) { data ->
            Snackbar(
                shape = RoundedCornerShape(MemoryArchitectRadii.card),
                containerColor = MemoryArchitectColors.bgElevated,
                contentColor = MemoryArchitectColors.textPrimary,
                actionContentColor = MemoryArchitectColors.accentGold,
            ) {
                Text(text = data.visuals.message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Anti-Cheat Privacy Shield, part 2 (see the DisposableEffect(activity) above for part 1,
        // FLAG_SECURE) - drawn last so it's the topmost layer over everything above, including the
        // drag ghost and Snackbar host, and fully opaque so nothing behind it is visible while up.
        AnimatedVisibility(
            visible = privacyShield != PrivacyShieldPhase.NONE,
            enter = fadeIn(tween(280)),
            exit = fadeOut(tween(220)),
        ) {
            PrivacyShieldOverlay(
                phase = privacyShield,
                countdownValue = shieldCountdown,
                onContinue = onShieldContinue,
            )
        }
    }
}

@Composable
private fun PremiumDragGhost(objectId: String, isSnapped: Boolean, modifier: Modifier = Modifier) {
    val scale = if (isSnapped) 1.22f else 1.12f
    Box(modifier = modifier.size(68.dp), contentAlignment = Alignment.Center) {
        // Soft glow ring — warmer/brighter once magnetically snapped.
        Box(
            modifier = Modifier
                .size(if (isSnapped) 78.dp else 64.dp)
                .graphicsLayer { alpha = if (isSnapped) 0.55f else 0.32f }
                .blur(14.dp)
                .background(if (isSnapped) MemoryArchitectColors.accentGold else MemoryArchitectColors.accentTerracotta, CircleShape),
        )
        // Drop shadow that grows with "lift height."
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(y = 10.dp)
                .graphicsLayer { alpha = 0.35f }
                .blur(8.dp)
                .background(MemoryArchitectColors.bgBase, CircleShape),
        )
        TrayDragGhost(
            objectId = objectId,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}

internal val GameplayHorizontalMargin = 16.dp
internal val GameplayTopExtraPadding = 20.dp
internal val GameplayBottomExtraPadding = 16.dp

@Composable
private fun InProgressContent(
    state: GameplayUiState.InProgress,
    equippedTimerStyleId: CosmeticId?,
    onRotate: (String) -> Unit,
    onPickUp: (String) -> Unit,
    hintState: HintUiState,
    onToggleHint: () -> Unit,
    onHintTargetSelected: (String) -> Unit,
    rewardedHintAdState: RewardedAdUiState,
    onWatchAd: () -> Unit,
    redoState: RedoUiState,
    onRedo: () -> Unit,
    rewardedRedoAdState: RewardedAdUiState,
    onWatchRedoAd: () -> Unit,
    rewatchState: RewatchUiState,
    onRewatchFree: () -> Unit,
    rewatchAdState: RewardedAdUiState,
    onWatchRewatchAd: () -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    dragState: DragState?,
    onDragStart: (String, Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onScenePanelPositioned: (LayoutCoordinates) -> Unit,
) {
    val isReconstructing = state.phase == GamePhase.RECONSTRUCT
    val visibleObjects = remember(state.phase, state.level, state.placements) {
        when (state.phase) {
            GamePhase.MEMORIZE -> state.level.objects
            GamePhase.RECONSTRUCT -> placedObjectsAsScene(state)
            else -> emptyList()
        }
    }
    val totalMs = when (state.phase) {
        GamePhase.MEMORIZE -> state.level.memorizeDurationMs
        GamePhase.RECONSTRUCT -> state.level.timeLimitMs
        else -> null
    }
    val layoutDirection = LocalLayoutDirection.current
    val systemBarPadding = WindowInsets.systemBars.asPaddingValues()

    // Safe-area aware: top/bottom start from the actual status/navigation bar insets and add
    // breathing room on top of them (never instead of them) — the app draws edge-to-edge with no
    // other inset handling, same fix already applied to Mode Select.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = GameplayHorizontalMargin + systemBarPadding.calculateStartPadding(layoutDirection),
                end = GameplayHorizontalMargin + systemBarPadding.calculateEndPadding(layoutDirection),
                top = systemBarPadding.calculateTopPadding() + GameplayTopExtraPadding,
                bottom = systemBarPadding.calculateBottomPadding() + GameplayBottomExtraPadding,
            ),
    ) {
        GameplayHud(
            phaseLabel = phaseLabel(state.phase, state.level.orderModeEnabled),
            remainingMs = state.remainingMs,
            totalMs = totalMs,
            showRotateCaption = state.phase == GamePhase.RECONSTRUCT && state.level.objects.any { it.rotationDegrees != 0 },
            equippedTimerStyleId = equippedTimerStyleId,
        )

        // The room takes the largest square that fits whatever vertical space remains once the
        // tray/submit button (below) claim theirs — bounded by both width and height via
        // fillMaxSize().aspectRatio(1f) inside a weighted, center-aligned Box — so on a tall
        // screen (or during Memorize, when there's no tray at all) the room grows to fill the
        // space instead of leaving it empty below a small fixed square.
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            GameplayScenePanel(
                level = state.level,
                phase = state.phase,
                visibleObjects = visibleObjects,
                onRotate = onRotate,
                onPickUp = onPickUp,
                modifier = Modifier.fillMaxSize().aspectRatio(1f),
                dragActive = dragState != null,
                highlightedSlotIndex = dragState?.snappedSlotIndex,
                hintReveal = hintState.activeReveal,
                onPanelPositioned = onScenePanelPositioned,
            )
        }

        // A single AnimatedVisibility around both tray + button (rather than a plain `if`) means
        // its expand/shrink height animation plays out over several layout passes, and the room
        // panel above — sized via weight(1f) — smoothly grows/shrinks in step with it instead of
        // snapping instantly when the phase changes.
        AnimatedVisibility(
            visible = isReconstructing,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameplayToolbar(
                    hintState = hintState,
                    onToggleHint = onToggleHint,
                    rewardedHintAdState = rewardedHintAdState,
                    onWatchHintAd = onWatchAd,
                    redoState = redoState,
                    canUndo = state.placementOrder.isNotEmpty(),
                    onRedo = onRedo,
                    rewardedRedoAdState = rewardedRedoAdState,
                    onWatchRedoAd = onWatchRedoAd,
                    rewatchState = rewatchState,
                    onRewatchFree = onRewatchFree,
                    rewatchAdState = rewatchAdState,
                    onWatchRewatchAd = onWatchRewatchAd,
                    modifier = Modifier.fillMaxWidth(),
                )
                GameplayTray(
                    trayObjectIds = state.trayObjectIds,
                    draggedObjectId = dragState?.objectId,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    isHintModeArmed = hintState.isArmed,
                    onHintTargetSelected = onHintTargetSelected,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                SubmitButton(
                    remainingCount = state.trayObjectIds.size,
                    isProcessing = isSubmitting,
                    onClick = onSubmit,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** Compose's [LocalContext] is often a [ContextWrapper] around the hosting Activity rather than
 * the Activity itself, so a plain `as? Activity` cast can fail even inside a normal Activity-
 * hosted screen — this unwraps until it finds one, or gives up (rather than crash) if there
 * somehow isn't one. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun placedObjectsAsScene(state: GameplayUiState.InProgress): List<SceneObjectSpec> {
    val originalById = state.level.objects.associateBy { it.objectId }
    return state.placements.values.map { placed ->
        val original = originalById[placed.objectId]
        SceneObjectSpec(
            objectId = placed.objectId,
            slotIndex = placed.slotIndex,
            rotationDegrees = placed.rotationDegrees,
            scale = original?.scale ?: 1f,
            isDistractor = original?.isDistractor ?: false,
        )
    }
}

@Composable
private fun phaseLabel(phase: GamePhase, orderModeEnabled: Boolean): String = when (phase) {
    GamePhase.MEMORIZE -> stringResource(if (orderModeEnabled) R.string.gameplay_phase_memorize_order else R.string.gameplay_phase_memorize)
    GamePhase.HIDDEN -> stringResource(R.string.gameplay_phase_hidden)
    GamePhase.RECONSTRUCT -> stringResource(R.string.gameplay_phase_reconstruct)
    GamePhase.FINISHED -> stringResource(R.string.gameplay_finished)
}

@Composable
private fun StatusMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MemoryArchitectColors.textSecondary)
    }
}

@Composable
private fun ErrorMessage(state: GameplayUiState.Error, onRetry: () -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.error.toDisplayMessage(context),
                style = MaterialTheme.typography.bodyLarge,
                color = MemoryArchitectColors.textSecondary,
            )
            PrimaryButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
