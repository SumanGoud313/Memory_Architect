package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compose replacement for the old `View.shake()` — same translationX keyframe steps
 * (0, -14, 14, -8.4, 8.4, 0) over 400ms, used on a perfect 3-star result panel. Fires once
 * every time [trigger] flips to true (e.g. `stars == 3` becoming true on a Finished state).
 */
fun Modifier.shakeOnTrigger(trigger: Boolean, offset: Dp = 14.dp): Modifier = composed {
    val offsetPx = with(LocalDensity.current) { offset.toPx() }
    val translationX = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            translationX.snapTo(0f)
            translationX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -offsetPx at 66
                    offsetPx at 133
                    -offsetPx * 0.6f at 200
                    offsetPx * 0.6f at 266
                    0f at 400
                },
            )
        }
    }

    graphicsLayer(translationX = translationX.value)
}
