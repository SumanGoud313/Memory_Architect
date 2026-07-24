package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.generation.LevelMirrorPolicy
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import com.suman.memoryarchitect.ui.components.ConfettiBurst
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.UnlockBurst
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.illustration.IdleAnimatedObject
import com.suman.memoryarchitect.ui.illustration.ObjectArtRegistry
import com.suman.memoryarchitect.ui.illustration.RoomArtRegistry
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay

private enum class TutorialStep { INTRO, HIDE, DRAG, TAP_SUBMIT, SUCCESS }

/**
 * A one-time, scripted walkthrough of the full Memorize → Rebuild → Submit loop, narrated by an
 * animated hand — shown only for a fresh player's very first Level 1 attempt (see
 * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.shouldShowTutorial]). This is
 * deliberately self-contained: it renders [GameplayScenePanel] read-only against the real Level 1
 * [LevelSpec] for visual consistency, but never touches [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]'s
 * placement/scoring state — the "Submit" button here is a no-op mockup, and the real Memorize
 * timer only starts once [onDismiss] fires, so the demo can never eat into the player's actual
 * time budget or accidentally score a placement the player didn't make themselves.
 */
@Composable
fun GameplayTutorialOverlay(level: LevelSpec, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(TutorialStep.INTRO) }
    var demoObjectPlaced by remember { mutableStateOf(false) }
    val demoObject = remember(level) { level.objects.firstOrNull { !it.isDistractor } ?: level.objects.first() }

    val particles = rememberParticleFieldState(ambientCount = 0)
    val handOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var handVisible by remember { mutableStateOf(false) }
    var handPressing by remember { mutableStateOf(false) }

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var panelCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var trayChipCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var submitCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val mirrored = remember(level.seed) { LevelMirrorPolicy.isMirrored(level.seed) }
    val roomArt = remember(level.sceneType, mirrored) { RoomArtRegistry.get(level.sceneType).mirrored(mirrored) }

    fun rootCenterOf(coords: LayoutCoordinates?): Offset? {
        val target = coords ?: return null
        val root = rootCoordinates ?: return null
        return root.localPositionOf(target, Offset(target.size.width / 2f, target.size.height / 2f))
    }

    fun slotRootPosition(): Offset? {
        val panel = panelCoordinates ?: return null
        val root = rootCoordinates ?: return null
        val slot = roomArt.slotAt(demoObject.slotIndex)
        val localPx = Offset(slot.xFraction * panel.size.width, slot.yFraction * panel.size.height)
        return root.localPositionOf(panel, localPx)
    }

    LaunchedEffect(step) {
        when (step) {
            TutorialStep.INTRO -> {
                delay(2600)
                step = TutorialStep.HIDE
            }
            TutorialStep.HIDE -> {
                delay(700)
                // The tray chip only enters composition once we're past INTRO — give layout a
                // few frames to report its position before the hand needs it.
                var attempts = 0
                while (trayChipCoordinates == null && attempts < 30) {
                    delay(50)
                    attempts++
                }
                step = TutorialStep.DRAG
            }
            TutorialStep.DRAG -> {
                val start = rootCenterOf(trayChipCoordinates)
                val target = slotRootPosition()
                if (start == null || target == null) {
                    // Coordinates never resolved (e.g. a measurement edge case) — don't strand
                    // the player on a frozen demo, just skip straight to the outcome.
                    demoObjectPlaced = true
                    step = TutorialStep.TAP_SUBMIT
                } else {
                    handOffset.snapTo(start)
                    handVisible = true
                    delay(500)
                    handPressing = true
                    delay(220)
                    handPressing = false
                    delay(150)
                    handOffset.animateTo(
                        target,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    )
                    demoObjectPlaced = true
                    particles.confettiBurst(
                        origin = target,
                        colors = listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.textPrimary),
                        count = 22,
                    )
                    delay(550)
                    handVisible = false
                    delay(350)
                    step = TutorialStep.TAP_SUBMIT
                }
            }
            TutorialStep.TAP_SUBMIT -> {
                val target = rootCenterOf(submitCoordinates)
                if (target != null) {
                    handOffset.snapTo(Offset(target.x, target.y - 90f))
                    handVisible = true
                    delay(350)
                    handOffset.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                    delay(150)
                    handPressing = true
                    delay(220)
                    handPressing = false
                    delay(300)
                    handVisible = false
                }
                delay(300)
                step = TutorialStep.SUCCESS
            }
            TutorialStep.SUCCESS -> {
                delay(1900)
                onDismiss()
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val systemBarPadding = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it },
    ) {
        Box(Modifier.fillMaxSize().background(MemoryArchitectColors.bgBase.copy(alpha = 0.55f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = GameplayHorizontalMargin + systemBarPadding.calculateStartPadding(layoutDirection),
                    end = GameplayHorizontalMargin + systemBarPadding.calculateEndPadding(layoutDirection),
                    top = systemBarPadding.calculateTopPadding() + GameplayTopExtraPadding,
                    bottom = systemBarPadding.calculateBottomPadding() + GameplayBottomExtraPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TutorialCaption(step = step, modifier = Modifier.fillMaxWidth())

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                GameplayScenePanel(
                    level = level,
                    phase = if (step == TutorialStep.INTRO) GamePhase.MEMORIZE else GamePhase.HIDDEN,
                    visibleObjects = tutorialVisibleObjects(step, level.objects, demoObject, demoObjectPlaced),
                    onRotate = {},
                    onPickUp = {},
                    modifier = Modifier.fillMaxSize().aspectRatio(1f).onGloballyPositioned { panelCoordinates = it },
                )
            }

            if (!demoObjectPlaced && step != TutorialStep.INTRO) {
                DemoTrayChip(
                    objectId = demoObject.objectId,
                    modifier = Modifier.padding(bottom = 16.dp).onGloballyPositioned { trayChipCoordinates = it },
                )
            }

            PrimaryButton(
                text = stringResource(R.string.gameplay_submit),
                onClick = {},
                modifier = Modifier
                    .graphicsLayer { alpha = if (demoObjectPlaced) 1f else 0.5f }
                    .onGloballyPositioned { submitCoordinates = it },
            )
        }

        ConfettiBurst(state = particles, modifier = Modifier.fillMaxSize())

        if (handVisible) {
            TutorialHand(
                isPressing = handPressing,
                modifier = Modifier.offset {
                    IntOffset((handOffset.value.x - 28.dp.toPx()).toInt(), (handOffset.value.y - 28.dp.toPx()).toInt())
                },
            )
        }

        if (step != TutorialStep.SUCCESS) {
            SkipButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(top = systemBarPadding.calculateTopPadding() + 8.dp, end = 8.dp))
        }

        if (step == TutorialStep.SUCCESS) {
            TutorialSuccessCelebration(modifier = Modifier.align(Alignment.Center))
        }
    }
}

private fun tutorialVisibleObjects(
    step: TutorialStep,
    allObjects: List<SceneObjectSpec>,
    demoObject: SceneObjectSpec,
    demoObjectPlaced: Boolean,
): List<SceneObjectSpec> = when {
    step == TutorialStep.INTRO -> allObjects
    demoObjectPlaced -> listOf(demoObject)
    else -> emptyList()
}

@Composable
private fun TutorialCaption(step: TutorialStep, modifier: Modifier = Modifier) {
    val text = when (step) {
        TutorialStep.INTRO -> stringResource(R.string.gameplay_tutorial_caption_memorize)
        TutorialStep.HIDE, TutorialStep.DRAG -> stringResource(R.string.gameplay_tutorial_caption_drag)
        TutorialStep.TAP_SUBMIT -> stringResource(R.string.gameplay_tutorial_caption_submit)
        TutorialStep.SUCCESS -> stringResource(R.string.gameplay_tutorial_caption_success)
    }
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        AnimatedContent(
            targetState = text,
            // clip = false: captions vary a lot in length, and the default SizeTransform clips
            // the outgoing/incoming pill to an in-between size while animating, which reads as
            // text getting cut off mid-word for a couple of frames.
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) using SizeTransform(clip = false) },
            label = "tutorialCaption",
        ) { currentText ->
            PillBadge(text = currentText)
        }
    }
}

@Composable
private fun DemoTrayChip(objectId: String, modifier: Modifier = Modifier) {
    val art = remember(objectId) { ObjectArtRegistry.get(objectId) }
    GlassCard(modifier = modifier.size(68.dp)) {
        IdleAnimatedObject(art = art, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressableScale(interactionSource)
            .background(MemoryArchitectColors.glassFill, RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null) {
                tick()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.gameplay_tutorial_skip),
            style = MaterialTheme.typography.labelLarge,
            color = MemoryArchitectColors.textSecondary,
        )
    }
}

@Composable
private fun TutorialSuccessCelebration(modifier: Modifier = Modifier) {
    UnlockBurst(modifier = modifier) {
        GlassCard(tint = MemoryArchitectColors.accentGold.copy(alpha = 0.18f)) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MemoryArchitectColors.accentGold, modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.gameplay_tutorial_success_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MemoryArchitectColors.accentGold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
