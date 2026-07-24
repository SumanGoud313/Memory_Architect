package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.components.ParticleField
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

private enum class SubmitButtonState { DISABLED, READY, PROCESSING }

/**
 * The Reconstruct phase's Submit control — three distinct states (Disabled/Ready/Processing)
 * derived purely from [remainingCount] and [isProcessing], so picking a placed object back up
 * (which increments the tray count the ViewModel already tracks) reactively drops the button
 * straight back to Disabled with zero extra wiring. [onClick] is only ever invoked while Ready;
 * the click is dropped entirely (never evaluated) otherwise — the defensive re-check for a
 * player somehow triggering Submit early lives in [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.submitReconstruction]
 * itself, not here.
 */
@Composable
fun SubmitButton(
    remainingCount: Int,
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticsTick()
    val isComplete = remainingCount == 0
    val state = when {
        isProcessing -> SubmitButtonState.PROCESSING
        isComplete -> SubmitButtonState.READY
        else -> SubmitButtonState.DISABLED
    }

    val interactionSource = remember { MutableInteractionSource() }
    val sparkles = rememberParticleFieldState(ambientCount = 0)
    var buttonSizePx by remember { mutableStateOf(IntSize.Zero) }

    // Idle glow/pulse — only while Ready, so a Disabled or Processing button never animates.
    val infiniteTransition = rememberInfiniteTransition(label = "submitButtonReady")
    val idlePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "submitIdlePulse",
    )
    val idleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "submitIdleGlow",
    )

    // One-time celebration exactly when the button transitions into Ready — a quick scale bump,
    // a sparkle burst, and a haptic tick, on top of (not instead of) the persistent idle glow.
    val readyBump = remember { Animatable(1f) }
    LaunchedEffect(isComplete) {
        if (isComplete) {
            tick()
            readyBump.snapTo(1f)
            readyBump.animateTo(1.12f, tween(180, easing = FastOutSlowInEasing))
            readyBump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            if (buttonSizePx.width > 0) {
                sparkles.burst(
                    origin = Offset(buttonSizePx.width / 2f, buttonSizePx.height / 2f),
                    colors = listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.textPrimary),
                    count = 14,
                )
            }
        }
    }

    val disabledAlpha by animateFloatAsState(
        targetValue = if (state == SubmitButtonState.DISABLED) 0.45f else 1f,
        animationSpec = tween(200),
        label = "submitDisabledAlpha",
    )

    val disabledDescription = pluralStringResource(R.plurals.gameplay_submit_disabled_description, remainingCount, remainingCount)
    val readyDescription = stringResource(R.string.gameplay_submit_ready_description)
    val processingDescription = stringResource(R.string.gameplay_submit_processing_description)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (state == SubmitButtonState.READY) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = idleGlowAlpha
                        scaleX = idlePulseScale
                        scaleY = idlePulseScale
                    }
                    .blur(24.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(MemoryArchitectColors.accentGold, Color.Transparent)),
                        shape = RoundedCornerShape(MemoryArchitectRadii.button),
                    ),
            )
        }

        ParticleField(state = sparkles, ambient = false, modifier = Modifier.matchParentSize())

        Box(
            modifier = Modifier
                .onSizeChanged { buttonSizePx = it }
                .pressableScale(interactionSource)
                .graphicsLayer {
                    alpha = disabledAlpha
                    val s = if (state == SubmitButtonState.READY) idlePulseScale * readyBump.value else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(MemoryArchitectRadii.button))
                .background(brush = Brush.horizontalGradient(listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold)))
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = when (state) {
                        SubmitButtonState.DISABLED -> disabledDescription
                        SubmitButtonState.READY -> readyDescription
                        SubmitButtonState.PROCESSING -> processingDescription
                    }
                }
                .clickable(interactionSource = interactionSource, indication = null, enabled = state == SubmitButtonState.READY) {
                    tick()
                    onClick()
                }
                .padding(horizontal = 32.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state == SubmitButtonState.PROCESSING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MemoryArchitectColors.bgBase,
                    )
                    Text(
                        text = stringResource(R.string.gameplay_submit_processing),
                        color = MemoryArchitectColors.bgBase,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.gameplay_submit),
                    color = MemoryArchitectColors.bgBase,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
