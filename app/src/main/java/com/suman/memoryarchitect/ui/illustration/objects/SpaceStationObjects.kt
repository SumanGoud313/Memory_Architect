package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.IdleAnimationKind
import com.suman.memoryarchitect.ui.illustration.ObjectArt
import com.suman.memoryarchitect.ui.illustration.register

/** Space Station's own objects — cool metallics and neon cyan/purple accents, the coldest
 * palette in the app, deliberately opposite the other rooms' warm wood tones. */
fun MutableMap<String, ObjectArt>.registerSpaceStationObjects() {
    register(ObjectArt("control_panel", R.string.object_control_panel, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> ControlPanelArt(m) }))
    register(ObjectArt("oxygen_tank", R.string.object_oxygen_tank, drawer = { m -> OxygenTankArt(m) }))
    register(ObjectArt("space_helmet", R.string.object_space_helmet, drawer = { m -> SpaceHelmetArt(m) }))
    register(ObjectArt("star_chart", R.string.object_star_chart, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> StarChartArt(m) }))
    register(ObjectArt("robot_companion", R.string.object_robot_companion, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> RobotCompanionArt(m) }))
    register(ObjectArt("food_pouch", R.string.object_food_pouch, drawer = { m -> FoodPouchArt(m) }))
    register(ObjectArt("communicator", R.string.object_communicator, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> CommunicatorArt(m) }))
    register(ObjectArt("hydroponic_pod", R.string.object_hydroponic_pod, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> HydroponicPodArt(m) }))
    register(ObjectArt("space_boots", R.string.object_space_boots, drawer = { m -> SpaceBootsArt(m) }))
    register(ObjectArt("satellite_model", R.string.object_satellite_model, drawer = { m -> SatelliteModelArt(m) }))
}

@Composable
fun ControlPanelArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.6f
        val h = size.height * 0.3f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.48f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFF4A5568), Color(0xFF232B34), cornerRadius = w * 0.06f)
        for (r in 0..1) {
            for (c in 0..3) {
                val cx = left + w * (0.14f + c * 0.24f)
                val cy = top + h * (0.3f + r * 0.4f)
                val neon = if ((r + c) % 2 == 0) Color(0xFF7CE0E8) else Color(0xFFB088E8)
                drawCircle(brush = Brush.radialGradient(listOf(neon, neon.copy(alpha = 0f)), center = Offset(cx, cy), radius = w * 0.06f), radius = w * 0.05f, center = Offset(cx, cy))
            }
        }
        rimLight(tl, sz, cornerRadius = w * 0.06f, alpha = 0.35f)
    }
}

@Composable
fun OxygenTankArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.3f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.32f
        val tl = Offset(left, top)
        val sz = Size(w, size.height * 0.46f)
        roundedBody(tl, sz, Color(0xFFE8E8EC), Color(0xFF9C9CA8), cornerRadius = w * 0.28f)
        drawRect(color = Color(0xFFE8674B), topLeft = Offset(left, top + sz.height * 0.36f), size = Size(w, sz.height * 0.14f))
        drawRoundRect(Color(0xFF7A8A9A), topLeft = Offset(left + w * 0.32f, top - size.height * 0.06f), size = Size(w * 0.36f, size.height * 0.08f), cornerRadius = CornerRadius(3f, 3f))
        rimLight(tl, sz, cornerRadius = w * 0.28f, alpha = 0.5f)
    }
}

@Composable
fun SpaceHelmetArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val center = Offset(size.width * 0.5f, size.height * 0.46f)
        val r = size.minDimension * 0.28f
        circleBody(center, r, Color(0xFFE8E8EC), Color(0xFF9C9CA8))
        drawOval(brush = Brush.radialGradient(listOf(Color(0xFFF0C875), Color(0xFF8B6A22)), center = center - Offset(r * 0.1f, r * 0.1f), radius = r), topLeft = center - Offset(r * 0.7f, r * 0.5f), size = Size(r * 1.4f, r * 1.1f))
        highlight(center - Offset(r * 0.5f, r * 0.4f), Size(r * 0.4f, r * 0.3f), alpha = 0.4f)
        drawRect(color = Color(0xFF7A8A9A), topLeft = Offset(center.x - r * 0.5f, center.y + r * 0.9f), size = Size(r, r * 0.3f))
        rimLightCircle(center, r, alpha = 0.5f)
    }
}

@Composable
fun StarChartArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.58f
        val h = size.height * 0.42f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.28f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFF232B34), Color(0xFF0D1420), cornerRadius = w * 0.04f)
        val points = listOf(0.2f to 0.3f, 0.4f to 0.55f, 0.6f to 0.25f, 0.75f to 0.6f, 0.5f to 0.75f)
        val positions = points.map { (dx, dy) -> Offset(left + w * dx, top + h * dy) }
        for (i in 0 until positions.size - 1) {
            drawLine(Color(0xFF7CE0E8).copy(alpha = 0.5f), positions[i], positions[i + 1], strokeWidth = 1.5f)
        }
        positions.forEach { p -> drawCircle(Color(0xFF7CE0E8), radius = 3f, center = p) }
        rimLight(tl, sz, cornerRadius = w * 0.04f, alpha = 0.3f)
    }
}

@Composable
fun RobotCompanionArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val bodyC = Offset(size.width * 0.5f, size.height * 0.52f)
        val r = size.minDimension * 0.2f
        circleBody(bodyC, r, Color(0xFFE8E8EC), Color(0xFF9C9CA8))
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFB088E8), Color(0xFF5A3E8A))), radius = r * 0.3f, center = bodyC)
        drawLine(Color(0xFF9C9CA8), bodyC - Offset(0f, r), bodyC - Offset(0f, r * 1.5f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF7CE0E8), radius = r * 0.12f, center = bodyC - Offset(0f, r * 1.5f))
        listOf(-1f, 1f).forEach { side -> drawLine(Color(0xFF9C9CA8), bodyC + Offset(side * r * 0.9f, 0f), bodyC + Offset(side * r * 1.3f, r * 0.4f), strokeWidth = r * 0.12f, cap = StrokeCap.Round) }
        rimLightCircle(bodyC, r, alpha = 0.5f)
    }
}

@Composable
fun FoodPouchArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.34f
        val h = size.height * 0.4f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.4f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFFE8E8EC), Color(0xFF9C9CA8), cornerRadius = w * 0.2f)
        drawRect(color = Color(0xFF9CB48D), topLeft = Offset(left + w * 0.14f, top + h * 0.32f), size = Size(w * 0.72f, h * 0.2f))
        drawLine(Color.Black.copy(alpha = 0.15f), Offset(left + w * 0.5f, top), Offset(left + w * 0.5f, top + h * 0.1f), strokeWidth = 3f)
        rimLight(tl, sz, cornerRadius = w * 0.2f, alpha = 0.4f)
    }
}

@Composable
fun CommunicatorArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val w = size.width * 0.28f
        val h = size.height * 0.44f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.3f
        val tl = Offset(left, top)
        val sz = Size(w, h)
        roundedBody(tl, sz, Color(0xFF4A5568), Color(0xFF232B34), cornerRadius = w * 0.16f)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF7CE0E8), Color(0xFF7CE0E8).copy(alpha = 0f))), radius = w * 0.24f, center = Offset(left + w * 0.5f, top + h * 0.3f))
        for (i in 0..2) {
            drawLine(Color(0xFF9C9CA8), Offset(left + w * 0.2f, top + h * (0.55f + i * 0.12f)), Offset(left + w * 0.8f, top + h * (0.55f + i * 0.12f)), strokeWidth = 1.5f)
        }
        drawLine(Color(0xFF9C9CA8), Offset(left + w * 0.5f, top), Offset(left + w * 0.5f, top - h * 0.14f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
        rimLight(tl, sz, cornerRadius = w * 0.16f, alpha = 0.4f)
    }
}

@Composable
fun HydroponicPodArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        val r = size.minDimension * 0.22f
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9CB48D).copy(alpha = 0.4f), Color.Transparent), center = center, radius = r * 1.6f), radius = r * 1.6f, center = center)
        drawCircle(color = Color(0xFF7CB4FA).copy(alpha = 0.28f), radius = r, center = center, style = Stroke(width = r * 0.1f))
        drawCircle(color = Color(0xFF7CB4FA).copy(alpha = 0.12f), radius = r * 0.9f, center = center)
        listOf(-0.3f, 0f, 0.3f).forEach { dx ->
            drawLine(Color(0xFF9CB48D), center + Offset(dx * r, r * 0.6f), center + Offset(dx * r * 1.4f, -r * 0.5f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
        }
        rimLightCircle(center, r, alpha = 0.4f)
    }
}

@Composable
fun SpaceBootsArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        listOf(-1f, 1f).forEach { side ->
            val cx = size.width * 0.5f + side * size.width * 0.14f
            val tl = Offset(cx - size.width * 0.1f, size.height * 0.44f)
            val sz = Size(size.width * 0.2f, size.height * 0.3f)
            roundedBody(tl, sz, Color(0xFFE8E8EC), Color(0xFF9C9CA8), cornerRadius = size.width * 0.06f)
            drawRoundRect(Color(0xFF9C9CA8), topLeft = Offset(tl.x, tl.y + sz.height * 0.82f), size = Size(sz.width, sz.height * 0.24f), cornerRadius = CornerRadius(4f, 4f))
        }
    }
}

@Composable
fun SatelliteModelArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        roundedBody(center - Offset(size.width * 0.08f, size.width * 0.08f), Size(size.width * 0.16f, size.width * 0.16f), Color(0xFFE8E8EC), Color(0xFF9C9CA8), cornerRadius = size.width * 0.03f)
        listOf(-1f, 1f).forEach { side ->
            drawRect(brush = Brush.linearGradient(listOf(Color(0xFF3F5A8A), Color(0xFF7CB4FA))), topLeft = center + Offset(side * size.width * 0.1f - if (side < 0) size.width * 0.22f else 0f, -size.width * 0.06f), size = Size(size.width * 0.22f, size.width * 0.12f))
        }
        drawArc(Color(0xFFC5C5CE), startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = center + Offset(-size.width * 0.02f, -size.width * 0.28f), size = Size(size.width * 0.24f, size.width * 0.2f), style = Stroke(width = size.width * 0.02f))
        drawCircle(Color(0xFF7CE0E8), radius = 2f, center = center + Offset(size.width * 0.08f, -size.width * 0.24f))
    }
}
