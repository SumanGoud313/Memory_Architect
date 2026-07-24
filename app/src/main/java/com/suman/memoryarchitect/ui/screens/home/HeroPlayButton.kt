package com.suman.memoryarchitect.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics
import com.suman.memoryarchitect.ui.components.ParticleField
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PARTICLE_COUNT = 12
private const val ROTATION_PERIOD_MS = 90_000
private const val BREATHING_PERIOD_MS = 4200
private const val IDLE_FLOAT_PERIOD_MS = 3600
private const val PULSE_INTERVAL_MS = 4000L
private const val RIPPLE_DURATION_MS = 650

private data class HeroParticle(
    val baseAngleDeg: Float,
    val orbitRadiusFraction: Float,
    val speedFactor: Float,
    val twinkleFactor: Float,
    val phaseOffset: Float,
    val sizeFraction: Float,
)

private fun randomHeroParticles(count: Int): List<HeroParticle> = List(count) {
    HeroParticle(
        baseAngleDeg = Random.nextFloat() * 360f,
        orbitRadiusFraction = 0.62f + Random.nextFloat() * 0.34f,
        speedFactor = 0.4f + Random.nextFloat() * 0.5f,
        twinkleFactor = 1.5f + Random.nextFloat() * 2f,
        phaseOffset = Random.nextFloat() * (2 * PI).toFloat(),
        sizeFraction = 0.5f + Random.nextFloat() * 0.7f,
    )
}

/**
 * The Home screen's signature interaction — a single tappable, richly animated circle that
 * replaces what used to be a decorative orb sitting above a separate rectangular Play button.
 * Every continuous animation (rotation/breathing/idle drift) runs on one shared
 * [rememberInfiniteTransition] clock; the discrete "pulse" every [PULSE_INTERVAL_MS] is a single
 * extra [Animatable]; press feedback layers on top via [collectIsPressedAsState]. None of it
 * triggers recomposition of the surrounding screen — every animated value is only ever read
 * inside a `graphicsLayer{}`/`Canvas{}` draw-phase lambda, the same pattern [com.suman.memoryarchitect.ui.screens.profile.LevelProgressRing]
 * and [com.suman.memoryarchitect.ui.components.ParticleField] already use elsewhere in this app.
 */
@Composable
fun HeroPlayButton(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp,
    onPressStart: () -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tick = rememberHapticsTick()
    // Recolors the existing rotating ring below when a Premium Border is equipped - deliberately
    // not a second ring layered on top (this button already has an ambient glow, orbiting
    // particles, and this ring; a second independently-animated ring would compete for attention
    // rather than read as "a shimmering border").
    val equippedBorderId = LocalEquippedCosmetics.current[CosmeticCategory.PROFILE_BORDER]
    val ringColors = equippedBorderId?.let { CosmeticVisualCatalog.get(it).gradientColors }
        ?: listOf(
            MemoryArchitectColors.accentGold.copy(alpha = 0.15f),
            MemoryArchitectColors.accentGold,
            MemoryArchitectColors.accentTerracotta,
            MemoryArchitectColors.accentGold.copy(alpha = 0.15f),
        )
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val burstParticles = rememberParticleFieldState(ambientCount = 0)
    val heroParticles = remember { randomHeroParticles(PARTICLE_COUNT) }

    val infiniteTransition = rememberInfiniteTransition(label = "heroPlayButton")
    val rotationProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ROTATION_PERIOD_MS, easing = LinearEasing)),
        label = "heroRotation",
    )
    val breathing by infiniteTransition.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(BREATHING_PERIOD_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroBreathing",
    )
    val idleFloat by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(IDLE_FLOAT_PERIOD_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroIdleFloat",
    )

    // A discrete "breath skips a beat" pulse every few seconds — distinct from the continuous
    // breathing above, layered additively on top of it.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(PULSE_INTERVAL_MS)
            pulse.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            pulse.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
        }
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "heroPressScale",
    )
    val pressGlow by animateFloatAsState(if (isPressed) 1f else 0f, tween(150), label = "heroPressGlow")
    val pressRotationBoost by animateFloatAsState(if (isPressed) 1f else 0f, tween(500), label = "heroPressRotationBoost")
    val pressIconScale by animateFloatAsState(
        targetValue = if (isPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heroPressIconScale",
    )
    val ripple = remember { Animatable(0f) }
    // Boosts scale further during the brief post-tap window before navigation fires — the
    // circle presses in on touch-down, then expands past 100% right as the tap is confirmed,
    // reading as "growing into" the next screen rather than a flat instant cut.
    val exitScale = remember { Animatable(1f) }

    val outerBoundsDp = sizeDp * 1.7f

    Box(modifier = modifier.size(outerBoundsDp), contentAlignment = Alignment.Center) {
        // Outer ambient glow — soft, breathing, brightens further on press.
        Box(
            modifier = Modifier
                .size(sizeDp * 1.4f)
                .graphicsLayer {
                    val s = breathing * pressScale * exitScale.value
                    scaleX = s
                    scaleY = s
                    alpha = (0.28f + pulse.value * 0.18f + pressGlow * 0.3f).coerceIn(0f, 1f)
                    translationY = idleFloat * 2.dp.toPx()
                }
                .blur(48.dp)
                .background(brush = Brush.radialGradient(listOf(MemoryArchitectColors.accentGold, Color.Transparent)), shape = CircleShape),
        )

        // Slow-orbiting firefly particles, drawn once per shared animation frame — no per-particle
        // composables or state objects, just polar-coordinate math inside a single Canvas.
        Canvas(modifier = Modifier.size(sizeDp * 1.55f)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = size.minDimension / 2f
            heroParticles.forEach { particle ->
                val angleRad = (particle.baseAngleDeg + rotationProgress * 360f * particle.speedFactor) * (PI / 180.0).toFloat()
                val wobble = sin(rotationProgress * (2 * PI).toFloat() * 0.6f + particle.phaseOffset) * baseRadius * 0.04f
                val orbitRadius = baseRadius * particle.orbitRadiusFraction + wobble
                val cx = centerX + cos(angleRad) * orbitRadius
                val cy = centerY + sin(angleRad) * orbitRadius
                val twinkle = 0.4f + 0.6f * ((sin(rotationProgress * (2 * PI).toFloat() * particle.twinkleFactor + particle.phaseOffset) + 1f) / 2f)
                val dotRadius = 2.dp.toPx() * particle.sizeFraction
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MemoryArchitectColors.accentGold.copy(alpha = twinkle * 0.9f), MemoryArchitectColors.accentGold.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = dotRadius * 3f,
                    ),
                    radius = dotRadius * 3f,
                    center = Offset(cx, cy),
                )
            }
        }

        // The tap-burst layer — invisible until burstParticles.burst(...) is called on tap.
        ParticleField(state = burstParticles, ambient = false, modifier = Modifier.size(sizeDp * 1.5f))

        // The actual button — sized and positioned to match what reads visually as "the circle",
        // deliberately smaller than the decorative halo above so the touch target lines up with
        // what a player would expect to tap (still far above the 64dp accessibility minimum).
        Box(
            modifier = Modifier
                .size(sizeDp)
                .graphicsLayer {
                    val s = breathing * pressScale * exitScale.value
                    scaleX = s
                    scaleY = s
                    translationY = idleFloat * 2.dp.toPx()
                }
                .clearAndSetSemantics {
                    contentDescription = context.getString(R.string.home_play_content_description)
                    role = Role.Button
                }
                .clickable(interactionSource = interactionSource, indication = null) {
                    tick()
                    onPressStart()
                    scope.launch {
                        launch {
                            ripple.snapTo(0f)
                            ripple.animateTo(1f, tween(RIPPLE_DURATION_MS, easing = FastOutSlowInEasing))
                        }
                        launch {
                            exitScale.animateTo(1.1f, tween(RIPPLE_DURATION_MS, easing = FastOutSlowInEasing))
                        }
                        val burstFieldCenterPx = with(density) { (sizeDp * 1.5f / 2).toPx() }
                        burstParticles.burst(
                            origin = Offset(burstFieldCenterPx, burstFieldCenterPx),
                            colors = listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.textPrimary),
                            count = 18,
                        )
                        delay(260)
                        onPlay()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // Inner glow, tighter and brighter than the outer halo — sits just behind the glass face.
            Box(
                modifier = Modifier
                    .size(sizeDp * 0.95f)
                    .blur(20.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(MemoryArchitectColors.accentGold.copy(alpha = 0.5f), Color.Transparent)),
                        shape = CircleShape,
                    ),
            )

            // The rotating gradient ring — the "glassmorphism frame."
            Canvas(modifier = Modifier.size(sizeDp)) {
                val strokeWidth = size.minDimension * 0.045f
                val inset = strokeWidth / 2f
                val ringSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                rotate(degrees = rotationProgress * 360f + pressRotationBoost * 50f) {
                    drawArc(
                        brush = Brush.sweepGradient(ringColors),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = ringSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            // The glass button face — reuses GlassCard (already the app's one glassmorphism
            // surface) so this reads as the same material as every other card, not a bespoke look.
            GlassCard(
                modifier = Modifier.size(sizeDp * 0.86f),
                shape = CircleShape,
                tint = MemoryArchitectColors.accentGold.copy(alpha = 0.18f),
            ) {
                // Soft reflection — a low-alpha highlight offset toward the upper-left, like light
                // glancing off glass.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(18.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(MemoryArchitectColors.textPrimary.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(0.32f, 0.28f),
                            ),
                            shape = CircleShape,
                        ),
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MemoryArchitectColors.textPrimary,
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { scaleX = pressIconScale; scaleY = pressIconScale },
                    )
                    Text(
                        text = stringResource(R.string.home_play).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MemoryArchitectColors.accentGold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_play_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // Expanding tap ripple — always composed, only visible for RIPPLE_DURATION_MS after
            // a tap, so reading ripple.value here never causes recomposition (draw-phase only).
            Canvas(modifier = Modifier.size(sizeDp)) {
                val progress = ripple.value
                if (progress > 0f) {
                    drawCircle(
                        color = MemoryArchitectColors.accentGold.copy(alpha = (1f - progress) * 0.5f),
                        radius = size.minDimension / 2f * (0.5f + progress * 0.6f),
                        style = Stroke(width = 3.dp.toPx() * (1f - progress * 0.5f)),
                    )
                }
            }
        }
    }
}
