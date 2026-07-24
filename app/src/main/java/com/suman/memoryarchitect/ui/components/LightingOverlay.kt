package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.suman.memoryarchitect.domain.generation.LightingMood
import kotlin.random.Random

/**
 * A cheap, reusable atmospheric wash drawn over any [com.suman.memoryarchitect.ui.illustration.RoomArt]
 * backdrop — a translucent tint (+ a few procedural details for Rainy/Night) rather than a
 * separately hand-drawn alternate room. Draws *above the backdrop but below every placed
 * object*, so the room's mood shifts without ever recoloring or obscuring the objects the player
 * actually needs to identify — see [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScenePanel].
 * [LightingMood.DAY] renders nothing.
 */
@Composable
fun LightingOverlay(mood: LightingMood, modifier: Modifier = Modifier, seed: Long = 0L) {
    if (mood == LightingMood.DAY) return
    Canvas(modifier = modifier) {
        when (mood) {
            LightingMood.NIGHT -> drawNight(seed)
            LightingMood.RAINY -> drawRainy(seed)
            LightingMood.SUNSET -> drawSunset()
            LightingMood.DAY -> Unit
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNight(seed: Long) {
    drawRect(color = Color(0xFF0B1226).copy(alpha = 0.34f), size = size)
    val random = Random(seed xor 0x1101L)
    repeat(4) {
        val center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * 0.4f)
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color(0xFFCEE0EC).copy(alpha = 0.10f), Color.Transparent), center = center, radius = size.width * 0.14f),
            radius = size.width * 0.14f,
            center = center,
        )
    }
    // A soft, cool moon glow in one corner — never the same corner twice for the same seed.
    val moonCenter = Offset(if (random.nextBoolean()) size.width * 0.88f else size.width * 0.12f, size.height * 0.1f)
    drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFF7F1E8).copy(alpha = 0.5f), Color.Transparent), center = moonCenter, radius = size.width * 0.1f), radius = size.width * 0.06f, center = moonCenter)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRainy(seed: Long) {
    drawRect(color = Color(0xFF4A5568).copy(alpha = 0.22f), size = size)
    val random = Random(seed xor 0x2202L)
    repeat(26) {
        val startX = random.nextFloat() * size.width * 1.2f - size.width * 0.1f
        val startY = random.nextFloat() * size.height
        val length = size.height * (0.06f + random.nextFloat() * 0.08f)
        drawLine(
            color = Color(0xFFCEE0EC).copy(alpha = 0.14f + random.nextFloat() * 0.1f),
            start = Offset(startX, startY),
            end = Offset(startX - length * 0.35f, startY + length),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunset() {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE8674B).copy(alpha = 0.22f), Color(0xFFF0C875).copy(alpha = 0.08f), Color.Transparent),
            start = Offset(size.width, size.height * 0.05f),
            end = Offset(size.width * 0.2f, size.height),
        ),
        size = size,
    )
}
