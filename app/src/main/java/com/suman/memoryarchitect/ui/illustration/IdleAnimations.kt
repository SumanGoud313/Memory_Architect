package com.suman.memoryarchitect.ui.illustration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random
import kotlinx.coroutines.isActive

/**
 * The five idle-animation categories, implemented once here and reused by every object across
 * every room via [Modifier.applyIdleAnimation] — object drawer composables (see the
 * `ui/illustration/objects` package) stay pure static illustrations and never duplicate this logic.
 */
fun Modifier.applyIdleAnimation(kind: IdleAnimationKind): Modifier = when (kind) {
    IdleAnimationKind.NONE -> this
    IdleAnimationKind.FLICKER -> idleFlicker()
    IdleAnimationKind.SWAY -> idleSway()
    IdleAnimationKind.PULSE -> idlePulse()
    IdleAnimationKind.ROTATE -> idleRotate()
    // STEAM has no self-transform — it's an overlay emitter, composed alongside the object by
    // IdleAnimatedObject below.
    IdleAnimationKind.STEAM -> this
}

/** Candle/fire-style flicker: irregular opacity + scale jitter from two out-of-phase sine waves. */
fun Modifier.idleFlicker(intensity: Float = 0.14f): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "flicker")
    val a by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(230, easing = LinearEasing), RepeatMode.Reverse),
        label = "flickerA",
    )
    val b by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(370, easing = LinearEasing), RepeatMode.Reverse),
        label = "flickerB",
    )
    // `a`/`b` are read inside this graphicsLayer lambda (draw phase), not hoisted into a plain
    // local first - every candle/fireplace/screen using this modifier ticks two independent
    // continuous animations, so reading eagerly here would recompose every such object every
    // frame instead of just redrawing it.
    graphicsLayer {
        val noise = a * 0.6f + b * 0.4f
        alpha = 1f - intensity + intensity * noise
        val scale = 1f - intensity * 0.3f + intensity * 0.3f * noise
        scaleX = scale
        scaleY = scale
    }
}

/** Gentle hanging-object sway (curtains, plants, coat hangers) — sine rotation about the base. */
fun Modifier.idleSway(maxDegrees: Float = 4f, periodMs: Int = 2600): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "sway")
    val angle by transition.animateFloat(
        initialValue = -maxDegrees, targetValue = maxDegrees,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swayAngle",
    )
    graphicsLayer {
        rotationZ = angle
        transformOrigin = TransformOrigin(0.5f, 0.85f)
    }
}

/** "Breathing" glow-pulse (lamps, screens, RGB gear) — soft alpha+scale cycle. */
fun Modifier.idlePulse(minScale: Float = 0.97f, maxScale: Float = 1.04f, periodMs: Int = 1600): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale, targetValue = maxScale,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.88f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/** Continuous spin (fans, coffee grinders). */
fun Modifier.idleRotate(periodMs: Int = 6000): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "rotate")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "rotateAngle",
    )
    graphicsLayer { rotationZ = angle }
}

private class SteamWisp(
    var x: Float,
    var y: Float,
    var vy: Float,
    var life: Float,
    val radius: Float,
    val drift: Float,
)

/**
 * Small rising-wisp emitter drawn above an object's top edge (kettles, mugs, espresso machines).
 * Built on the same soft radial-gradient-dot technique as [com.suman.memoryarchitect.ui.components.ParticleField]
 * but self-contained (continuous local emission, not ambient/burst), so it can be layered
 * directly above any object without a shared full-screen particle state.
 */
@androidx.compose.runtime.Composable
fun SteamEmitter(modifier: Modifier = Modifier.fillMaxSize(), color: Color = Color.White) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val wisps = remember { mutableStateListOf<SteamWisp>() }
    var frameTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var lastFrameNanos = -1L
        var spawnAccumulator = 0f
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val dt = if (lastFrameNanos < 0) 0f else ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrameNanos = frameNanos

            if (size.width > 0) {
                spawnAccumulator += dt
                if (spawnAccumulator > 0.4f) {
                    spawnAccumulator = 0f
                    wisps.add(
                        SteamWisp(
                            x = size.width / 2f + (Random.nextFloat() - 0.5f) * size.width * 0.25f,
                            y = size.height.toFloat(),
                            vy = -(Random.nextFloat() * 14f + 16f),
                            life = 1f,
                            radius = Random.nextFloat() * 3.5f + 3f,
                            drift = (Random.nextFloat() - 0.5f) * 10f,
                        ),
                    )
                }
                val iterator = wisps.iterator()
                while (iterator.hasNext()) {
                    val w = iterator.next()
                    w.y += w.vy * dt
                    w.x += w.drift * dt
                    w.vy *= 0.985f
                    w.life -= dt / 1.3f
                    if (w.life <= 0f) iterator.remove()
                }
            }
            frameTick++
        }
    }

    Canvas(modifier = modifier.onSizeChanged { size = it }) {
        @Suppress("UNUSED_EXPRESSION")
        frameTick
        wisps.forEach { w ->
            val alpha = (w.life * 0.35f).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = Offset(w.x, w.y),
                    radius = w.radius * 3f,
                ),
                radius = w.radius * 3f,
                center = Offset(w.x, w.y),
            )
        }
    }
}
