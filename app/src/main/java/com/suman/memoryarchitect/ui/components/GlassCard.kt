package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/**
 * Warm-tinted translucent "glass" surface — replaces the old `bg_glass_card`/`bg_pill_glass`
 * 9-patch drawables. A soft vertical gradient fill plus a 1dp bright-on-top stroke. Pass [tint]
 * to wash a low-alpha color underneath the glass (e.g. giving each Mode Select card its own
 * accent identity) — `null` (the default) preserves the neutral look every existing call site
 * already relies on.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MemoryArchitectRadii.card),
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .then(
                if (tint != null) {
                    Modifier.background(brush = Brush.verticalGradient(listOf(tint, Color.Transparent)), shape = shape)
                } else {
                    Modifier
                },
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MemoryArchitectColors.glassFillPressed, MemoryArchitectColors.glassFill),
                ),
                shape = shape,
            )
            .border(width = 1.dp, color = MemoryArchitectColors.glassStroke, shape = shape)
            // An extra, outer Premium Border layered on top of the glass stroke above when a
            // border cosmetic is equipped - a true no-op otherwise (see premiumBorder's doc). This
            // one change is what makes every GlassCard-based surface in the app (ModeCard/LevelCell/
            // HeroPlayButton's face) inherit the equipped border automatically, with zero changes
            // to those files themselves.
            .premiumBorder(shape = shape),
    ) {
        content()
    }
}
