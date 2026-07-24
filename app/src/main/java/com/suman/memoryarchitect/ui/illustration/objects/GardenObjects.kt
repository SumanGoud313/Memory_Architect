package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.IdleAnimationKind
import com.suman.memoryarchitect.ui.illustration.ObjectArt
import com.suman.memoryarchitect.ui.illustration.register

/** Garden's own objects — terracotta, straw, and leaf tones instead of the app's usual indoor
 * wood/glass palette, since this is the first outdoor room. */
fun MutableMap<String, ObjectArt>.registerGardenObjects() {
    register(ObjectArt("watering_can", R.string.object_watering_can, drawer = { m -> WateringCanArt(m) }))
    register(ObjectArt("garden_gnome", R.string.object_garden_gnome, drawer = { m -> GardenGnomeArt(m) }))
    register(ObjectArt("flower_pot", R.string.object_flower_pot, idleAnimation = IdleAnimationKind.SWAY, drawer = { m -> FlowerPotArt(m) }))
    register(ObjectArt("garden_trowel", R.string.object_garden_trowel, drawer = { m -> GardenTrowelArt(m) }))
    register(ObjectArt("bird_bath", R.string.object_bird_bath, drawer = { m -> BirdBathArt(m) }))
    register(ObjectArt("wheelbarrow", R.string.object_wheelbarrow, drawer = { m -> WheelbarrowArt(m) }))
    register(ObjectArt("gardening_gloves", R.string.object_gardening_gloves, drawer = { m -> GardeningGlovesArt(m) }))
    register(ObjectArt("sunhat", R.string.object_sunhat, drawer = { m -> SunhatArt(m) }))
    register(ObjectArt("picnic_basket", R.string.object_picnic_basket, drawer = { m -> PicnicBasketArt(m) }))
}

@Composable
fun WateringCanArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val bodyC = Offset(size.width * 0.44f, size.height * 0.56f)
        circleBody(bodyC, size.width * 0.2f, Color(0xFF9CB48D), Color(0xFF4E6647))
        drawLine(Color(0xFF4E6647), bodyC + Offset(size.width * 0.16f, -size.width * 0.05f), bodyC + Offset(size.width * 0.4f, -size.width * 0.22f), strokeWidth = size.width * 0.06f, cap = StrokeCap.Round)
        drawArc(Color(0xFF7CA271), startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = bodyC - Offset(size.width * 0.14f, size.width * 0.3f), size = Size(size.width * 0.3f, size.width * 0.3f), style = Stroke(width = size.width * 0.045f, cap = StrokeCap.Round))
        rimLightCircle(bodyC, size.width * 0.2f, alpha = 0.4f)
    }
}

@Composable
fun GardenGnomeArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val bodyTL = Offset(size.width * 0.36f, size.height * 0.5f)
        val bodySize = Size(size.width * 0.28f, size.height * 0.24f)
        roundedBody(bodyTL, bodySize, Color(0xFF5C7A9C), Color(0xFF33465C), cornerRadius = size.width * 0.1f)
        val headC = Offset(size.width * 0.5f, size.height * 0.42f)
        circleBody(headC, size.width * 0.12f, Color(0xFFE8967F), Color(0xFF9C4A2E))
        val hat = Path().apply {
            moveTo(headC.x - size.width * 0.16f, headC.y - size.width * 0.02f)
            lineTo(headC.x + size.width * 0.02f, headC.y - size.width * 0.4f)
            lineTo(headC.x + size.width * 0.16f, headC.y - size.width * 0.02f)
            close()
        }
        drawPath(hat, brush = verticalBodyBrush(Color(0xFFE8674B), Color(0xFFA83E28)), style = Fill)
        drawOval(color = Color(0xFFF7F1E8), topLeft = headC + Offset(-size.width * 0.1f, size.width * 0.02f), size = Size(size.width * 0.2f, size.width * 0.12f))
        rimLightCircle(headC, size.width * 0.12f, alpha = 0.4f)
    }
}

@Composable
fun FlowerPotArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val potW = size.width * 0.36f
        val potLeft = size.width * 0.5f - potW / 2f
        val potTop = size.height * 0.6f
        val path = Path().apply {
            moveTo(potLeft, potTop)
            lineTo(potLeft + potW, potTop)
            lineTo(potLeft + potW * 0.82f, size.height * 0.82f)
            lineTo(potLeft + potW * 0.18f, size.height * 0.82f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFC9713F), Color(0xFF6E3D22)), style = Fill)
        val petalColors = listOf(Color(0xFFE8674B), Color(0xFFF0C875), Color(0xFFC98B7A), Color(0xFFE0937F), Color(0xFF9CB48D))
        val center = Offset(size.width * 0.5f, potTop - size.height * 0.1f)
        for (i in 0 until 6) {
            val angle = Math.toRadians(i * 60.0)
            val petalC = center + Offset((kotlin.math.cos(angle) * size.width * 0.1f).toFloat(), (kotlin.math.sin(angle) * size.width * 0.1f).toFloat())
            drawCircle(petalColors[i % petalColors.size], radius = size.width * 0.08f, center = petalC)
        }
        drawCircle(Color(0xFFF0C875), radius = size.width * 0.06f, center = center)
    }
}

@Composable
fun GardenTrowelArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val handleTop = Offset(size.width * 0.62f, size.height * 0.22f)
        val handleBottom = Offset(size.width * 0.44f, size.height * 0.46f)
        drawLine(Color(0xFF6B4A32), handleTop, handleBottom, strokeWidth = size.width * 0.08f, cap = StrokeCap.Round)
        val blade = Path().apply {
            moveTo(handleBottom.x, handleBottom.y)
            quadraticTo(handleBottom.x - size.width * 0.18f, handleBottom.y + size.height * 0.14f, handleBottom.x - size.width * 0.08f, handleBottom.y + size.height * 0.32f)
            quadraticTo(handleBottom.x + size.width * 0.08f, handleBottom.y + size.height * 0.4f, handleBottom.x + size.width * 0.16f, handleBottom.y + size.height * 0.2f)
            close()
        }
        drawPath(blade, brush = verticalBodyBrush(Color(0xFFC5C5CE), Color(0xFF827A6F)), style = Fill)
    }
}

@Composable
fun BirdBathArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val cx = size.width * 0.5f
        val basinY = size.height * 0.44f
        drawLine(Color(0xFFC5C5CE), Offset(cx, basinY), Offset(cx, size.height * 0.78f), strokeWidth = size.width * 0.08f, cap = StrokeCap.Round)
        drawOval(brush = verticalBodyBrush(Color(0xFFE8E8EC), Color(0xFF9C9CA8)), topLeft = Offset(cx - size.width * 0.28f, basinY - size.height * 0.06f), size = Size(size.width * 0.56f, size.height * 0.16f))
        drawOval(color = Color(0xFF7CB4FA).copy(alpha = 0.6f), topLeft = Offset(cx - size.width * 0.22f, basinY - size.height * 0.02f), size = Size(size.width * 0.44f, size.height * 0.08f))
        drawOval(brush = verticalBodyBrush(Color(0xFFE8E8EC), Color(0xFF9C9CA8)), topLeft = Offset(cx - size.width * 0.16f, size.height * 0.76f), size = Size(size.width * 0.32f, size.height * 0.06f))
    }
}

@Composable
fun WheelbarrowArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val trayPath = Path().apply {
            moveTo(size.width * 0.24f, size.height * 0.44f)
            lineTo(size.width * 0.72f, size.height * 0.44f)
            lineTo(size.width * 0.64f, size.height * 0.62f)
            lineTo(size.width * 0.32f, size.height * 0.62f)
            close()
        }
        drawPath(trayPath, brush = verticalBodyBrush(Color(0xFFE8674B), Color(0xFFA83E28)), style = Fill)
        drawCircle(Color(0xFF9CB48D).copy(alpha = 0.8f), radius = size.width * 0.05f, center = Offset(size.width * 0.42f, size.height * 0.5f))
        drawCircle(Color(0xFF463B31), radius = size.width * 0.09f, center = Offset(size.width * 0.5f, size.height * 0.72f), style = Stroke(width = size.width * 0.03f))
        listOf(-1f, 1f).forEach { side ->
            drawLine(Color(0xFF6B5238), Offset(size.width * (0.36f + side * 0.28f), size.height * 0.5f), Offset(size.width * (0.42f + side * 0.36f), size.height * 0.7f), strokeWidth = size.width * 0.025f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun GardeningGlovesArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        listOf(-1f, 1f).forEach { side ->
            val cx = size.width * 0.5f + side * size.width * 0.14f
            val path = Path().apply {
                moveTo(cx - size.width * 0.1f, size.height * 0.6f)
                quadraticTo(cx - size.width * 0.14f, size.height * 0.36f, cx - size.width * 0.04f, size.height * 0.3f)
                lineTo(cx + size.width * 0.06f, size.height * 0.3f)
                quadraticTo(cx + size.width * 0.14f, size.height * 0.4f, cx + size.width * 0.1f, size.height * 0.6f)
                close()
            }
            drawPath(path, brush = verticalBodyBrush(Color(0xFF9CB48D), Color(0xFF4E6647)))
        }
    }
}

@Composable
fun SunhatArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val cx = size.width * 0.5f
        val brimY = size.height * 0.56f
        drawOval(brush = verticalBodyBrush(Color(0xFFF0C875), Color(0xFFC79A46)), topLeft = Offset(cx - size.width * 0.36f, brimY), size = Size(size.width * 0.72f, size.height * 0.14f))
        drawArc(Color(0xFFC79A46), startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - size.width * 0.2f, brimY - size.height * 0.22f), size = Size(size.width * 0.4f, size.height * 0.3f))
        drawLine(Color(0xFF8B3A3A), Offset(cx - size.width * 0.2f, brimY - size.height * 0.02f), Offset(cx + size.width * 0.2f, brimY - size.height * 0.02f), strokeWidth = size.height * 0.03f)
        highlight(Offset(cx - size.width * 0.14f, brimY - size.height * 0.18f), Size(size.width * 0.16f, size.height * 0.12f), alpha = 0.3f)
    }
}

@Composable
fun PicnicBasketArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.5f
        val h = size.height * 0.28f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.5f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFFC9A97D), Color(0xFF8B6A4A), cornerRadius = w * 0.08f)
        for (i in 1..4) {
            drawLine(Color.Black.copy(alpha = 0.14f), Offset(left + w * i / 5f, top), Offset(left + w * i / 5f, top + h), strokeWidth = 2f)
        }
        drawArc(Color(0xFF6B4A32), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(left + w * 0.2f, top - h * 0.6f), size = Size(w * 0.6f, h * 0.9f), style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
        drawRoundRect(Color(0xFF8B3A3A), topLeft = Offset(left - 2f, top - 4f), size = Size(w + 4f, h * 0.16f), cornerRadius = CornerRadius(3f, 3f))
        rimLight(tl, sz, cornerRadius = w * 0.08f, alpha = 0.4f)
    }
}
