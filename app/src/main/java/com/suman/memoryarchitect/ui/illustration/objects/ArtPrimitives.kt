package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.random.Random

/**
 * Shared drawing primitives for object illustrations — every object across every room (see the
 * sibling `*Objects.kt` files) composes these instead of hand-rolling gradients/shadows per
 * object, so the "soft lighting, gradients, depth" look stays consistent across ~70 objects.
 * DrawScope's (0,0)-(1,1) is remapped by callers to the actual composable size.
 *
 * Light-direction convention used everywhere in this package: the key light comes from the
 * top-left, so [highlight]/[rimLight] sit near an object's upper-left surfaces and shadows
 * ([groundShadow]/[contactShadow]) sit at the bottom. Keeping this consistent across every
 * object is what makes the whole tray read as one coherently-lit scene.
 */

/** Soft grounding shadow ellipse beneath an object, anchored to the bottom of the canvas. */
fun DrawScope.groundShadow(
    widthFraction: Float = 0.62f,
    heightFraction: Float = 0.14f,
    centerXFraction: Float = 0.5f,
    bottomInsetFraction: Float = 0.04f,
    alpha: Float = 0.28f,
) {
    val w = size.width * widthFraction
    val h = size.height * heightFraction
    val cx = size.width * centerXFraction
    val cy = size.height * (1f - bottomInsetFraction) - h / 2f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = alpha), Color.Black.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = w / 2f,
        ),
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(w, h),
    )
}

/**
 * A two-layer grounding shadow — the existing soft wide [groundShadow] falloff plus a tight,
 * darker core shadow right at the actual contact point — so objects read as resting with real
 * weight rather than floating over a uniform blur puddle. Drop-in replacement for bare
 * `groundShadow()` calls; same parameters, same call site.
 */
fun DrawScope.contactShadow(
    widthFraction: Float = 0.62f,
    heightFraction: Float = 0.14f,
    centerXFraction: Float = 0.5f,
    bottomInsetFraction: Float = 0.04f,
    alpha: Float = 0.28f,
) {
    groundShadow(widthFraction, heightFraction, centerXFraction, bottomInsetFraction, alpha)
    val coreW = size.width * widthFraction * 0.4f
    val coreH = size.height * heightFraction * 0.5f
    val cx = size.width * centerXFraction
    val cy = size.height * (1f - bottomInsetFraction) - coreH * 0.4f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = (alpha * 1.7f).coerceAtMost(0.55f)), Color.Black.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = coreW / 2f,
        ),
        topLeft = Offset(cx - coreW / 2f, cy - coreH / 2f),
        size = Size(coreW, coreH),
    )
}

/** A vertical gradient fill from [light] (top) to [dark] (bottom) — the default "body" fill. */
fun verticalBodyBrush(light: Color, dark: Color): Brush = Brush.verticalGradient(listOf(light, dark))

/** A richer 3-stop vertical gradient for larger bodies — more depth than the plain 2-stop version. */
fun verticalBodyBrush3(top: Color, mid: Color, bottom: Color): Brush = Brush.verticalGradient(listOf(top, mid, bottom))

/** A small soft highlight ellipse, e.g. near an object's top-left, suggesting a light source. */
fun DrawScope.highlight(
    topLeft: Offset,
    size: Size,
    color: Color = Color.White,
    alpha: Float = 0.35f,
) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f),
            radius = maxOf(size.width, size.height) / 2f,
        ),
        topLeft = topLeft,
        size = size,
    )
}

/**
 * A thin, fading stroke tracing an object's top and left edges — the "key light catching the
 * edge" that separates a lit illustration from a flat gradient icon. Follows the rounded-rect
 * silhouette from partway across the top edge, around the top-left corner, down partway along
 * the left edge, fading to transparent at both ends.
 */
fun DrawScope.rimLight(
    topLeft: Offset,
    size: Size,
    cornerRadius: Float = size.minDimension * 0.2f,
    color: Color = Color.White,
    alpha: Float = 0.5f,
    strokeWidth: Float = size.minDimension * 0.05f,
) {
    val inset = strokeWidth / 2f
    val topStart = Offset(topLeft.x + size.width * 0.6f, topLeft.y + inset)
    val cornerEnd = Offset(topLeft.x + inset, topLeft.y + size.height * 0.55f)
    val path = Path().apply {
        moveTo(topStart.x, topStart.y)
        lineTo(topLeft.x + cornerRadius, topLeft.y + inset)
        quadraticTo(topLeft.x + inset, topLeft.y + inset, topLeft.x + inset, topLeft.y + cornerRadius)
        lineTo(cornerEnd.x, cornerEnd.y)
    }
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(color.copy(alpha = 0f), color.copy(alpha = alpha), color.copy(alpha = 0f)),
            start = topStart,
            end = cornerEnd,
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

/** [rimLight]'s equivalent for circular bodies — a partial bright arc over the upper-left quarter. */
fun DrawScope.rimLightCircle(
    center: Offset,
    radius: Float,
    color: Color = Color.White,
    alpha: Float = 0.55f,
    strokeWidth: Float = radius * 0.12f,
) {
    drawArc(
        color = color.copy(alpha = alpha * 0.5f),
        startAngle = 195f,
        sweepAngle = 95f,
        useCenter = false,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    drawArc(
        color = color.copy(alpha = alpha),
        startAngle = 210f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth * 0.6f, cap = StrokeCap.Round),
    )
}

fun DrawScope.roundedBody(
    topLeft: Offset,
    size: Size,
    light: Color,
    dark: Color,
    cornerRadius: Float = size.minDimension * 0.22f,
) {
    drawRoundRect(
        brush = verticalBodyBrush(light, dark),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Fill,
    )
}

fun DrawScope.circleBody(center: Offset, radius: Float, light: Color, dark: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(light, dark),
            center = center - Offset(radius * 0.3f, radius * 0.3f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
}

/** A handful of thin low-alpha horizontal strokes suggesting wood grain across [topLeft]/[size]. */
fun DrawScope.woodGrainLines(
    topLeft: Offset,
    size: Size,
    color: Color = Color.Black,
    alpha: Float = 0.08f,
    lineCount: Int = 3,
) {
    val step = size.height / (lineCount + 1)
    for (i in 1..lineCount) {
        val y = topLeft.y + step * i
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(topLeft.x + size.width * 0.06f, y),
            end = Offset(topLeft.x + size.width * 0.94f, y),
            strokeWidth = size.height * 0.02f,
            cap = StrokeCap.Round,
        )
    }
}

/** A few diagonal low-alpha dashes suggesting woven fabric across [topLeft]/[size]. */
fun DrawScope.fabricWeave(
    topLeft: Offset,
    size: Size,
    color: Color = Color.Black,
    alpha: Float = 0.07f,
    lineCount: Int = 4,
) {
    val spacing = size.width / (lineCount + 1)
    for (i in 1..lineCount) {
        val x = topLeft.x + spacing * i
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(x, topLeft.y + size.height * 0.1f),
            end = Offset(x - size.height * 0.22f, topLeft.y + size.height * 0.9f),
            strokeWidth = size.width * 0.012f,
            cap = StrokeCap.Round,
        )
    }
}

/** A deterministic scatter of tiny dots suggesting ceramic/stone speckle. [seed] keeps it stable across recompositions. */
fun DrawScope.speckle(
    topLeft: Offset,
    size: Size,
    color: Color = Color.Black,
    alpha: Float = 0.12f,
    count: Int = 7,
    seed: Int = 1,
) {
    val rnd = Random(seed)
    repeat(count) {
        val x = topLeft.x + rnd.nextFloat() * size.width
        val y = topLeft.y + rnd.nextFloat() * size.height
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension * 0.025f, center = Offset(x, y))
    }
}

/** A very faint deterministic dot scatter suggesting paper/cardboard grain. [seed] keeps it stable across recompositions. */
fun DrawScope.matteNoise(
    topLeft: Offset,
    size: Size,
    color: Color = Color.Black,
    alpha: Float = 0.05f,
    count: Int = 14,
    seed: Int = 2,
) {
    val rnd = Random(seed)
    repeat(count) {
        val x = topLeft.x + rnd.nextFloat() * size.width
        val y = topLeft.y + rnd.nextFloat() * size.height
        drawCircle(color = color.copy(alpha = alpha * (0.5f + rnd.nextFloat() * 0.5f)), radius = size.minDimension * 0.012f, center = Offset(x, y))
    }
}
