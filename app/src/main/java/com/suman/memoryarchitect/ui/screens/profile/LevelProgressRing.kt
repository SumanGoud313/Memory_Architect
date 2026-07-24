package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private val DefaultRingSweep = listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold, MemoryArchitectColors.accentTerracotta)

/**
 * Hand-drawn circular level-progress ring (a Canvas arc, no image asset) — the level number
 * sits inside, the sweep shows how far into the current level the player's XP is. [sizeDp]
 * defaults to the original Profile-screen size but is overridable (e.g. a smaller results-screen
 * instance) without affecting existing callers. [accentSweep] lets an equipped milestone-reward
 * palette (see [com.suman.memoryarchitect.ui.theme.AccentPaletteCatalog]) recolor the ring;
 * defaults to the original terracotta/gold sweep when no palette is equipped.
 */
@Composable
fun LevelProgressRing(
    level: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 128.dp,
    accentSweep: List<Color> = DefaultRingSweep,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "levelRingProgress",
    )

    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.09f
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
                brush = Brush.sweepGradient(accentSweep),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$level",
            style = if (sizeDp < 110.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
            color = accentSweep.getOrElse(1) { MemoryArchitectColors.accentGold },
        )
    }
}
