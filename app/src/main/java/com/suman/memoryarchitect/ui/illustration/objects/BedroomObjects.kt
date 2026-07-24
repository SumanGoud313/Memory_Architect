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

fun MutableMap<String, ObjectArt>.registerBedroomObjects() {
    register(ObjectArt("pillow", R.string.object_pillow, drawer = { m -> PillowArt(m) }))
    register(ObjectArt("table_lamp", R.string.object_table_lamp, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> TableLampArt(m) }))
    register(ObjectArt("alarm_clock", R.string.object_alarm_clock, drawer = { m -> AlarmClockArt(m) }))
    register(ObjectArt("picture_frame", R.string.object_picture_frame, drawer = { m -> PictureFrameArt(m) }))
    register(ObjectArt("mirror", R.string.object_mirror, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> MirrorArt(m) }))
    register(ObjectArt("jewelry_box", R.string.object_jewelry_box, drawer = { m -> JewelryBoxArt(m) }))
    register(ObjectArt("candle", R.string.object_candle, idleAnimation = IdleAnimationKind.FLICKER, drawer = { m -> CandleArt(m) }))
    register(ObjectArt("teddy_bear", R.string.object_teddy_bear, drawer = { m -> TeddyBearArt(m) }))
    register(ObjectArt("folded_blanket", R.string.object_folded_blanket, drawer = { m -> FoldedBlanketArt(m) }))
    register(ObjectArt("slippers", R.string.object_slippers, drawer = { m -> SlippersArt(m) }))
    register(ObjectArt("coat_hanger", R.string.object_coat_hanger, idleAnimation = IdleAnimationKind.SWAY, drawer = { m -> CoatHangerArt(m) }))
    register(ObjectArt("perfume_bottle", R.string.object_perfume_bottle, drawer = { m -> PerfumeBottleArt(m) }))
    register(ObjectArt("eyeglasses", R.string.object_eyeglasses, drawer = { m -> EyeglassesArt(m) }))
    register(ObjectArt("tissue_box", R.string.object_tissue_box, drawer = { m -> TissueBoxArt(m) }))
    register(ObjectArt("flower_vase", R.string.object_flower_vase, idleAnimation = IdleAnimationKind.SWAY, drawer = { m -> FlowerVaseArt(m) }))
}

@Composable
fun PillowArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val tl = Offset(size.width * 0.12f, size.height * 0.34f)
        val sz = Size(size.width * 0.76f, size.height * 0.42f)
        roundedBody(tl, sz, Color(0xFFFAF5EC), Color(0xFFB8AC97), cornerRadius = size.width * 0.18f)
        fabricWeave(tl, sz, color = Color(0xFF8B5E3C), alpha = 0.05f, lineCount = 5)
        rimLight(tl, sz, cornerRadius = size.width * 0.18f, alpha = 0.5f)
        highlight(Offset(size.width * 0.2f, size.height * 0.4f), Size(size.width * 0.3f, size.height * 0.16f))
    }
}

@Composable
fun TableLampArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val baseW = size.width * 0.3f
        val baseTL = Offset(size.width * 0.5f - baseW / 2f, size.height * 0.78f)
        val baseSize = Size(baseW, size.height * 0.08f)
        roundedBody(baseTL, baseSize, Color(0xFFAEA48F), Color(0xFF5C5344), cornerRadius = baseW * 0.2f)
        drawRect(color = Color(0xFF948977), topLeft = Offset(size.width * 0.48f, size.height * 0.5f), size = Size(size.width * 0.04f, size.height * 0.3f))
        val shadeTop = Offset(size.width * 0.34f, size.height * 0.5f)
        val path = Path().apply {
            moveTo(shadeTop.x, shadeTop.y)
            lineTo(shadeTop.x + size.width * 0.32f, shadeTop.y)
            lineTo(shadeTop.x + size.width * 0.4f, shadeTop.y + size.height * 0.22f)
            lineTo(shadeTop.x - size.width * 0.08f, shadeTop.y + size.height * 0.22f)
            close()
        }
        drawOval(
            brush = Brush.radialGradient(listOf(Color(0xFFFFE9A8).copy(alpha = 0.5f), Color.Transparent), center = Offset(size.width * 0.5f, shadeTop.y + size.height * 0.05f), radius = size.width * 0.4f),
            topLeft = Offset(shadeTop.x - size.width * 0.2f, shadeTop.y - size.height * 0.16f),
            size = Size(size.width * 0.8f, size.height * 0.32f),
        )
        drawPath(path, brush = verticalBodyBrush(Color(0xFFF7D68C), Color(0xFFC79A46)), style = Fill)
        rimLight(shadeTop, Size(size.width * 0.4f, size.height * 0.22f), cornerRadius = size.width * 0.04f, alpha = 0.4f)
        highlight(baseTL, baseSize, alpha = 0.2f)
    }
}

@Composable
fun AlarmClockArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.32f
        val center = Offset(size.width * 0.5f, size.height * 0.52f)
        circleBody(center, r, Color(0xFFFAF5EC), Color(0xFFB8AC97))
        drawCircle(color = Color(0xFF3A322A), radius = r * 0.9f, center = center, style = Stroke(width = r * 0.08f))
        drawLine(Color(0xFF3A322A), center, center + Offset(0f, -r * 0.5f), strokeWidth = r * 0.12f, cap = StrokeCap.Round)
        drawLine(Color(0xFF3A322A), center, center + Offset(r * 0.35f, 0f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        listOf(-1f, 1f).forEach { side ->
            drawCircle(color = Color(0xFF948977), radius = r * 0.22f, center = center + Offset(side * r * 0.75f, -r * 0.9f))
        }
        rimLightCircle(center, r, alpha = 0.45f)
    }
}

@Composable
fun PictureFrameArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.62f
        val h = size.height * 0.68f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.12f
        val frameTL = Offset(left, top)
        val frameSize = Size(w, h)
        roundedBody(frameTL, frameSize, Color(0xFFD8AC56), Color(0xFF7A4E2E), cornerRadius = w * 0.06f)
        woodGrainLines(frameTL, frameSize, color = Color(0xFF4A2E18), alpha = 0.14f, lineCount = 5)
        val innerInset = w * 0.1f
        drawRect(
            brush = verticalBodyBrush3(Color(0xFFC5E0EE), Color(0xFFAED4E8), Color(0xFF8FA383)),
            topLeft = Offset(left + innerInset, top + innerInset),
            size = Size(w - innerInset * 2f, h - innerInset * 2f),
        )
        rimLight(frameTL, frameSize, cornerRadius = w * 0.06f, alpha = 0.45f)
    }
}

@Composable
fun MirrorArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val center = Offset(size.width * 0.5f, size.height * 0.42f)
        val r = size.minDimension * 0.36f
        drawOval(
            brush = verticalBodyBrush(Color(0xFFD8AC56), Color(0xFF7A4E2E)),
            topLeft = center - Offset(r, r * 1.1f), size = Size(r * 2f, r * 2.2f),
        )
        drawOval(
            brush = verticalBodyBrush(Color(0xFFE8F0F7), Color(0xFFAED4E8)),
            topLeft = center - Offset(r * 0.8f, r * 0.9f), size = Size(r * 1.6f, r * 1.8f),
        )
        drawLine(
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f))),
            start = center - Offset(r * 0.5f, r * 0.75f),
            end = center + Offset(r * 0.05f, -r * 0.1f),
            strokeWidth = r * 0.18f,
            cap = StrokeCap.Round,
        )
        highlight(center - Offset(r * 0.4f, r * 0.6f), Size(r * 0.5f, r * 0.7f), alpha = 0.5f)
    }
}

@Composable
fun JewelryBoxArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.58f
        val h = size.height * 0.34f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.5f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        val lidTL = Offset(left, top - h * 0.28f)
        val lidSize = Size(w, h * 0.32f)
        roundedBody(bodyTL, bodySize, Color(0xFFE8967F), Color(0xFF9C4A2E), cornerRadius = w * 0.08f)
        roundedBody(lidTL, lidSize, Color(0xFFF7D68C), Color(0xFFB88936), cornerRadius = w * 0.08f)
        speckle(lidTL, lidSize, color = Color(0xFFFFF6DD), alpha = 0.6f, count = 4, seed = 21)
        rimLight(lidTL, lidSize, cornerRadius = w * 0.08f, alpha = 0.5f)
        highlight(Offset(left + w * 0.1f, top + h * 0.1f), Size(w * 0.3f, h * 0.3f))
    }
}

@Composable
fun CandleArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.26f
        val h = size.height * 0.4f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.42f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFFFAF5EC), Color(0xFFB8AC97), cornerRadius = w * 0.1f)
        val flameCenter = Offset(size.width * 0.5f, top - size.height * 0.1f)
        drawOval(
            brush = Brush.radialGradient(listOf(Color(0xFFFFD98A).copy(alpha = 0.45f), Color.Transparent), center = flameCenter, radius = size.width * 0.24f),
            topLeft = flameCenter - Offset(size.width * 0.24f, size.width * 0.24f),
            size = Size(size.width * 0.48f, size.width * 0.48f),
        )
        drawLine(Color(0xFF5C5344), flameCenter + Offset(0f, size.height * 0.02f), flameCenter + Offset(0f, -size.height * 0.02f), strokeWidth = w * 0.06f)
        drawPath(
            path = Path().apply {
                moveTo(flameCenter.x, flameCenter.y - size.height * 0.14f)
                quadraticTo(flameCenter.x + size.width * 0.09f, flameCenter.y - size.height * 0.02f, flameCenter.x, flameCenter.y + size.height * 0.08f)
                quadraticTo(flameCenter.x - size.width * 0.09f, flameCenter.y - size.height * 0.02f, flameCenter.x, flameCenter.y - size.height * 0.14f)
                close()
            },
            brush = verticalBodyBrush(Color(0xFFF2B441), Color(0xFFE8674B)),
            style = Fill,
        )
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.1f, alpha = 0.4f)
        highlight(Offset(left + w * 0.15f, top + h * 0.1f), Size(w * 0.3f, h * 0.35f))
    }
}

@Composable
fun TeddyBearArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val bodyR = size.minDimension * 0.24f
        val bodyCenter = Offset(size.width * 0.5f, size.height * 0.58f)
        circleBody(bodyCenter, bodyR, Color(0xFFD8B98C), Color(0xFF8B5E3C))
        val headR = bodyR * 0.68f
        val headCenter = bodyCenter - Offset(0f, bodyR + headR * 0.7f)
        circleBody(headCenter, headR, Color(0xFFD8B98C), Color(0xFF8B5E3C))
        listOf(-1f, 1f).forEach { side ->
            drawCircle(color = Color(0xFF8B5E3C), radius = headR * 0.32f, center = headCenter + Offset(side * headR * 0.75f, -headR * 0.7f))
            drawCircle(color = Color(0xFF3A322A), radius = headR * 0.08f, center = headCenter + Offset(side * headR * 0.32f, -headR * 0.1f))
        }
        drawCircle(color = Color(0xFFF0E6D2), radius = headR * 0.4f, center = headCenter + Offset(0f, headR * 0.25f))
        rimLightCircle(headCenter, headR, alpha = 0.4f)
        rimLightCircle(bodyCenter, bodyR, alpha = 0.35f)
    }
}

@Composable
fun FoldedBlanketArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.7f
        val left = size.width * 0.5f - w / 2f
        val stripeColors = listOf(Color(0xFFE8967F) to Color(0xFF9C4A2E), Color(0xFFF7D68C) to Color(0xFFB88936), Color(0xFF9CB48D) to Color(0xFF4E6647))
        stripeColors.forEachIndexed { i, (light, dark) ->
            val tl = Offset(left, size.height * (0.42f + i * 0.13f))
            val sz = Size(w, size.height * 0.16f)
            roundedBody(tl, sz, light, dark, cornerRadius = size.height * 0.05f)
            fabricWeave(tl, sz, color = Color.Black, alpha = 0.06f, lineCount = 6)
            if (i == 0) rimLight(tl, sz, cornerRadius = size.height * 0.05f, alpha = 0.4f)
        }
    }
}

@Composable
fun SlippersArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        listOf(-1f, 1f).forEach { side ->
            val cx = size.width * 0.5f + side * size.width * 0.18f
            val topLeft = Offset(cx - size.width * 0.16f, size.height * 0.56f)
            val ovalSize = Size(size.width * 0.32f, size.height * 0.24f)
            drawOval(brush = verticalBodyBrush(Color(0xFFE8967F), Color(0xFF9C4A2E)), topLeft = topLeft, size = ovalSize)
            drawOval(color = Color.Black.copy(alpha = 0.12f), topLeft = topLeft + Offset(0f, ovalSize.height * 0.55f), size = Size(ovalSize.width, ovalSize.height * 0.45f))
            drawArc(
                brush = verticalBodyBrush(Color(0xFFFAF5EC), Color(0xFFDCD2BE)),
                startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(cx - size.width * 0.12f, size.height * 0.5f),
                size = Size(size.width * 0.24f, size.height * 0.16f),
            )
        }
    }
}

@Composable
fun CoatHangerArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val top = Offset(size.width * 0.5f, size.height * 0.18f)
        drawCircle(color = Color(0xFF948977), radius = size.width * 0.03f, center = top)
        val path = Path().apply {
            moveTo(top.x, top.y)
            lineTo(size.width * 0.2f, size.height * 0.42f)
            lineTo(size.width * 0.8f, size.height * 0.42f)
            close()
        }
        drawPath(path, color = Color(0xFF8B5E3C), style = Stroke(width = size.width * 0.05f, cap = StrokeCap.Round))
        val garmentTL = Offset(size.width * 0.15f, size.height * 0.42f)
        val garmentSize = Size(size.width * 0.7f, size.height * 0.3f)
        roundedBody(garmentTL, garmentSize, Color(0xFFC5E0EE), Color(0xFF6F8F65), cornerRadius = size.width * 0.05f)
        fabricWeave(garmentTL, garmentSize, color = Color.Black, alpha = 0.05f, lineCount = 5)
        rimLight(garmentTL, garmentSize, cornerRadius = size.width * 0.05f, alpha = 0.4f)
    }
}

@Composable
fun PerfumeBottleArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.3f
        val left = size.width * 0.5f - w / 2f
        val bodyTL = Offset(left, size.height * 0.42f)
        val bodySize = Size(w, size.height * 0.4f)
        roundedBody(bodyTL, bodySize, Color(0xFFE8F0F7), Color(0xFF9CBAD0), cornerRadius = w * 0.14f)
        roundedBody(Offset(size.width * 0.5f - w * 0.18f, size.height * 0.28f), Size(w * 0.36f, size.height * 0.16f), Color(0xFFD8AC56), Color(0xFF7A4E2E), cornerRadius = w * 0.06f)
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.14f, alpha = 0.55f)
        highlight(Offset(left + w * 0.12f, size.height * 0.48f), Size(w * 0.25f, size.height * 0.2f))
    }
}

@Composable
fun EyeglassesArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val r = size.width * 0.16f
        val y = size.height * 0.5f
        listOf(-1f, 1f).forEach { side ->
            val c = Offset(size.width * 0.5f + side * r * 1.3f, y)
            drawCircle(color = Color.White.copy(alpha = 0.1f), radius = r * 0.75f, center = c)
            drawCircle(color = Color(0xFF3A322A), radius = r, center = c, style = Stroke(width = r * 0.22f))
            rimLightCircle(c, r, alpha = 0.4f, strokeWidth = r * 0.14f)
        }
        drawLine(Color(0xFF3A322A), Offset(size.width * 0.5f - r * 0.3f, y), Offset(size.width * 0.5f + r * 0.3f, y), strokeWidth = r * 0.18f)
    }
}

@Composable
fun TissueBoxArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.6f
        val h = size.height * 0.36f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.5f
        val boxTL = Offset(left, top)
        val boxSize = Size(w, h)
        roundedBody(boxTL, boxSize, Color(0xFFF7D68C), Color(0xFFB88936), cornerRadius = w * 0.06f)
        matteNoise(boxTL, boxSize, color = Color(0xFF6E4E1E), alpha = 0.08f, count = 12, seed = 31)
        drawOval(color = Color(0xFFFAF5EC), topLeft = Offset(left + w * 0.32f, top - h * 0.06f), size = Size(w * 0.36f, h * 0.24f))
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.5f, top - h * 0.06f)
                quadraticTo(size.width * 0.58f, top - h * 0.3f, size.width * 0.54f, top - h * 0.42f)
            },
            color = Color(0xFFFAF5EC),
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round),
        )
        rimLight(boxTL, boxSize, cornerRadius = w * 0.06f, alpha = 0.4f)
    }
}

@Composable
fun FlowerVaseArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.28f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.54f
        val vaseTL = Offset(left, top)
        val vaseSize = Size(w, size.height * 0.3f)
        roundedBody(vaseTL, vaseSize, Color(0xFFC5E0EE), Color(0xFF6F8F65), cornerRadius = w * 0.3f)
        rimLight(vaseTL, vaseSize, cornerRadius = w * 0.3f, alpha = 0.5f)
        listOf(-0.3f, 0f, 0.3f).forEachIndexed { i, dx ->
            val stemTop = Offset(size.width * 0.5f + dx * size.width, top - size.height * (0.3f + i * 0.05f))
            drawLine(Color(0xFF4E6647), Offset(size.width * 0.5f, top), stemTop, strokeWidth = size.width * 0.02f)
            val petalColors = listOf(Color(0xFFE8967F) to Color(0xFF9C4A2E), Color(0xFFF7D68C) to Color(0xFFB88936), Color(0xFFE8674B) to Color(0xFFA83E28))[i]
            circleBody(stemTop, size.width * 0.07f, petalColors.first, petalColors.second)
        }
    }
}
