package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Compose replacement for the old `View.applyPressSpring()` — springs down to [pressedScale]
 * while pressed via the same bouncy spring feel (`DampingRatioMediumBouncy`/`StiffnessMedium`).
 * Does not consume the press/click itself; combine with `Modifier.clickable`/`combinedClickable`.
 *
 * Used on nearly every interactive element in the app (every button, card, tray chip), so the
 * `graphicsLayer { }` *lambda* overload here is deliberate, not stylistic — it defers reading
 * [scale] to the draw phase, so the spring ticking through a press/release never recomposes this
 * modifier's `composed { }` scope. The plain-parameter `graphicsLayer(scaleX = scale, ...)`
 * overload looks identical but reads [scale] eagerly while the modifier chain is being built
 * (i.e. during composition), which would recompose this scope on every one of the spring's frames
 * — cheap in isolation, but this modifier is on nearly every tappable surface in the app, so that
 * cost is paid everywhere at once.
 */
fun Modifier.pressableScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.93f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressableScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}
