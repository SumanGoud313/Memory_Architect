package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

fun MutableMap<String, ObjectArt>.registerKitchenObjects() {
    register(ObjectArt("kettle", R.string.object_kettle, idleAnimation = IdleAnimationKind.STEAM, drawer = { m -> KettleArt(m) }))
    register(ObjectArt("frying_pan", R.string.object_frying_pan, drawer = { m -> FryingPanArt(m) }))
    register(ObjectArt("cooking_pot", R.string.object_cooking_pot, idleAnimation = IdleAnimationKind.STEAM, drawer = { m -> CookingPotArt(m) }))
    register(ObjectArt("toaster", R.string.object_toaster, drawer = { m -> ToasterArt(m) }))
    register(ObjectArt("dinner_plate", R.string.object_dinner_plate, drawer = { m -> DinnerPlateArt(m) }))
    register(ObjectArt("mixing_bowl", R.string.object_mixing_bowl, drawer = { m -> MixingBowlArt(m) }))
    register(ObjectArt("cutting_board", R.string.object_cutting_board, drawer = { m -> CuttingBoardArt(m) }))
    register(ObjectArt("chef_knife", R.string.object_chef_knife, drawer = { m -> ChefKnifeArt(m) }))
    register(ObjectArt("whisk", R.string.object_whisk, drawer = { m -> WhiskArt(m) }))
    register(ObjectArt("spatula", R.string.object_spatula, drawer = { m -> SpatulaArt(m) }))
    register(ObjectArt("colander", R.string.object_colander, drawer = { m -> ColanderArt(m) }))
    register(ObjectArt("teapot", R.string.object_teapot, idleAnimation = IdleAnimationKind.STEAM, drawer = { m -> TeapotArt(m) }))
    register(ObjectArt("spice_jar", R.string.object_spice_jar, drawer = { m -> SpiceJarArt(m) }))
    register(ObjectArt("apple", R.string.object_apple, drawer = { m -> AppleArt(m) }))
    register(ObjectArt("banana", R.string.object_banana, drawer = { m -> BananaArt(m) }))
    register(ObjectArt("oven_mitt", R.string.object_oven_mitt, idleAnimation = IdleAnimationKind.SWAY, drawer = { m -> OvenMittArt(m) }))
    register(ObjectArt("blender", R.string.object_blender, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> BlenderArt(m) }))
}

@Composable
fun KettleArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.28f
        val center = Offset(size.width * 0.48f, size.height * 0.58f)
        circleBody(center, r, Color(0xFFF0876A), Color(0xFFA83E28))
        drawArc(
            color = Color(0xFF3A322A), startAngle = -60f, sweepAngle = 120f, useCenter = false,
            topLeft = center + Offset(r * 0.6f, -r * 0.5f), size = Size(r * 0.7f, r), style = Stroke(width = r * 0.14f, cap = StrokeCap.Round),
        )
        val spout = Path().apply {
            moveTo(center.x - r * 0.9f, center.y - r * 0.2f)
            lineTo(center.x - r * 1.5f, center.y - r * 0.6f)
            lineTo(center.x - r * 1.15f, center.y - r * 0.1f)
            close()
        }
        drawPath(spout, brush = verticalBodyBrush(Color(0xFFE8674B), Color(0xFFA83E28)))
        drawCircle(color = Color(0xFF3A322A), radius = r * 0.14f, center = center - Offset(0f, r * 0.95f))
        rimLightCircle(center, r, alpha = 0.5f)
        highlight(center - Offset(r * 0.4f, r * 0.4f), Size(r * 0.5f, r * 0.6f))
    }
}

@Composable
fun FryingPanArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.3f
        val center = Offset(size.width * 0.44f, size.height * 0.56f)
        circleBody(center, r, Color(0xFF827A6F), Color(0xFF241D17))
        drawCircle(color = Color(0xFF17120D), radius = r * 0.72f, center = center)
        drawLine(Color(0xFF241D17), center + Offset(r * 0.9f, 0f), center + Offset(r * 2.1f, 0f), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
        rimLightCircle(center, r, alpha = 0.4f)
        highlight(center - Offset(r * 0.3f, r * 0.4f), Size(r * 0.4f, r * 0.3f), alpha = 0.18f)
    }
}

@Composable
fun CookingPotArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.5f
        val h = size.height * 0.36f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.46f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFFD8D0C2), Color(0xFF5C5344), cornerRadius = w * 0.08f)
        drawOval(color = Color(0xFF241D17), topLeft = Offset(left, top - h * 0.08f), size = Size(w, h * 0.16f))
        listOf(-1f, 1f).forEach { side ->
            drawCircle(color = Color(0xFF3A322A), radius = w * 0.05f, center = Offset(size.width * 0.5f + side * w * 0.58f, top + h * 0.1f))
        }
        drawCircle(color = Color(0xFF3A322A), radius = w * 0.05f, center = Offset(size.width * 0.5f, top - h * 0.08f))
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.08f, alpha = 0.4f)
    }
}

@Composable
fun ToasterArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.56f
        val h = size.height * 0.38f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.46f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFFD8D0C2), Color(0xFF6B6157), cornerRadius = w * 0.12f)
        listOf(-0.22f, 0.22f).forEach { dx ->
            roundedBody(
                Offset(size.width * 0.5f + dx * w - w * 0.08f, top - h * 0.14f), Size(w * 0.16f, h * 0.2f),
                Color(0xFF3A322A), Color(0xFF211A14), cornerRadius = w * 0.02f,
            )
        }
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.12f, alpha = 0.55f)
        highlight(Offset(left + w * 0.1f, top + h * 0.15f), Size(w * 0.25f, h * 0.3f), alpha = 0.2f)
    }
}

@Composable
fun DinnerPlateArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.1f)
        val r = size.minDimension * 0.34f
        val center = Offset(size.width * 0.5f, size.height * 0.52f)
        circleBody(center, r, Color(0xFFFAF5EC), Color(0xFFB8AC97))
        drawCircle(color = Color(0xFFC9BFAF), radius = r * 0.66f, center = center, style = Stroke(width = r * 0.06f))
        rimLightCircle(center, r, alpha = 0.4f)
    }
}

@Composable
fun MixingBowlArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.34f
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        drawArc(
            brush = verticalBodyBrush3(Color(0xFFE8F0F7), Color(0xFFC5E0EE), Color(0xFF6F8F65)),
            startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = center - Offset(r, 0f), size = Size(r * 2f, r * 1.1f),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.4f), startAngle = 190f, sweepAngle = 60f, useCenter = false,
            topLeft = center - Offset(r, 0f), size = Size(r * 2f, r * 1.1f), style = Stroke(width = r * 0.06f, cap = StrokeCap.Round),
        )
        highlight(center - Offset(r * 0.5f, -r * 0.1f), Size(r * 0.5f, r * 0.3f))
    }
}

@Composable
fun CuttingBoardArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.08f)
        val boardTL = Offset(size.width * 0.18f, size.height * 0.3f)
        val boardSize = Size(size.width * 0.64f, size.height * 0.48f)
        roundedBody(boardTL, boardSize, Color(0xFFF0E6D2), Color(0xFFC9A87E), cornerRadius = size.width * 0.08f)
        woodGrainLines(boardTL, boardSize, color = Color(0xFF7A5230), alpha = 0.12f, lineCount = 4)
        drawCircle(color = Color(0xFFC9A87E), radius = size.width * 0.03f, center = Offset(size.width * 0.72f, size.height * 0.38f), style = Stroke(width = size.width * 0.015f))
        rimLight(boardTL, boardSize, cornerRadius = size.width * 0.08f, alpha = 0.4f)
    }
}

@Composable
fun ChefKnifeArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.08f)
        val path = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.68f)
            lineTo(size.width * 0.75f, size.height * 0.36f)
            lineTo(size.width * 0.8f, size.height * 0.42f)
            lineTo(size.width * 0.35f, size.height * 0.72f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFF4F4F7), Color(0xFF9C9CA8)), style = Fill)
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(size.width * 0.32f, size.height * 0.62f), end = Offset(size.width * 0.72f, size.height * 0.4f),
            strokeWidth = size.width * 0.012f,
        )
        drawLine(Color(0xFF3A322A), Offset(size.width * 0.2f, size.height * 0.72f), Offset(size.width * 0.32f, size.height * 0.62f), strokeWidth = size.width * 0.06f, cap = StrokeCap.Round)
    }
}

@Composable
fun WhiskArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.08f)
        drawLine(Color(0xFFB8AC97), Offset(size.width * 0.5f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.4f), strokeWidth = size.width * 0.08f, cap = StrokeCap.Round)
        listOf(-0.16f, -0.06f, 0.06f, 0.16f).forEach { dx ->
            drawArc(
                color = Color(0xFFC5C5CE), startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(size.width * (0.5f + dx) - size.width * 0.1f, size.height * 0.16f),
                size = Size(size.width * 0.2f, size.height * 0.3f), style = Stroke(width = size.width * 0.015f, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun SpatulaArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.08f)
        drawLine(Color(0xFF8B5E3C), Offset(size.width * 0.5f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.42f), strokeWidth = size.width * 0.07f, cap = StrokeCap.Round)
        val headTL = Offset(size.width * 0.32f, size.height * 0.2f)
        val headSize = Size(size.width * 0.36f, size.height * 0.24f)
        roundedBody(headTL, headSize, Color(0xFFE8967F), Color(0xFF9C4A2E), cornerRadius = size.width * 0.1f)
        rimLight(headTL, headSize, cornerRadius = size.width * 0.1f, alpha = 0.4f)
    }
}

@Composable
fun ColanderArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.32f
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        drawArc(
            brush = verticalBodyBrush(Color(0xFFD8D0C2), Color(0xFF6B6157)),
            startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = center - Offset(r, 0f), size = Size(r * 2f, r * 1.1f),
        )
        for (i in 0..3) {
            drawCircle(color = Color(0xFF3A322A), radius = r * 0.05f, center = center + Offset((i - 1.5f) * r * 0.4f, r * 0.35f))
        }
        drawArc(
            color = Color.White.copy(alpha = 0.35f), startAngle = 190f, sweepAngle = 55f, useCenter = false,
            topLeft = center - Offset(r, 0f), size = Size(r * 2f, r * 1.1f), style = Stroke(width = r * 0.06f, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun TeapotArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.3f
        val center = Offset(size.width * 0.46f, size.height * 0.56f)
        circleBody(center, r, Color(0xFF9CB48D), Color(0xFF3F5A3A))
        val spout = Path().apply {
            moveTo(center.x - r * 0.85f, center.y - r * 0.1f)
            lineTo(center.x - r * 1.5f, center.y - r * 0.55f)
            lineTo(center.x - r * 1.1f, center.y + r * 0.05f)
            close()
        }
        drawPath(spout, brush = verticalBodyBrush(Color(0xFF7CA271), Color(0xFF3F5A3A)))
        drawArc(
            color = Color(0xFF3F5A3A), startAngle = -70f, sweepAngle = 140f, useCenter = false,
            topLeft = center + Offset(r * 0.55f, -r * 0.45f), size = Size(r * 0.7f, r * 0.9f), style = Stroke(width = r * 0.14f, cap = StrokeCap.Round),
        )
        drawCircle(color = Color(0xFF3F5A3A), radius = r * 0.12f, center = center - Offset(0f, r * 0.95f))
        rimLightCircle(center, r, alpha = 0.5f)
        highlight(center - Offset(r * 0.4f, r * 0.4f), Size(r * 0.5f, r * 0.6f))
    }
}

@Composable
fun SpiceJarArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.32f
        val left = size.width * 0.5f - w / 2f
        val jarTL = Offset(left, size.height * 0.44f)
        val jarSize = Size(w, size.height * 0.38f)
        roundedBody(jarTL, jarSize, Color(0xFFE8F0F7), Color(0xFF9CBAD0), cornerRadius = w * 0.14f)
        roundedBody(Offset(size.width * 0.5f - w * 0.22f, size.height * 0.32f), Size(w * 0.44f, size.height * 0.14f), Color(0xFF8B5E3C), Color(0xFF5C3A26), cornerRadius = w * 0.05f)
        drawRect(brush = verticalBodyBrush(Color(0xFFF0876A), Color(0xFFA83E28)), topLeft = Offset(left + w * 0.15f, size.height * 0.58f), size = Size(w * 0.7f, size.height * 0.18f))
        speckle(Offset(left + w * 0.15f, size.height * 0.58f), Size(w * 0.7f, size.height * 0.18f), color = Color(0xFF5C1A0E), alpha = 0.3f, count = 6, seed = 41)
        rimLight(jarTL, jarSize, cornerRadius = w * 0.14f, alpha = 0.55f)
    }
}

@Composable
fun AppleArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.26f
        val center = Offset(size.width * 0.5f, size.height * 0.56f)
        circleBody(center, r, Color(0xFFF0876A), Color(0xFFA83E28))
        drawLine(Color(0xFF5C3A26), center - Offset(0f, r), center - Offset(0f, r * 1.4f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        drawPath(
            path = Path().apply {
                moveTo(center.x, center.y - r * 1.15f)
                quadraticTo(center.x + r * 0.5f, center.y - r * 1.4f, center.x + r * 0.15f, center.y - r * 1.1f)
                close()
            },
            brush = verticalBodyBrush(Color(0xFF9CB48D), Color(0xFF4E6647)),
        )
        rimLightCircle(center, r, alpha = 0.5f)
        highlight(center - Offset(r * 0.4f, r * 0.4f), Size(r * 0.5f, r * 0.6f))
    }
}

@Composable
fun BananaArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val path = Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.68f)
            quadraticTo(size.width * 0.2f, size.height * 0.4f, size.width * 0.5f, size.height * 0.32f)
            quadraticTo(size.width * 0.72f, size.height * 0.28f, size.width * 0.7f, size.height * 0.38f)
            quadraticTo(size.width * 0.48f, size.height * 0.44f, size.width * 0.42f, size.height * 0.68f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush3(Color(0xFFF7DD7A), Color(0xFFF2C94C), Color(0xFFB88936)), style = Fill)
        drawLine(
            color = Color.Black.copy(alpha = 0.15f),
            start = Offset(size.width * 0.36f, size.height * 0.62f), end = Offset(size.width * 0.5f, size.height * 0.36f),
            strokeWidth = size.width * 0.01f,
        )
        highlight(Offset(size.width * 0.38f, size.height * 0.34f), Size(size.width * 0.16f, size.height * 0.1f), alpha = 0.4f)
    }
}

@Composable
fun OvenMittArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.7f)
            quadraticTo(size.width * 0.24f, size.height * 0.32f, size.width * 0.5f, size.height * 0.3f)
            quadraticTo(size.width * 0.76f, size.height * 0.32f, size.width * 0.7f, size.height * 0.7f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFE8967F), Color(0xFF9C4A2E)), style = Fill)
        fabricWeave(Offset(size.width * 0.3f, size.height * 0.32f), Size(size.width * 0.4f, size.height * 0.36f), color = Color.Black, alpha = 0.06f, lineCount = 4)
        drawRect(color = Color(0xFFFAF5EC), topLeft = Offset(size.width * 0.28f, size.height * 0.66f), size = Size(size.width * 0.44f, size.height * 0.08f))
    }
}

@Composable
fun BlenderArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.34f
        val left = size.width * 0.5f - w / 2f
        val path = Path().apply {
            moveTo(left, size.height * 0.32f)
            lineTo(left + w, size.height * 0.32f)
            lineTo(left + w * 0.8f, size.height * 0.7f)
            lineTo(left + w * 0.2f, size.height * 0.7f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFC5E0EE), Color(0xFF60A5FA)), style = Fill)
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(left + w * 0.18f, size.height * 0.36f), end = Offset(left + w * 0.24f, size.height * 0.64f),
            strokeWidth = size.width * 0.012f,
        )
        roundedBody(Offset(left + w * 0.1f, size.height * 0.7f), Size(w * 0.8f, size.height * 0.12f), Color(0xFF3A322A), Color(0xFF211A14), cornerRadius = w * 0.06f)
    }
}
