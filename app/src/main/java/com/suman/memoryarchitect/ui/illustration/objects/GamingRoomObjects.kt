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

/** Gaming Room's own objects — desk_fan (rotate) and gaming_pc_tower/monitor (RGB pulse) are the hero pieces. */
fun MutableMap<String, ObjectArt>.registerGamingRoomObjects() {
    register(ObjectArt("gaming_pc_tower", R.string.object_gaming_pc_tower, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> GamingPcTowerArt(m) }))
    register(ObjectArt("monitor", R.string.object_monitor, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> MonitorArt(m) }))
    register(ObjectArt("gaming_chair", R.string.object_gaming_chair, drawer = { m -> GamingChairArt(m) }))
    register(ObjectArt("headset", R.string.object_headset, drawer = { m -> HeadsetArt(m) }))
    register(ObjectArt("mechanical_keyboard", R.string.object_mechanical_keyboard, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> MechanicalKeyboardArt(m) }))
    register(ObjectArt("gaming_mouse", R.string.object_gaming_mouse, drawer = { m -> GamingMouseArt(m) }))
    register(ObjectArt("controller", R.string.object_controller, drawer = { m -> ControllerArt(m) }))
    register(ObjectArt("desk_fan", R.string.object_desk_fan, idleAnimation = IdleAnimationKind.ROTATE, drawer = { m -> DeskFanArt(m) }))
    register(ObjectArt("led_strip", R.string.object_led_strip, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> LedStripArt(m) }))
    register(ObjectArt("poster", R.string.object_poster, drawer = { m -> PosterArt(m) }))
    register(ObjectArt("energy_drink_can", R.string.object_energy_drink_can, drawer = { m -> EnergyDrinkCanArt(m) }))
    register(ObjectArt("figurine_collectible", R.string.object_figurine_collectible, drawer = { m -> FigurineArt(m) }))
    register(ObjectArt("trophy", R.string.object_trophy, drawer = { m -> TrophyArt(m) }))
    register(ObjectArt("backpack", R.string.object_backpack, drawer = { m -> BackpackArt(m) }))
}

@Composable
fun GamingPcTowerArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.4f
        val h = size.height * 0.5f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.3f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.08f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFF0876A), Color(0xFFA83E28)), center = Offset(left + w * 0.5f, top + h * 0.3f) - Offset(w * 0.04f, w * 0.04f), radius = w * 0.22f),
            radius = w * 0.14f, center = Offset(left + w * 0.5f, top + h * 0.3f),
        )
        drawRect(color = Color(0xFF7CA271).copy(alpha = 0.75f), topLeft = Offset(left + w * 0.15f, top + h * 0.55f), size = Size(w * 0.7f, h * 0.32f))
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.08f, alpha = 0.4f)
        highlight(Offset(left + w * 0.1f, top + h * 0.05f), Size(w * 0.2f, h * 0.2f), alpha = 0.2f)
    }
}

@Composable
fun MonitorArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.7f
        val h = size.height * 0.42f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.28f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.04f)
        roundedBody(Offset(left + w * 0.06f, top + h * 0.08f), Size(w * 0.88f, h * 0.84f), Color(0xFF7CB4FA), Color(0xFFA83E28), cornerRadius = w * 0.02f)
        drawRect(
            brush = Brush.radialGradient(listOf(Color(0xFF7CB4FA).copy(alpha = 0.35f), Color.Transparent), center = Offset(size.width * 0.5f, top + h * 0.4f), radius = w * 0.6f),
            topLeft = bodyTL, size = bodySize,
        )
        drawRect(color = Color(0xFF6B6157), topLeft = Offset(size.width * 0.5f - w * 0.05f, top + h), size = Size(w * 0.1f, h * 0.16f))
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.04f, alpha = 0.45f)
    }
}

@Composable
fun GamingChairArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.42f
        val left = size.width * 0.5f - w / 2f
        val backTL = Offset(left, size.height * 0.16f)
        val backSize = Size(w, size.height * 0.4f)
        roundedBody(backTL, backSize, Color(0xFFF0876A), Color(0xFF241D17), cornerRadius = w * 0.16f)
        drawLine(Color(0xFF60A5FA).copy(alpha = 0.7f), backTL + Offset(w * 0.5f, size.height * 0.06f), backTL + Offset(w * 0.5f, size.height * 0.32f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
        roundedBody(Offset(left, size.height * 0.5f), Size(w, size.height * 0.14f), Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.1f)
        drawLine(Color(0xFF6B6157), Offset(size.width * 0.5f, size.height * 0.64f), Offset(size.width * 0.5f, size.height * 0.78f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        rimLight(backTL, backSize, cornerRadius = w * 0.16f, alpha = 0.45f)
    }
}

@Composable
fun HeadsetArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        drawArc(
            color = Color(0xFF463B31), startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.18f), size = Size(size.width * 0.56f, size.height * 0.4f), style = Stroke(width = size.width * 0.06f, cap = StrokeCap.Round),
        )
        listOf(-1f, 1f).forEach { side ->
            val c = Offset(size.width * 0.5f + side * size.width * 0.28f, size.height * 0.46f)
            circleBody(c, size.width * 0.1f, Color(0xFFF0876A), Color(0xFF241D17))
            rimLightCircle(c, size.width * 0.1f, alpha = 0.5f)
        }
    }
}

@Composable
fun MechanicalKeyboardArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.62f
        val h = size.height * 0.24f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.5f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.06f)
        val cols = 6
        val rows = 2
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val kx = left + w * (0.08f + c * 0.85f / cols)
                val ky = top + h * (0.2f + r * 0.5f)
                val keyColor = listOf(Color(0xFFF0876A), Color(0xFF7CB4FA), Color(0xFF9CB48D))[(r + c) % 3]
                drawRect(
                    brush = Brush.radialGradient(listOf(keyColor, keyColor.copy(alpha = 0.3f)), center = Offset(kx, ky), radius = w * 0.12f),
                    topLeft = Offset(kx, ky), size = Size(w * 0.1f, h * 0.28f),
                )
            }
        }
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.06f, alpha = 0.4f)
    }
}

@Composable
fun GamingMouseArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val tl = Offset(size.width * 0.36f, size.height * 0.36f)
        val sz = Size(size.width * 0.28f, size.height * 0.4f)
        drawOval(brush = verticalBodyBrush(Color(0xFF827A6F), Color(0xFF241D17)), topLeft = tl, size = sz)
        drawLine(Color(0xFFF0876A), Offset(size.width * 0.5f, size.height * 0.4f), Offset(size.width * 0.5f, size.height * 0.52f), strokeWidth = size.width * 0.015f)
        highlight(tl + Offset(sz.width * 0.15f, sz.height * 0.1f), Size(sz.width * 0.35f, sz.height * 0.3f), alpha = 0.3f)
    }
}

@Composable
fun ControllerArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.5f)
            quadraticTo(size.width * 0.22f, size.height * 0.36f, size.width * 0.38f, size.height * 0.4f)
            lineTo(size.width * 0.62f, size.height * 0.4f)
            quadraticTo(size.width * 0.78f, size.height * 0.36f, size.width * 0.78f, size.height * 0.5f)
            quadraticTo(size.width * 0.78f, size.height * 0.66f, size.width * 0.66f, size.height * 0.6f)
            lineTo(size.width * 0.34f, size.height * 0.6f)
            quadraticTo(size.width * 0.22f, size.height * 0.66f, size.width * 0.22f, size.height * 0.5f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFF827A6F), Color(0xFF241D17)), style = Fill)
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(size.width * 0.3f, size.height * 0.44f), end = Offset(size.width * 0.4f, size.height * 0.42f),
            strokeWidth = size.width * 0.012f,
        )
        listOf(-1f, 1f).forEach { side ->
            drawCircle(color = Color(0xFFF0876A), radius = size.width * 0.03f, center = Offset(size.width * 0.5f + side * size.width * 0.16f, size.height * 0.5f))
        }
    }
}

@Composable
fun DeskFanArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.28f
        val center = Offset(size.width * 0.5f, size.height * 0.44f)
        drawCircle(color = Color(0xFF463B31), radius = r, center = center, style = Stroke(width = r * 0.1f))
        for (i in 0..2) {
            val angle = Math.toRadians((i * 120).toDouble())
            val tip = center + Offset((kotlin.math.cos(angle) * r * 0.75f).toFloat(), (kotlin.math.sin(angle) * r * 0.75f).toFloat())
            drawOval(brush = verticalBodyBrush(Color(0xFFE8E8EC), Color(0xFF9C9CA8)), topLeft = tip - Offset(r * 0.28f, r * 0.16f), size = Size(r * 0.56f, r * 0.32f))
        }
        drawCircle(color = Color(0xFF6B6157), radius = r * 0.12f, center = center)
        rimLightCircle(center, r, alpha = 0.35f)
        drawLine(Color(0xFF948977), center + Offset(0f, r), center + Offset(0f, r * 1.6f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
    }
}

@Composable
fun LedStripArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width * 0.7f
        val left = size.width * 0.5f - w / 2f
        val colors = listOf(Color(0xFFF0876A), Color(0xFFFFD98A), Color(0xFF9CB48D), Color(0xFF7CB4FA))
        colors.forEachIndexed { i, c ->
            drawRect(
                brush = Brush.radialGradient(listOf(c, c.copy(alpha = 0f))),
                topLeft = Offset(left + w * i / colors.size, size.height * 0.46f),
                size = Size(w / colors.size, size.height * 0.1f),
            )
        }
        drawRect(
            brush = verticalBodyBrush(Color(0xFF463B31), Color(0xFF17120D)),
            topLeft = Offset(left, size.height * 0.5f), size = Size(w, size.height * 0.02f),
        )
    }
}

@Composable
fun PosterArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.58f
        val h = size.height * 0.7f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.1f
        val frameTL = Offset(left, top)
        val frameSize = Size(w, h)
        roundedBody(frameTL, frameSize, Color(0xFF463B31), Color(0xFF17120D), cornerRadius = w * 0.03f)
        drawRect(
            brush = Brush.linearGradient(listOf(Color(0xFFF0876A), Color(0xFF7CB4FA))),
            topLeft = Offset(left + w * 0.08f, top + h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
        )
        rimLight(frameTL, frameSize, cornerRadius = w * 0.03f, alpha = 0.35f)
    }
}

@Composable
fun EnergyDrinkCanArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.24f
        val left = size.width * 0.5f - w / 2f
        val bodyTL = Offset(left, size.height * 0.34f)
        val bodySize = Size(w, size.height * 0.46f)
        roundedBody(bodyTL, bodySize, Color(0xFF9CB48D), Color(0xFF241D17), cornerRadius = w * 0.1f)
        drawRect(brush = verticalBodyBrush(Color(0xFFFFD98A), Color(0xFFB88936)), topLeft = Offset(left, size.height * 0.5f), size = Size(w, size.height * 0.12f))
        drawOval(color = Color(0xFFC5C5CE), topLeft = Offset(left, size.height * 0.32f), size = Size(w, size.height * 0.05f))
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.1f, alpha = 0.5f)
    }
}

@Composable
fun FigurineArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val headCenter = Offset(size.width * 0.5f, size.height * 0.38f)
        val headR = size.minDimension * 0.14f
        circleBody(headCenter, headR, Color(0xFFE8967F), Color(0xFF9C4A2E))
        rimLightCircle(headCenter, headR, alpha = 0.5f)
        val torsoTL = Offset(size.width * 0.38f, size.height * 0.48f)
        val torsoSize = Size(size.width * 0.24f, size.height * 0.24f)
        roundedBody(torsoTL, torsoSize, Color(0xFF7CB4FA), Color(0xFF241D17), cornerRadius = size.width * 0.06f)
        rimLight(torsoTL, torsoSize, cornerRadius = size.width * 0.06f, alpha = 0.4f)
        roundedBody(
            Offset(size.width * 0.34f, size.height * 0.72f), Size(size.width * 0.32f, size.height * 0.06f),
            Color(0xFF463B31), Color(0xFF17120D), cornerRadius = size.width * 0.02f,
        )
    }
}

@Composable
fun TrophyArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.34f
        val left = size.width * 0.5f - w / 2f
        drawArc(
            brush = verticalBodyBrush3(Color(0xFFFFE9A8), Color(0xFFF7D68C), Color(0xFFB88936)),
            startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(left, size.height * 0.3f), size = Size(w, w * 0.9f),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.4f), startAngle = 200f, sweepAngle = 50f, useCenter = false,
            topLeft = Offset(left, size.height * 0.3f), size = Size(w, w * 0.9f), style = Stroke(width = w * 0.04f, cap = StrokeCap.Round),
        )
        listOf(-1f, 1f).forEach { side ->
            drawArc(
                color = Color(0xFFB88936), startAngle = if (side < 0) 90f else -90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(left + w / 2f + side * w * 0.45f - w * 0.1f, size.height * 0.32f),
                size = Size(w * 0.2f, w * 0.3f), style = Stroke(width = w * 0.05f, cap = StrokeCap.Round),
            )
        }
        drawRect(color = Color(0xFFB88936), topLeft = Offset(size.width * 0.5f - w * 0.06f, size.height * 0.3f + w * 0.42f), size = Size(w * 0.12f, size.height * 0.14f))
        roundedBody(
            Offset(size.width * 0.5f - w * 0.28f, size.height * 0.3f + w * 0.42f + size.height * 0.14f), Size(w * 0.56f, size.height * 0.06f),
            Color(0xFFB88936), Color(0xFF7A4E2E), cornerRadius = w * 0.04f,
        )
    }
}

@Composable
fun BackpackArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val bodyTL = Offset(size.width * 0.28f, size.height * 0.3f)
        val bodySize = Size(size.width * 0.44f, size.height * 0.46f)
        roundedBody(bodyTL, bodySize, Color(0xFF7CB4FA), Color(0xFF241D17), cornerRadius = size.width * 0.14f)
        roundedBody(
            Offset(size.width * 0.38f, size.height * 0.4f), Size(size.width * 0.24f, size.height * 0.2f),
            Color(0xFF9CB48D), Color(0xFF4E6647), cornerRadius = size.width * 0.06f,
        )
        listOf(-1f, 1f).forEach { side ->
            drawArc(
                color = Color(0xFF463B31), startAngle = if (side < 0) 200f else -20f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(size.width * 0.5f + side * size.width * 0.14f - size.width * 0.04f, size.height * 0.24f),
                size = Size(size.width * 0.08f, size.height * 0.14f), style = Stroke(width = size.width * 0.02f, cap = StrokeCap.Round),
            )
        }
        rimLight(bodyTL, bodySize, cornerRadius = size.width * 0.14f, alpha = 0.4f)
    }
}
