package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Full-bleed warm diagonal gradient — replaces the old `bg_deep_space_gradient` drawable
 * used behind every top-level screen (Home, ModeSelect, LevelSelect, Gameplay, Profile).
 *
 * [colors] defaults to the app's own signature gradient - a no-op for every screen unless a
 * Background Theme cosmetic is equipped, same "optional color param, default reproduces today's
 * exact look" discipline [com.suman.memoryarchitect.ui.components.premiumBorder] already
 * established. Not `@Composable` (no `drawWithCache`-incompatible state read needed), so the
 * caller ([AmbientBackground]) resolves the equipped color list itself and passes it in.
 */
fun Modifier.warmGradientBackground(colors: List<Color> = MemoryArchitectColors.backgroundGradient): Modifier = this
    .fillMaxSize()
    .background(
        Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        ),
    )

/**
 * A soft, off-center radial highlight layered over [warmGradientBackground] — reads as a single
 * warm light source in the room rather than a flat wash, and gives the flat gradient a sense of
 * depth. Deliberately very low alpha: it's a mood cue, not a shape anyone should consciously
 * notice. See [AmbientBackground] for how this combines with the particle depth layers.
 * [glowColor] defaults to the app's own gold accent, same no-op-unless-themed discipline as
 * [warmGradientBackground].
 */
fun Modifier.warmGlowOverlay(glowColor: Color = MemoryArchitectColors.accentGold): Modifier = this.drawWithCache {
    val glow = Brush.radialGradient(
        colors = listOf(glowColor.copy(alpha = 0.10f), Color.Transparent),
        center = Offset(size.width * 0.82f, size.height * 0.1f),
        radius = size.maxDimension * 0.6f,
    )
    onDrawBehind { drawRect(brush = glow) }
}
