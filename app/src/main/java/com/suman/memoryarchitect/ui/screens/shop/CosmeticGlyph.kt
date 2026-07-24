package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Shared cosmetic-item visual, used by Shop/Collections/Profile alike - one renderer per
 * [CosmeticCategory] shape (ring stroke for frames/borders/timers, solid swatch for name colors,
 * radial burst for victory/confetti, icon-in-circle for stickers/trophies), all built from
 * [CosmeticVisualCatalog]'s pure color/icon data - no art assets. */
@Composable
fun CosmeticGlyph(
    id: CosmeticId,
    category: CosmeticCategory,
    isOwned: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp,
) {
    if (!isOwned) {
        Box(
            modifier = modifier.size(sizeDp).background(MemoryArchitectColors.glassFill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = MemoryArchitectColors.textTertiary)
        }
        return
    }

    val spec = CosmeticVisualCatalog.get(id)
    when (category) {
        CosmeticCategory.AVATAR_FRAME, CosmeticCategory.PROFILE_BORDER, CosmeticCategory.TIMER_STYLE -> {
            Canvas(modifier = modifier.size(sizeDp)) {
                val strokeWidth = size.minDimension * 0.14f
                drawArc(
                    brush = Brush.sweepGradient(spec.gradientColors),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                )
            }
        }
        CosmeticCategory.NAME_COLOR -> {
            Box(
                modifier = modifier
                    .size(sizeDp)
                    .background(Brush.linearGradient(spec.gradientColors), CircleShape),
            )
        }
        CosmeticCategory.VICTORY_ANIMATION, CosmeticCategory.CONFETTI_EFFECT, CosmeticCategory.BACKGROUND_THEME -> {
            Box(
                modifier = modifier.size(sizeDp).background(Brush.radialGradient(spec.gradientColors), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = spec.icon, contentDescription = null, tint = MemoryArchitectColors.bgBase, modifier = Modifier.size(sizeDp * 0.5f))
            }
        }
        CosmeticCategory.STICKER_PACK, CosmeticCategory.TROPHY_RELIC, CosmeticCategory.PROFILE_BADGE -> {
            Box(
                modifier = modifier.size(sizeDp).background(Brush.verticalGradient(spec.gradientColors), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = spec.icon, contentDescription = null, tint = MemoryArchitectColors.bgBase, modifier = Modifier.size(sizeDp * 0.55f))
            }
        }
    }
}
