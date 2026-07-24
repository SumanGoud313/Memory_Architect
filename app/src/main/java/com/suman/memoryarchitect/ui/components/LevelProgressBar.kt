package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Slim rounded gradient fill bar — a visual companion to a plain "N / total" text readout
 * (e.g. Level Select's campaign progress, Home's achievement count). Animates toward
 * [fraction] with the same tween timing [com.suman.memoryarchitect.ui.screens.profile.LevelProgressRing]
 * uses, so the two progress affordances feel like one system.
 */
@Composable
fun LevelProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "levelProgressBar",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MemoryArchitectColors.glassFill, RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold),
                    ),
                    shape = RoundedCornerShape(50),
                ),
        )
    }
}
