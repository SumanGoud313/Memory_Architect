package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * The tutorial's guiding hand — semi-transparent, soft-shadowed, with a gentle idle bounce so it
 * reads as alive rather than a static icon pasted on screen. Purely presentational: [isPressing]
 * drives a quick squeeze accent for the tap/grab moments; the actual movement between points is
 * choreographed by the caller (see [GameplayTutorialOverlay]) via an [androidx.compose.animation.core.Animatable]
 * and expressed here only through [modifier] (an offset).
 */
@Composable
fun TutorialHand(isPressing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "handIdleBounce")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "handBounce",
    )
    val pressScale = if (isPressing) 0.85f else 1f

    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.Center) {
        // Soft drop shadow beneath the hand — same blurred-wash grounding technique used
        // throughout the app (Mode Select's icon badges, the real drag ghost).
        Box(
            modifier = Modifier
                .size(40.dp)
                .offset(y = 16.dp)
                .graphicsLayer { alpha = 0.3f }
                .blur(8.dp)
                .background(MemoryArchitectColors.bgBase, CircleShape),
        )
        Icon(
            imageVector = Icons.Filled.BackHand,
            contentDescription = null,
            tint = MemoryArchitectColors.textPrimary.copy(alpha = 0.85f),
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    translationY = (bounce - 0.5f) * 6f
                    scaleX = pressScale
                    scaleY = pressScale
                    rotationZ = -18f
                },
        )
    }
}
