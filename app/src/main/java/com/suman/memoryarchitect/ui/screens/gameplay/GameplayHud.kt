package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.ui.components.CircularCountdownTimer
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Phase label pill + circular countdown — the countdown now renders identically (same ring,
 * same color grading, same pulse/haptic urgency in the final 5 seconds) during both Memorize and
 * Rebuild, instead of a plain text pill that only really read as "present" during Memorize.
 * [totalMs] is phase's full duration (needed for the ring's fill fraction); both it and
 * [remainingMs] are null together whenever the phase has no timer at all (e.g. Practice's untimed
 * Rebuild) — that case is unaffected, on purpose, since there's nothing to fabricate a countdown
 * against. [showRotateCaption] adds a small line under the pill, purely data-driven (whether
 * *this* generated level actually contains a rotated object) rather than tied to any level number
 * or mode - a level that happens to have no rotation never shows it, and one that does always
 * does, regardless of where it falls in the campaign. */
@Composable
fun GameplayHud(
    phaseLabel: String,
    remainingMs: Long?,
    totalMs: Long?,
    showRotateCaption: Boolean,
    equippedTimerStyleId: CosmeticId? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // weight(fill = false) reserves the timer's space first (it's the non-weighted
            // sibling) and only lets the label grow into whatever's left — a long phase label
            // wraps within its share instead of crowding the timer out toward (or past) the
            // screen edge.
            PillBadge(text = phaseLabel, modifier = Modifier.weight(1f, fill = false))
            AnimatedVisibility(visible = remainingMs != null && totalMs != null && totalMs > 0) {
                if (remainingMs != null && totalMs != null && totalMs > 0) {
                    // The decorative ring (if a Timer Style is equipped) is a separate Canvas layer
                    // drawn behind the timer, not a modification to CircularCountdownTimer itself -
                    // its own color-graded urgency arc (green/yellow/orange/red, the pulse/haptic
                    // threshold logic) is untouched, on purpose: that's real gameplay feedback, not
                    // a skin.
                    Box(contentAlignment = Alignment.Center) {
                        if (equippedTimerStyleId != null) {
                            TimerStyleRing(styleId = equippedTimerStyleId)
                        }
                        CircularCountdownTimer(
                            remainingMs = remainingMs,
                            totalMs = totalMs,
                            pulseOnUrgent = true,
                            vibrateOnUrgent = true,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = showRotateCaption) {
            Text(
                text = stringResource(R.string.gameplay_rotate_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Decorative ring drawn behind [CircularCountdownTimer] when a Timer Style cosmetic is equipped -
 * same technique as Profile's `AvatarFrameRing`. Sized a bit larger than the timer's own 52dp
 * default so it frames it without overlapping the timer's own stroke. */
@Composable
private fun TimerStyleRing(styleId: CosmeticId, modifier: Modifier = Modifier) {
    val spec = CosmeticVisualCatalog.get(styleId)
    Canvas(modifier = modifier.size(66.dp)) {
        val strokeWidth = size.minDimension * 0.07f
        val inset = strokeWidth / 2f
        drawArc(
            brush = Brush.sweepGradient(spec.gradientColors),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
