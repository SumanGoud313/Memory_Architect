package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlin.random.Random
import kotlinx.coroutines.isActive

/**
 * Compose replacement for the old `ParticleBurstView` — a Canvas-driven particle system with
 * two modes: a continuous drifting "ambient" field (soft glow dots, used as a background layer
 * on every top-level screen) and one-shot radial [ParticleFieldState.burst]s (button presses,
 * level-complete celebrations). Soft "glow" is faked with a radial-gradient brush per dot rather
 * than a blur mask filter, so it renders identically regardless of hardware-layer support.
 */
private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    var radius: Float,
    val color: Color,
    var rotation: Float = 0f,
    var vRotation: Float = 0f,
    val isRect: Boolean = false,
    val isAmbient: Boolean = false,
    val seedPhase: Float = 0f,
    /** How many seconds [life] takes to decay from [maxLife] to 0 - a [burst]'s quick 0.7s pop
     * versus [ParticleFieldState.celebrationRain]'s much longer full-screen fall use the same
     * gravity/drag physics in [step], just stretched or compressed in time. */
    val lifeDecaySeconds: Float = 0.7f,
)

/**
 * [ambientRadiusRange], [ambientDriftScale] and [ambientAlpha] default to the field's original
 * fixed values (6f..16f, 1x drift, 22% alpha) — a second state with a smaller radius, slower
 * drift and lower alpha reads as a "farther" depth plane when layered behind the default one,
 * the cheap two-plane trick [AmbientBackground] uses for a parallax feel with no scroll/sensor
 * input required.
 */
class ParticleFieldState(
    private val ambientCount: Int = 18,
    private val ambientRadiusRange: ClosedFloatingPointRange<Float> = 6f..16f,
    private val ambientDriftScale: Float = 1f,
    private val ambientAlpha: Float = 0.22f,
) {
    internal var frameTick by mutableIntStateOf(0)
    private val particles = ArrayList<Particle>()
    private var ambientSeeded = false
    private var ambientTime = 0f

    /** Radial burst of particles from [origin] — button presses, reward celebrations. */
    fun burst(origin: Offset, colors: List<Color>, count: Int = 26, rects: Boolean = false) {
        repeat(count) {
            val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
            val speed = Random.nextFloat() * 9f + 3f
            particles.add(
                Particle(
                    x = origin.x,
                    y = origin.y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    life = 1f,
                    maxLife = 1f,
                    radius = Random.nextFloat() * 5f + 3f,
                    color = colors[Random.nextInt(colors.size)],
                    rotation = Random.nextFloat() * 360f,
                    vRotation = (Random.nextFloat() - 0.5f) * 12f,
                    isRect = rects,
                ),
            )
        }
    }

    /** A full-screen "confetti rain" - the Lucky Spin reward celebration. Unlike [burst] (a
     * radial pop from one point, gone in ~0.7s), this seeds a wide band of particles just above
     * the visible top edge, staggered in starting height so they don't all begin falling in the
     * same instant, with a much longer [Particle.lifeDecaySeconds] so the existing gravity/drag
     * physics in [step] has time to carry them visibly down and across the whole screen rather
     * than fading before they've fallen far - the "flowing down over the whole screen" effect,
     * built entirely from physics [step] already runs for every other particle. */
    fun celebrationRain(
        width: Float,
        colors: List<Color> = listOf(
            MemoryArchitectColors.accentGold,
            MemoryArchitectColors.accentTerracotta,
            MemoryArchitectColors.accentSage,
            MemoryArchitectColors.textPrimary,
        ),
        count: Int = 140,
        fallDurationSeconds: Float = 2.6f,
    ) {
        repeat(count) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = -Random.nextFloat() * 400f,
                    vx = (Random.nextFloat() - 0.5f) * 4f,
                    vy = Random.nextFloat() * 2f,
                    life = 1f,
                    maxLife = 1f,
                    radius = Random.nextFloat() * 5f + 3f,
                    color = colors[Random.nextInt(colors.size)],
                    rotation = Random.nextFloat() * 360f,
                    vRotation = (Random.nextFloat() - 0.5f) * 10f,
                    isRect = true,
                    lifeDecaySeconds = fallDurationSeconds,
                ),
            )
        }
    }

    private fun seedAmbient(size: IntSize, colors: List<Color>) {
        if (ambientSeeded || size.width == 0 || size.height == 0) return
        ambientSeeded = true
        repeat(ambientCount) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * size.width,
                    y = Random.nextFloat() * size.height,
                    vx = 0f,
                    vy = 0f,
                    life = 1f,
                    maxLife = 1f,
                    radius = Random.nextFloat() * (ambientRadiusRange.endInclusive - ambientRadiusRange.start) + ambientRadiusRange.start,
                    color = colors[Random.nextInt(colors.size)],
                    isAmbient = true,
                    seedPhase = Random.nextFloat() * (Math.PI * 2).toFloat(),
                ),
            )
        }
    }

    internal fun step(dtSeconds: Float, size: IntSize, ambient: Boolean, ambientColors: List<Color>) {
        if (ambient) seedAmbient(size, ambientColors)
        ambientTime += dtSeconds

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            if (p.isAmbient) {
                p.x += kotlin.math.cos(ambientTime * 0.3f + p.seedPhase) * 6f * ambientDriftScale * dtSeconds
                p.y += kotlin.math.sin(ambientTime * 0.24f + p.seedPhase) * 6f * ambientDriftScale * dtSeconds
                continue
            }
            p.vy += 0.35f * dtSeconds * 60f
            p.vx *= 0.985f
            p.x += p.vx * dtSeconds * 60f
            p.y += p.vy * dtSeconds * 60f
            p.rotation += p.vRotation
            p.life -= dtSeconds / p.lifeDecaySeconds
            if (p.life <= 0f) iterator.remove()
        }
        frameTick++
    }

    internal fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) {
        with(scope) {
            particles.forEach { p ->
                val alpha = if (p.isAmbient) ambientAlpha else (p.life / p.maxLife).coerceIn(0f, 1f)
                if (p.isRect) {
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                        drawRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(p.x - p.radius, p.y - p.radius * 0.6f),
                            size = Size(p.radius * 2f, p.radius * 1.2f),
                        )
                    }
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(p.color.copy(alpha = alpha), p.color.copy(alpha = 0f)),
                            center = Offset(p.x, p.y),
                            radius = p.radius * 2.2f,
                        ),
                        radius = p.radius * 2.2f,
                        center = Offset(p.x, p.y),
                    )
                }
            }
        }
    }
}

@Composable
fun rememberParticleFieldState(
    ambientCount: Int = 18,
    ambientRadiusRange: ClosedFloatingPointRange<Float> = 6f..16f,
    ambientDriftScale: Float = 1f,
    ambientAlpha: Float = 0.22f,
): ParticleFieldState =
    remember { ParticleFieldState(ambientCount, ambientRadiusRange, ambientDriftScale, ambientAlpha) }

/**
 * Renders [state]'s particles and drives its physics loop. Pass [ambient] = true for the
 * continuous background drift (with [ambientColors] seeding the dot palette); false renders
 * burst-only particles with no ambient seeding (e.g. [ConfettiBurst]).
 */
@Composable
fun ParticleField(
    state: ParticleFieldState,
    modifier: Modifier = Modifier,
    ambient: Boolean = true,
    ambientColors: List<Color> = listOf(
        MemoryArchitectColors.accentTerracotta,
        MemoryArchitectColors.accentGold,
        MemoryArchitectColors.accentSage,
    ),
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state) {
        var lastFrameTimeNanos = -1L
        while (isActive) {
            val frameTimeNanos = withFrameNanos { it }
            val dt = if (lastFrameTimeNanos < 0) {
                0f
            } else {
                ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            }
            lastFrameTimeNanos = frameTimeNanos
            state.step(dt, canvasSize, ambient, ambientColors)
        }
    }

    Canvas(modifier = modifier.onSizeChanged { canvasSize = it }) {
        @Suppress("UNUSED_EXPRESSION")
        state.frameTick
        state.draw(this)
    }
}
