package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.launch

/**
 * One-shot "just unlocked" celebration — an overshoot scale-in (same curve as the gameplay scene's
 * object pop-in) plus a brief radial glow flash behind the content. Used for newly-unlocked
 * achievements, the level-unlock banner, and new-record badges on the results screen; nothing
 * else in the app currently has a dedicated unlock moment.
 */
@Composable
fun UnlockBurst(
    modifier: Modifier = Modifier,
    glowColor: Color = MemoryArchitectColors.accentGold,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(0.3f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, tween(450, easing = PopInOvershoot)) }
        launch {
            glowAlpha.animateTo(0.5f, tween(200))
            glowAlpha.animateTo(0f, tween(600))
        }
    }

    Box(
        modifier = modifier
            .drawBehind {
                if (glowAlpha.value > 0.01f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = glowAlpha.value), glowColor.copy(alpha = 0f)),
                            center = center,
                            radius = size.maxDimension * 0.75f,
                        ),
                    )
                }
            }
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        content()
    }
}
