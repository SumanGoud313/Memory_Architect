package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog

private const val PREMIUM_BORDER_CYCLE_MS = 8000

/**
 * The one shared rendering entry point behind the app-wide "Premium Border" system - every
 * primary button/surface (`GlassCard`, `Buttons.kt`'s `PrimaryButton`/`OutlineButton`,
 * `IconGlassButton`, `AccountSection`'s identity card) calls this instead of drawing its own
 * border logic. Reads [LocalEquippedCosmetics] directly - true no-op (`this` returned unchanged)
 * whenever nothing's equipped for [category], guaranteeing every caller's default/unequipped look
 * stays pixel-identical to before this system existed.
 *
 * Static borders (Common/Rare rarity) render a fixed [Brush.sweepGradient] stroke - the same
 * technique already used by `AvatarFrameRing`/`TimerStyleRing`/`AvatarFrameGlow` elsewhere in this
 * app. Animated borders (Epic/Legendary, [com.suman.memoryarchitect.domain.model.CosmeticDefinition.isAnimated])
 * animate a diagonal linear-gradient sweep across the *fixed* stroke geometry - deliberately not a
 * rotating canvas, which would visibly spin a rounded-rect/pill's corners (reads as "the button is
 * spinning," not "a shimmering border"). [TileMode.Mirror] keeps the sweep fully colored at every
 * phase, never showing a hard gradient edge. The phase is only ever read inside the draw-phase
 * `drawWithContent` lambda, so the animation never triggers recomposition of the button itself -
 * same discipline `HeroPlayButton`/`ParticleField` already document elsewhere in this app.
 */
@Composable
fun Modifier.premiumBorder(
    shape: Shape,
    category: CosmeticCategory = CosmeticCategory.PROFILE_BORDER,
    strokeWidthDp: Dp = 2.dp,
): Modifier {
    val equippedId = LocalEquippedCosmetics.current[category] ?: return this
    val definition = remember(equippedId) { AllCosmeticsCatalog.definitionOrNull(equippedId) } ?: return this
    val spec = remember(equippedId) { CosmeticVisualCatalog.get(equippedId) }

    return if (definition.isAnimated) {
        this.animatedGradientBorder(shape, spec.gradientColors, strokeWidthDp)
    } else {
        this.border(strokeWidthDp, Brush.sweepGradient(spec.gradientColors), shape)
    }
}

/** The animated half of [premiumBorder]'s rendering, factored out so a preview surface (see
 * `CosmeticPreviewDialog`) can show an animated-tier item's actual motion by feeding it that
 * item's own colors directly - independent of whatever is (or isn't) actually equipped right now. */
@Composable
fun Modifier.animatedGradientBorder(shape: Shape, colors: List<Color>, strokeWidthDp: Dp = 2.dp): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "premiumBorder")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PREMIUM_BORDER_CYCLE_MS, easing = LinearEasing)),
        label = "premiumBorderPhase",
    )
    return this.drawWithContent {
        drawContent()
        val travel = size.width + size.height
        val offset = phase * travel
        val outlinePath = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }
        drawPath(
            path = outlinePath,
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(offset - size.height, 0f),
                end = Offset(offset, size.height),
                tileMode = TileMode.Mirror,
            ),
            style = Stroke(width = strokeWidthDp.toPx()),
        )
    }
}

/** Circular sibling for [IconGlassButton] - deliberately always static (see scope decision: a fast
 * shimmer on a small 56dp corner icon reads as visual noise disproportionate to its size), with a
 * thinner default stroke than [premiumBorder]'s so it doesn't look cramped. */
@Composable
fun Modifier.premiumBorderRing(
    category: CosmeticCategory = CosmeticCategory.PROFILE_BORDER,
    strokeWidthDp: Dp = 1.5.dp,
): Modifier {
    val equippedId = LocalEquippedCosmetics.current[category] ?: return this
    val spec = remember(equippedId) { CosmeticVisualCatalog.get(equippedId) }
    return this.border(strokeWidthDp, Brush.sweepGradient(spec.gradientColors), CircleShape)
}
