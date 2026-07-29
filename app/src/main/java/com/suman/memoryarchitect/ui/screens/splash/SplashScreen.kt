package com.suman.memoryarchitect.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import kotlinx.coroutines.launch

/**
 * The premium cold-start splash - shown as the topmost overlay in [com.suman.memoryarchitect.ui.ConnectivityGate]
 * for [SplashTiming.ENTRANCE_DURATION_MS] + [SplashTiming.HOLD_DURATION_MS] before that overlay's
 * own `AnimatedVisibility` fades it out (see that file). This composable owns only the *entrance*
 * (fade + scale + glow) - the exit crossfade into Home is deliberately a separate concern, handled
 * by the caller wrapping this in `AnimatedVisibility(exit = fadeOut(...))`, so this file never needs
 * to know whether it's about to be dismissed.
 *
 * Background is a dark blue/black radial gradient - intentionally the "dark neon glass" navy/violet
 * palette (matching [R.drawable.memory_architect_logo]'s own actual colors, sampled directly from
 * the source PNG: pure black corners, deep navy `#080D12`-ish mid-tones) rather than
 * [com.suman.memoryarchitect.ui.theme.MemoryArchitectColors]' current warm terracotta in-game
 * palette. Those are two deliberately different visual registers - the logo itself is cool
 * blue/black, so a warm terracotta backdrop behind it would clash; the gameplay screens' warm
 * palette is a separate, later styling decision for the room-decorating experience itself. Uses the
 * same hex values as `colors.xml`'s `bg_deep_space`/`bg_deep_space_alt` tokens (also reused by
 * `ic_launcher_background.xml` and `Theme.MemoryArchitect.Splash`) so the adaptive launcher icon,
 * the native Android 12+ splash, and this Compose splash are all one consistent color, not three
 * independent guesses - kept as plain Kotlin [Color] constants rather than `colorResource()` calls
 * since no other Compose screen in this app reads XML color resources either.
 *
 * The full, unmodified [R.drawable.memory_architect_logo] is used directly (no crop, no inset, no
 * re-render) - unlike the adaptive launcher icon's foreground, this splash icon is never
 * system-masked, so nothing here risks clipping any part of the artwork.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(SplashTiming.ENTRANCE_START_SCALE) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, tween(SplashTiming.ENTRANCE_DURATION_MS.toInt(), easing = FastOutSlowInEasing))
        }
        launch {
            scale.animateTo(1f, tween(SplashTiming.ENTRANCE_DURATION_MS.toInt(), easing = FastOutSlowInEasing))
        }
        launch {
            glowAlpha.animateTo(1f, tween(SplashTiming.ENTRANCE_DURATION_MS.toInt(), easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SplashColors.DeepSpaceAlt, SplashColors.DeepSpace, Color.Black),
                    center = Offset(0.5f, 0.42f),
                    radius = 1400f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Soft glow halo behind the logo - a low-alpha radial gradient, not a Modifier.blur, so it
        // renders identically across every API level this app supports (minSdk 26) rather than
        // depending on RenderEffect availability.
        Box(
            modifier = Modifier
                .size(SplashTiming.GLOW_SIZE_DP.dp)
                .graphicsLayer { this.alpha = glowAlpha.value }
                .background(
                    Brush.radialGradient(
                        colors = listOf(SplashColors.Glow.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )

        Image(
            painter = painterResource(R.drawable.memory_architect_logo),
            contentDescription = null,
            modifier = Modifier
                .size(SplashTiming.LOGO_SIZE_DP.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

/** Same hex values as `colors.xml`'s `bg_deep_space`/`bg_deep_space_alt`/`bg_deep_space_glow` -
 * see [SplashScreen]'s own doc for why this stays independent of [com.suman.memoryarchitect.ui.theme.MemoryArchitectColors]. */
private object SplashColors {
    val DeepSpace = Color(0xFF0A0A16)
    val DeepSpaceAlt = Color(0xFF160B2E)
    val Glow = Color(0xFFA855F7) // matches colors.xml's neon_violet
}

/** Central timing/sizing constants for the splash - shared with [com.suman.memoryarchitect.ui.ConnectivityGate],
 * which owns the hold-then-exit scheduling this composable itself doesn't need to know about. */
object SplashTiming {
    /** Within the requested ~700-900ms window for the fade + scale + glow entrance. */
    const val ENTRANCE_DURATION_MS = 800L
    const val ENTRANCE_START_SCALE = 0.95f
    /** How long the fully-formed logo holds on screen before [com.suman.memoryarchitect.ui.ConnectivityGate] begins its exit fade. */
    const val HOLD_DURATION_MS = 200L
    const val EXIT_FADE_DURATION_MS = 300L
    const val LOGO_SIZE_DP = 220
    const val GLOW_SIZE_DP = 320
}
