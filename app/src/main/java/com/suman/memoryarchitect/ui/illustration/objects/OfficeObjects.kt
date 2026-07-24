package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.IdleAnimationKind
import com.suman.memoryarchitect.ui.illustration.ObjectArt
import com.suman.memoryarchitect.ui.illustration.register

/** Office's own objects — desk_lamp glows softly; the rest are plain, businesslike shapes to
 * contrast the gaming room's saturated, neon-lit objects. */
fun MutableMap<String, ObjectArt>.registerOfficeObjects() {
    register(ObjectArt("desk_lamp", R.string.object_desk_lamp, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> DeskLampArt(m) }))
    register(ObjectArt("pen_cup", R.string.object_pen_cup, drawer = { m -> PenCupArt(m) }))
    register(ObjectArt("stapler", R.string.object_stapler, drawer = { m -> StaplerArt(m) }))
    register(ObjectArt("binder", R.string.object_binder, drawer = { m -> BinderArt(m) }))
    register(ObjectArt("award_plaque", R.string.object_award_plaque, drawer = { m -> AwardPlaqueArt(m) }))
    register(ObjectArt("wall_clock", R.string.object_wall_clock, drawer = { m -> WallClockArt(m) }))
    register(ObjectArt("calendar", R.string.object_calendar, drawer = { m -> CalendarArt(m) }))
    register(ObjectArt("office_chair", R.string.object_office_chair, drawer = { m -> OfficeChairArt(m) }))
    register(ObjectArt("waste_bin", R.string.object_waste_bin, drawer = { m -> WasteBinArt(m) }))
}

@Composable
fun DeskLampArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val baseCenter = Offset(size.width * 0.5f, size.height * 0.78f)
        drawOval(brush = verticalBodyBrush(Color(0xFF827A6F), Color(0xFF463B31)), topLeft = baseCenter - Offset(size.width * 0.18f, size.height * 0.03f), size = Size(size.width * 0.36f, size.height * 0.08f))
        val elbow = baseCenter - Offset(0f, size.height * 0.24f)
        val headCenter = elbow - Offset(size.width * 0.2f, size.height * 0.18f)
        drawLine(Color(0xFF827A6F), baseCenter, elbow, strokeWidth = size.width * 0.045f, cap = StrokeCap.Round)
        drawLine(Color(0xFF827A6F), elbow, headCenter, strokeWidth = size.width * 0.04f, cap = StrokeCap.Round)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFE9A8).copy(alpha = 0.7f), Color.Transparent), center = headCenter, radius = size.width * 0.32f), radius = size.width * 0.32f, center = headCenter)
        circleBody(headCenter, size.width * 0.14f, Color(0xFFF0C875), Color(0xFFB88936))
        rimLightCircle(headCenter, size.width * 0.14f, alpha = 0.5f)
    }
}

@Composable
fun PenCupArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.36f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.5f
        val tl = Offset(left, top)
        val sz = Size(w, size.height * 0.26f)
        roundedBody(tl, sz, Color(0xFF7CB4FA), Color(0xFF3F5A8A), cornerRadius = w * 0.08f)
        rimLight(tl, sz, cornerRadius = w * 0.08f, alpha = 0.5f)
        listOf(-0.5f to Color(0xFFE8674B), -0.1f to Color(0xFF9CB48D), 0.35f to Color(0xFFF0C875)).forEach { (dx, c) ->
            val bx = left + w * 0.5f + dx * w
            drawLine(c, Offset(bx, top + sz.height * 0.1f), Offset(bx + w * 0.06f, top - size.height * 0.22f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun StaplerArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.56f
        val left = size.width * 0.5f - w / 2f
        val baseTop = size.height * 0.6f
        roundedBody(Offset(left, baseTop), Size(w, size.height * 0.1f), Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.06f)
        val path = Path().apply {
            moveTo(left + w * 0.06f, baseTop)
            lineTo(left + w * 0.14f, baseTop - size.height * 0.2f)
            lineTo(left + w * 0.92f, baseTop - size.height * 0.14f)
            lineTo(left + w * 0.94f, baseTop)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFE8674B), Color(0xFFA83E28)), style = Fill)
        drawLine(Color.White.copy(alpha = 0.35f), Offset(left + w * 0.2f, baseTop - size.height * 0.17f), Offset(left + w * 0.7f, baseTop - size.height * 0.13f), strokeWidth = 2f)
    }
}

@Composable
fun BinderArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.5f
        val h = size.height * 0.56f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.66f - h
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFF5C7A9C), Color(0xFF33465C), cornerRadius = w * 0.06f)
        drawRect(color = Color(0xFF33465C), topLeft = tl, size = Size(w * 0.14f, h))
        drawRect(color = Color(0xFFF7F1E8).copy(alpha = 0.9f), topLeft = Offset(left + w * 0.28f, top + h * 0.14f), size = Size(w * 0.5f, h * 0.24f))
        rimLight(tl, sz, cornerRadius = w * 0.06f, alpha = 0.4f)
    }
}

@Composable
fun AwardPlaqueArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.54f
        val h = size.height * 0.4f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.32f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFF6B5238), Color(0xFF3F2E1E), cornerRadius = w * 0.05f)
        roundedBody(Offset(left + w * 0.08f, top + h * 0.14f), Size(w * 0.84f, h * 0.5f), Color(0xFFFFE9A8), Color(0xFFC79A46), cornerRadius = w * 0.03f)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFE9A8), Color(0xFFB88936)), center = Offset(left + w * 0.5f, top + h * 0.36f)), radius = h * 0.16f, center = Offset(left + w * 0.5f, top + h * 0.36f))
        rimLight(tl, sz, cornerRadius = w * 0.05f, alpha = 0.4f)
    }
}

@Composable
fun WallClockArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.3f
        val center = Offset(size.width * 0.5f, size.height * 0.44f)
        circleBody(center, r, Color(0xFFF7F1E8), Color(0xFFC9BFAF))
        rimLightCircle(center, r, alpha = 0.5f)
        drawCircle(color = Color(0xFF463B31), radius = r * 0.94f, center = center, style = Stroke(width = r * 0.06f))
        listOf(0, 90, 180, 270).forEach { deg ->
            val angle = Math.toRadians(deg.toDouble() - 90)
            val outer = center + Offset((kotlin.math.cos(angle) * r * 0.82f).toFloat(), (kotlin.math.sin(angle) * r * 0.82f).toFloat())
            val inner = center + Offset((kotlin.math.cos(angle) * r * 0.68f).toFloat(), (kotlin.math.sin(angle) * r * 0.68f).toFloat())
            drawLine(Color(0xFF463B31), inner, outer, strokeWidth = r * 0.06f, cap = StrokeCap.Round)
        }
        drawLine(Color(0xFF463B31), center, center + Offset(-r * 0.1f, -r * 0.42f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
        drawLine(Color(0xFFE8674B), center, center + Offset(r * 0.36f, r * 0.14f), strokeWidth = r * 0.05f, cap = StrokeCap.Round)
        drawCircle(color = Color(0xFF463B31), radius = r * 0.06f, center = center)
    }
}

@Composable
fun CalendarArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.56f
        val h = size.height * 0.48f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.26f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFFF7F1E8), Color(0xFFDCD2BE), cornerRadius = w * 0.04f)
        drawRect(color = Color(0xFFE8674B), topLeft = tl, size = Size(w, h * 0.18f))
        for (r in 0..2) {
            for (c in 0..3) {
                drawRect(
                    color = Color(0xFF8B6A4A).copy(alpha = 0.35f),
                    topLeft = Offset(left + w * (0.1f + c * 0.22f), top + h * (0.32f + r * 0.2f)),
                    size = Size(w * 0.14f, h * 0.1f),
                )
            }
        }
        listOf(0.3f, 0.7f).forEach { dx ->
            drawLine(Color(0xFF463B31), Offset(left + w * dx, top - h * 0.06f), Offset(left + w * dx, top + h * 0.08f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        }
        rimLight(tl, sz, cornerRadius = w * 0.04f, alpha = 0.35f)
    }
}

@Composable
fun OfficeChairArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.4f
        val left = size.width * 0.5f - w / 2f
        val backTL = Offset(left, size.height * 0.18f)
        val backSize = Size(w, size.height * 0.38f)
        roundedBody(backTL, backSize, Color(0xFF827A6F), Color(0xFF463B31), cornerRadius = w * 0.18f)
        roundedBody(Offset(left + w * 0.05f, size.height * 0.5f), Size(w * 0.9f, size.height * 0.14f), Color(0xFF6B6157), Color(0xFF3A322A), cornerRadius = w * 0.1f)
        drawLine(Color(0xFF463B31), Offset(size.width * 0.5f, size.height * 0.64f), Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        listOf(-1f, -0.35f, 0.35f, 1f).forEach { dx ->
            drawLine(Color(0xFF3A322A), Offset(size.width * 0.5f, size.height * 0.8f), Offset(size.width * 0.5f + dx * w * 0.4f, size.height * 0.86f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
        }
        rimLight(backTL, backSize, cornerRadius = w * 0.18f, alpha = 0.4f)
    }
}

@Composable
fun WasteBinArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val topW = size.width * 0.4f
        val botW = size.width * 0.28f
        val top = size.height * 0.42f
        val bottom = size.height * 0.78f
        val cx = size.width * 0.5f
        val path = Path().apply {
            moveTo(cx - topW / 2f, top)
            lineTo(cx + topW / 2f, top)
            lineTo(cx + botW / 2f, bottom)
            lineTo(cx - botW / 2f, bottom)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFF9C9CA8), Color(0xFF54626F)), style = Fill)
        drawOval(color = Color(0xFF6B7684), topLeft = Offset(cx - topW / 2f, top - size.height * 0.02f), size = Size(topW, size.height * 0.04f))
        drawLine(Color.Black.copy(alpha = 0.12f), Offset(cx, top + size.height * 0.06f), Offset(cx, bottom - size.height * 0.04f), strokeWidth = size.width * 0.015f)
    }
}
