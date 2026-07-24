package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Reward-celebration confetti — a non-ambient [ParticleField] emitting small rotating rectangles
 * instead of glow dots. Used on the Gameplay results panel (see GameplayResultsPanel).
 */
@Composable
fun ConfettiBurst(state: ParticleFieldState, modifier: Modifier = Modifier.fillMaxSize()) {
    ParticleField(state = state, modifier = modifier, ambient = false)
}

fun ParticleFieldState.confettiBurst(
    origin: Offset,
    colors: List<Color> = listOf(
        MemoryArchitectColors.accentGold,
        MemoryArchitectColors.accentTerracotta,
        MemoryArchitectColors.accentSage,
        MemoryArchitectColors.textPrimary,
    ),
    count: Int = 60,
) = burst(origin = origin, colors = colors, count = count, rects = true)
