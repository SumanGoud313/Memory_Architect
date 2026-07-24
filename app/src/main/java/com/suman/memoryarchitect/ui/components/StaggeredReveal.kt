package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose replacement for the old `EntranceAnimations.staggeredReveal(views)` — each element
 * applying this modifier starts at alpha=0/translationY=40dp and animates in with the same
 * `baseDelayMs + index*staggerMs` timing and overshoot-flavored ease used everywhere on Home,
 * LevelSelect, and Gameplay results today. Apply directly to each child with its own [index]
 * (no wrapping container needed, unlike the old View-list API).
 */
fun Modifier.staggeredReveal(
    index: Int,
    baseDelayMs: Long = 60L,
    staggerMs: Long = 90L,
): Modifier = composed {
    val offsetPx = with(LocalDensity.current) { 40.dp.toPx() }
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(offsetPx) }

    LaunchedEffect(index) {
        delay(baseDelayMs + index * staggerMs)
        launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 420)) }
        launch {
            translationY.animateTo(
                0f,
                animationSpec = tween(durationMillis = 420, easing = PopInOvershoot),
            )
        }
    }

    graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}
