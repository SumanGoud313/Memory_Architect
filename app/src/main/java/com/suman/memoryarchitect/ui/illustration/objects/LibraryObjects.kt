package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/** Library's own objects — reading_lamp glows like [BedroomObjects]' table_lamp, everything else
 * leans into worn leather, brass, and parchment rather than the app's usual warm-gold palette. */
fun MutableMap<String, ObjectArt>.registerLibraryObjects() {
    register(ObjectArt("globe", R.string.object_globe, drawer = { m -> GlobeArt(m) }))
    register(ObjectArt("reading_lamp", R.string.object_reading_lamp, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> ReadingLampArt(m) }))
    register(ObjectArt("magnifying_glass", R.string.object_magnifying_glass, drawer = { m -> MagnifyingGlassArt(m) }))
    register(ObjectArt("quill_pen", R.string.object_quill_pen, drawer = { m -> QuillPenArt(m) }))
    register(ObjectArt("inkwell", R.string.object_inkwell, drawer = { m -> InkwellArt(m) }))
    register(ObjectArt("bookend", R.string.object_bookend, drawer = { m -> BookendArt(m) }))
    register(ObjectArt("armchair", R.string.object_armchair, drawer = { m -> ArmchairArt(m) }))
    register(ObjectArt("hourglass", R.string.object_hourglass, drawer = { m -> HourglassArt(m) }))
    register(ObjectArt("chess_set", R.string.object_chess_set, drawer = { m -> ChessSetArt(m) }))
    register(ObjectArt("wall_map", R.string.object_wall_map, drawer = { m -> WallMapArt(m) }))
    register(ObjectArt("telescope", R.string.object_telescope, drawer = { m -> TelescopeArt(m) }))
    register(ObjectArt("leather_journal", R.string.object_leather_journal, drawer = { m -> LeatherJournalArt(m) }))
    register(ObjectArt("wax_seal_stamp", R.string.object_wax_seal_stamp, drawer = { m -> WaxSealStampArt(m) }))
}

@Composable
fun GlobeArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.26f
        val center = Offset(size.width * 0.5f, size.height * 0.42f)
        drawLine(Color(0xFF6B5238), center + Offset(0f, r * 0.9f), center + Offset(0f, r * 1.5f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
        drawOval(brush = verticalBodyBrush(Color(0xFF6B5238), Color(0xFF3F2E1E)), topLeft = center + Offset(-r * 0.5f, r * 1.4f), size = Size(r, r * 0.3f))
        circleBody(center, r, Color(0xFF7CB4FA), Color(0xFF33465C))
        listOf(-0.4f, 0f, 0.4f).forEach { dx ->
            drawArc(Color(0xFF9CB48D).copy(alpha = 0.7f), startAngle = -70f, sweepAngle = 140f, useCenter = false, topLeft = center - Offset(r * (0.6f - dx * 0.1f), r), size = Size(r * (1.2f - kotlin.math.abs(dx)), r * 2f), style = Stroke(width = r * 0.16f))
        }
        drawLine(Color(0xFF463B31), center - Offset(0f, r * 1.05f), center + Offset(0f, r * 1.05f), strokeWidth = r * 0.05f)
        rimLightCircle(center, r, alpha = 0.5f)
    }
}

@Composable
fun ReadingLampArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val base = Offset(size.width * 0.5f, size.height * 0.76f)
        drawOval(brush = verticalBodyBrush(Color(0xFFC79A46), Color(0xFF8B6A22)), topLeft = base - Offset(size.width * 0.16f, size.height * 0.02f), size = Size(size.width * 0.32f, size.height * 0.06f))
        drawLine(Color(0xFFC79A46), base, base - Offset(0f, size.height * 0.32f), strokeWidth = size.width * 0.035f, cap = StrokeCap.Round)
        val shadeCenter = base - Offset(0f, size.height * 0.36f)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9CB48D).copy(alpha = 0.6f), Color.Transparent), center = shadeCenter, radius = size.width * 0.3f), radius = size.width * 0.3f, center = shadeCenter)
        val path = Path().apply {
            moveTo(shadeCenter.x - size.width * 0.2f, shadeCenter.y + size.height * 0.08f)
            lineTo(shadeCenter.x + size.width * 0.2f, shadeCenter.y + size.height * 0.08f)
            lineTo(shadeCenter.x + size.width * 0.12f, shadeCenter.y - size.height * 0.08f)
            lineTo(shadeCenter.x - size.width * 0.12f, shadeCenter.y - size.height * 0.08f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFF9CB48D), Color(0xFF4E6647)), style = Fill)
    }
}

@Composable
fun MagnifyingGlassArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val r = size.minDimension * 0.22f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFCEE0EC).copy(alpha = 0.5f), Color(0xFF7CB4FA).copy(alpha = 0.25f)), center = center, radius = r), radius = r, center = center)
        drawCircle(color = Color(0xFFC79A46), radius = r, center = center, style = Stroke(width = r * 0.16f))
        val handleStart = center + Offset(r * 0.7f, r * 0.7f)
        drawLine(Color(0xFF8B6A22), handleStart, handleStart + Offset(size.width * 0.2f, size.height * 0.2f), strokeWidth = r * 0.18f, cap = StrokeCap.Round)
        highlight(center - Offset(r * 0.4f, r * 0.4f), Size(r * 0.5f, r * 0.4f), alpha = 0.3f)
    }
}

@Composable
fun QuillPenArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val tip = Offset(size.width * 0.42f, size.height * 0.72f)
        val top = Offset(size.width * 0.66f, size.height * 0.2f)
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            quadraticTo(tip.x - size.width * 0.1f, (tip.y + top.y) / 2f, top.x - size.width * 0.08f, top.y)
            quadraticTo(top.x + size.width * 0.06f, (tip.y + top.y) / 2f + size.height * 0.06f, tip.x, tip.y)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFF7F1E8), Color(0xFFC9BFAF)), style = Fill)
        for (i in 1..4) {
            val t = i / 5f
            val fx = tip.x + (top.x - tip.x) * t
            val fy = tip.y + (top.y - tip.y) * t
            drawLine(Color(0xFFC9BFAF), Offset(fx, fy), Offset(fx - size.width * 0.05f * (1 - t), fy - size.height * 0.02f), strokeWidth = 1.5f)
        }
        drawLine(Color(0xFF463B31), tip, tip + Offset(-size.width * 0.02f, size.height * 0.06f), strokeWidth = size.width * 0.02f, cap = StrokeCap.Round)
    }
}

@Composable
fun InkwellArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.26f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.54f
        val tl = Offset(left, top)
        val sz = Size(w, size.height * 0.22f)
        roundedBody(tl, sz, Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.14f)
        drawOval(color = Color(0xFF1A1510), topLeft = Offset(left + w * 0.16f, top - size.height * 0.03f), size = Size(w * 0.68f, size.height * 0.06f))
        drawRoundRect(Color(0xFFC79A46), topLeft = Offset(left + w * 0.32f, top - size.height * 0.08f), size = Size(w * 0.36f, size.height * 0.06f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
        rimLight(tl, sz, cornerRadius = w * 0.14f, alpha = 0.4f)
    }
}

@Composable
fun BookendArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.3f
        val left = size.width * 0.5f - w / 2f
        val path = Path().apply {
            moveTo(left, size.height * 0.7f)
            lineTo(left, size.height * 0.34f)
            lineTo(left + w, size.height * 0.7f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFC79A46), Color(0xFF8B6A22)), style = Fill)
        drawCircle(color = Color(0xFFF0C875), radius = w * 0.14f, center = Offset(left, size.height * 0.34f))
        rimLightCircle(Offset(left, size.height * 0.34f), w * 0.14f, alpha = 0.5f)
    }
}

@Composable
fun ArmchairArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.48f
        val left = size.width * 0.5f - w / 2f
        val backTL = Offset(left, size.height * 0.2f)
        val backSize = Size(w, size.height * 0.36f)
        roundedBody(backTL, backSize, Color(0xFF8B3A3A), Color(0xFF4A1F1F), cornerRadius = w * 0.24f)
        roundedBody(Offset(left, size.height * 0.5f), Size(w, size.height * 0.18f), Color(0xFF8B3A3A), Color(0xFF4A1F1F), cornerRadius = w * 0.16f)
        listOf(-1f, 1f).forEach { side ->
            roundedBody(Offset(size.width * 0.5f + side * w * 0.44f - w * 0.1f, size.height * 0.3f), Size(w * 0.2f, size.height * 0.32f), Color(0xFF8B3A3A), Color(0xFF4A1F1F), cornerRadius = w * 0.1f)
        }
        rimLight(backTL, backSize, cornerRadius = w * 0.24f, alpha = 0.4f)
    }
}

@Composable
fun HourglassArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.3f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.24f
        val mid = size.height * 0.48f
        val bottom = size.height * 0.72f
        val frame = Path().apply {
            moveTo(left, top); lineTo(left + w, top); lineTo(size.width * 0.5f, mid); lineTo(left + w, bottom); lineTo(left, bottom); lineTo(size.width * 0.5f, mid); close()
        }
        drawPath(frame, color = Color(0xFFC79A46), style = Stroke(width = size.width * 0.025f))
        val sandTop = Path().apply { moveTo(left + w * 0.15f, top + size.height * 0.02f); lineTo(left + w * 0.85f, top + size.height * 0.02f); lineTo(size.width * 0.5f, mid - size.height * 0.02f); close() }
        drawPath(sandTop, brush = verticalBodyBrush(Color(0xFFF0C875), Color(0xFFC79A46)))
        val sandBottom = Path().apply { moveTo(left + w * 0.3f, bottom - size.height * 0.04f); lineTo(left + w * 0.7f, bottom - size.height * 0.04f); lineTo(size.width * 0.5f, mid + size.height * 0.06f); close() }
        drawPath(sandBottom, brush = verticalBodyBrush(Color(0xFFC79A46), Color(0xFFF0C875)))
        drawRoundRect(Color(0xFF6B5238), topLeft = Offset(left - 4f, top - size.height * 0.03f), size = Size(w + 8f, size.height * 0.03f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawRoundRect(Color(0xFF6B5238), topLeft = Offset(left - 4f, bottom), size = Size(w + 8f, size.height * 0.03f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
    }
}

@Composable
fun ChessSetArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.5f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.5f
        val cell = w / 4f
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if ((r + c) % 2 == 0) {
                    drawRect(Color(0xFF3F2E1E), topLeft = Offset(left + c * cell, top + r * cell * 0.5f), size = Size(cell, cell * 0.5f))
                }
            }
        }
        drawRect(color = Color(0xFF6B5238), topLeft = Offset(left, top), size = Size(w, cell * 2f), style = Stroke(width = 2f))
        drawCircle(color = Color(0xFFF7F1E8), radius = cell * 0.28f, center = Offset(left + cell * 1.5f, top + cell * 0.25f))
        drawRect(color = Color(0xFF463B31), topLeft = Offset(left + cell * 2.35f, top + cell * 0.1f), size = Size(cell * 0.3f, cell * 0.4f))
    }
}

@Composable
fun WallMapArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.6f
        val h = size.height * 0.44f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.28f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFFE8DCC0), Color(0xFFC9B98E), cornerRadius = w * 0.03f)
        matteNoise(tl, sz, color = Color(0xFF7A5230), alpha = 0.15f, count = 20, seed = 44)
        listOf(0.3f to 0.4f, 0.6f to 0.55f, 0.45f to 0.7f).forEach { (dx, dy) ->
            drawCircle(Color(0xFF8B3A3A).copy(alpha = 0.4f), radius = w * 0.02f, center = Offset(left + w * dx, top + h * dy))
        }
        drawLine(Color(0xFF6B5238), Offset(left + w * 0.2f, top + h * 0.3f), Offset(left + w * 0.7f, top + h * 0.65f), strokeWidth = 1.5f)
        rimLight(tl, sz, cornerRadius = w * 0.03f, alpha = 0.3f)
    }
}

@Composable
fun TelescopeArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val base = Offset(size.width * 0.5f, size.height * 0.76f)
        listOf(-1f, 0f, 1f).forEach { dx ->
            drawLine(Color(0xFF6B5238), base, base + Offset(dx * size.width * 0.16f, size.height * 0.14f), strokeWidth = size.width * 0.025f, cap = StrokeCap.Round)
        }
        val tubeStart = base - Offset(0f, size.height * 0.06f)
        val tubeEnd = tubeStart - Offset(size.width * 0.24f, size.height * 0.42f)
        drawLine(brush = verticalBodyBrush(Color(0xFFC79A46), Color(0xFF6B5238)), start = tubeStart, end = tubeEnd, strokeWidth = size.width * 0.14f, cap = StrokeCap.Round)
        drawCircle(color = Color(0xFF33465C), radius = size.width * 0.08f, center = tubeEnd)
        rimLightCircle(tubeEnd, size.width * 0.08f, alpha = 0.5f)
    }
}

@Composable
fun LeatherJournalArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.5f
        val h = size.height * 0.12f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.6f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFF6B3A22), Color(0xFF3F2010), cornerRadius = h * 0.2f)
        drawLine(Color(0xFFC79A46), Offset(left + w * 0.2f, top - h * 0.4f), Offset(left + w * 0.2f, top + h * 1.4f), strokeWidth = w * 0.02f)
        rimLight(tl, sz, cornerRadius = h * 0.2f, alpha = 0.4f)
        matteNoise(tl, sz, color = Color.Black, alpha = 0.1f, count = 6, seed = 55)
    }
}

@Composable
fun WaxSealStampArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val r = size.minDimension * 0.16f
        val center = Offset(size.width * 0.42f, size.height * 0.6f)
        circleBody(center, r, Color(0xFFC94E3A), Color(0xFF7A2418))
        drawCircle(color = Color(0xFF7A2418), radius = r * 0.4f, center = center)
        rimLightCircle(center, r, alpha = 0.5f)
        drawLine(Color(0xFF463B31), center + Offset(r * 0.7f, -r * 0.4f), center + Offset(r * 1.6f, -r * 1.3f), strokeWidth = size.width * 0.03f, cap = StrokeCap.Round)
    }
}
