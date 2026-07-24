package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private const val PULSE_THRESHOLD_SECONDS = 5
private const val HAPTIC_THRESHOLD_SECONDS = 3

/**
 * A circular, color-graded countdown shared by every timed gameplay phase (Memorize and Rebuild)
 * — replaces a plain-text pill so the timer is always visually present and reads the same way
 * regardless of phase. Color tracks [remainingMs] as a *fraction* of [totalMs] (green → yellow →
 * orange → red) rather than absolute seconds, so a short memorize window and a much longer
 * rebuild countdown both feel proportionally paced instead of one always looking "more urgent."
 * [pulseOnUrgent]/[vibrateOnUrgent] are separate opt-ins (Rebuild only, per design) so a short
 * Memorize phase doesn't spend its entire duration buzzing/pulsing.
 */
@Composable
fun CircularCountdownTimer(
    remainingMs: Long,
    totalMs: Long,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 52.dp,
    pulseOnUrgent: Boolean = false,
    vibrateOnUrgent: Boolean = false,
) {
    val tick = rememberHapticsTick()
    val remainingSeconds = (remainingMs / 1000 + 1).coerceAtLeast(0)
    val fraction = if (totalMs > 0) (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
    val isUrgent = remainingSeconds <= PULSE_THRESHOLD_SECONDS

    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(150), label = "timerFraction")
    val targetColor = when {
        fraction > 0.5f -> MemoryArchitectColors.accentSage
        fraction > 0.25f -> MemoryArchitectColors.accentAmber
        fraction > 0.1f -> MemoryArchitectColors.accentTerracotta
        else -> MemoryArchitectColors.danger
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(300), label = "timerColor")

    val urgentTransition = rememberInfiniteTransition(label = "timerUrgentPulse")
    val urgentPulseScale by urgentTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulseOnUrgent && isUrgent) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
        label = "timerUrgentScale",
    )

    LaunchedEffect(remainingSeconds, vibrateOnUrgent) {
        if (vibrateOnUrgent && remainingSeconds in 1..HAPTIC_THRESHOLD_SECONDS) {
            tick()
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer { scaleX = urgentPulseScale; scaleY = urgentPulseScale },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.11f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = MemoryArchitectColors.glassStrokeBright,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$remainingSeconds",
            style = MaterialTheme.typography.labelLarge,
            color = MemoryArchitectColors.textPrimary,
        )
    }
}
