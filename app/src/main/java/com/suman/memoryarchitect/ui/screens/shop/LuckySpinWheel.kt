package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.isActive

private const val SEGMENT_COUNT = 8
private const val SEGMENT_DEGREES = 360f / SEGMENT_COUNT
private const val CONTINUOUS_SPIN_DEGREES_PER_LOOP = 360f
private const val CONTINUOUS_SPIN_LOOP_MS = 650

/** One wedge of [LuckySpinWheel] - deliberately more wedges than reward *kinds* (4) so the wheel
 * reads as a real wheel-of-fortune rather than a 4-slice pie; several wedges share a kind (see
 * [WHEEL_SEGMENTS]), and [targetSegmentIndex] picks any one of the matching wedges to land on. */
private enum class WheelSegmentKind(val label: String) {
    COINS_150("150"),
    COINS_250("250"),
    JACKPOT_500("500"),
    COSMETIC("🎁"),
}

private val WHEEL_SEGMENTS = listOf(
    WheelSegmentKind.COINS_150,
    WheelSegmentKind.COSMETIC,
    WheelSegmentKind.COINS_250,
    WheelSegmentKind.COSMETIC,
    WheelSegmentKind.COINS_150,
    WheelSegmentKind.JACKPOT_500,
    WheelSegmentKind.COINS_250,
    WheelSegmentKind.COSMETIC,
)

private fun WheelSegmentKind.color(): Color = when (this) {
    WheelSegmentKind.COINS_150 -> MemoryArchitectColors.accentGold.copy(alpha = 0.7f)
    WheelSegmentKind.COINS_250 -> MemoryArchitectColors.accentGold
    WheelSegmentKind.JACKPOT_500 -> MemoryArchitectColors.accentTerracotta
    WheelSegmentKind.COSMETIC -> MemoryArchitectColors.accentSage
}

/** Picks the wheel wedge index the pointer should land on for [reward] - any wedge sharing that
 * reward's kind (there are several, see [WHEEL_SEGMENTS]) is a valid landing spot, chosen at
 * random so the same reward doesn't always land on the same physical wedge. Falls back to index 0
 * only if [reward] somehow matches nothing (unreachable given [WHEEL_SEGMENTS] covers every
 * possible [SpinRewardKind] shape [com.suman.memoryarchitect.domain.progression.SpinRules]
 * produces, but a safe default beats a crash). */
private fun targetSegmentIndex(reward: SpinRewardKind): Int {
    val matches = WHEEL_SEGMENTS.indices.filter { index ->
        when (WHEEL_SEGMENTS[index]) {
            WheelSegmentKind.COSMETIC -> reward is SpinRewardKind.Cosmetic
            WheelSegmentKind.COINS_150 -> reward is SpinRewardKind.Coins && reward.amount == 150L
            WheelSegmentKind.COINS_250 -> reward is SpinRewardKind.Coins && reward.amount == 250L
            WheelSegmentKind.JACKPOT_500 -> reward is SpinRewardKind.Coins && reward.amount == 500L
        }
    }
    return if (matches.isNotEmpty()) matches.random() else 0
}

/** Steep ease-out - fast for most of its range, a pronounced deceleration only in the final
 * stretch, the "wheel coasting to a stop" feel a linear or gentler curve doesn't give. */
private class SpinSettleEasingImpl : Easing {
    override fun transform(fraction: Float): Float {
        val inv = 1f - fraction
        return 1f - inv * inv * inv * inv
    }
}
private val SpinSettleEasing = SpinSettleEasingImpl()

/**
 * The Lucky Spin wheel itself - continuously rotates while [isSpinning] is true (however long
 * that lasts; [com.suman.memoryarchitect.feature.shop.LuckySpinViewModel.spin] guarantees a fixed
 * 5 seconds, see its own doc for why the wheel doesn't need to know that duration itself), then
 * performs one more short "settle" tween onto the wedge matching [targetReward] the instant
 * spinning stops and a result is known - [onSettled] fires when that settle animation completes,
 * which is the actual cue [LuckySpinScreen] uses to reveal the reward card/celebration, not the
 * raw [isSpinning] flip (revealing at that exact instant would show the result while the wheel
 * still visually appears to be spinning).
 *
 * [hasSpunThisSession] (internal) guards against replaying the settle animation if this composable
 * is freshly recomposed with an already-resolved [targetReward] left over from a previous visit to
 * this screen (the ViewModel survives navigation; this Composable doesn't) - the settle effect
 * only ever fires following a genuine [isSpinning] true-to-false transition observed within this
 * composition's own lifetime.
 */
@Composable
fun LuckySpinWheel(
    isSpinning: Boolean,
    targetReward: SpinRewardKind?,
    modifier: Modifier = Modifier,
    onSettled: () -> Unit = {},
) {
    val rotation = remember { Animatable(0f) }
    var hasSpunThisSession by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(isSpinning) {
        if (!isSpinning) return@LaunchedEffect
        hasSpunThisSession = true
        while (isActive) {
            rotation.animateTo(
                rotation.value + CONTINUOUS_SPIN_DEGREES_PER_LOOP,
                animationSpec = tween(CONTINUOUS_SPIN_LOOP_MS, easing = LinearEasing),
            )
        }
    }

    LaunchedEffect(isSpinning, targetReward) {
        if (isSpinning || targetReward == null || !hasSpunThisSession) return@LaunchedEffect
        val targetIndex = targetSegmentIndex(targetReward)
        val wedgeMidAngle = targetIndex * SEGMENT_DEGREES + SEGMENT_DEGREES / 2f
        val currentMod = rotation.value % 360f
        // A few extra full turns purely for flourish, plus whatever's needed so wedgeMidAngle
        // ends up under the fixed top pointer (angle 0) once rotation is applied.
        val extraSpins = 2 * 360f
        val remainder = (360f - currentMod - wedgeMidAngle + 360f) % 360f
        rotation.animateTo(rotation.value + extraSpins + remainder, animationSpec = tween(700, easing = SpinSettleEasing))
        onSettled()
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val radius = size.minDimension / 2f
            rotate(degrees = rotation.value, pivot = center) {
                WHEEL_SEGMENTS.forEachIndexed { index, kind ->
                    val startAngle = -90f + index * SEGMENT_DEGREES
                    drawArc(
                        color = kind.color(),
                        startAngle = startAngle,
                        sweepAngle = SEGMENT_DEGREES,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                    )
                    val midAngleRad = Math.toRadians((startAngle + SEGMENT_DEGREES / 2f).toDouble())
                    val labelDistance = radius * 0.66f
                    val labelLayout = textMeasurer.measure(
                        kind.label,
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MemoryArchitectColors.bgBase),
                    )
                    val labelCenter = Offset(
                        center.x + (kotlin.math.cos(midAngleRad) * labelDistance).toFloat(),
                        center.y + (kotlin.math.sin(midAngleRad) * labelDistance).toFloat(),
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(labelCenter.x - labelLayout.size.width / 2f, labelCenter.y - labelLayout.size.height / 2f),
                    )
                }
                drawCircle(color = MemoryArchitectColors.bgBase, radius = radius * 0.14f, center = center)
            }
        }
        // Fixed pointer, never rotates with the wheel - a small downward-pointing triangle at 12
        // o'clock marking exactly which wedge currently reads as "selected."
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val radius = size.minDimension / 2f
            val pointerPath = Path().apply {
                moveTo(center.x - 14.dp.toPx(), 0f)
                lineTo(center.x + 14.dp.toPx(), 0f)
                lineTo(center.x, radius * 0.16f)
                close()
            }
            drawPath(pointerPath, color = MemoryArchitectColors.textPrimary)
        }
    }
}
