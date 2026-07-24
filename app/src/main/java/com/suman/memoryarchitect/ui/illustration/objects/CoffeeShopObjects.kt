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

/** Coffee Shop's own objects — espresso_machine/coffee_grinder/pendant_lamp are its hero pieces. */
fun MutableMap<String, ObjectArt>.registerCoffeeShopObjects() {
    register(ObjectArt("espresso_machine", R.string.object_espresso_machine, idleAnimation = IdleAnimationKind.STEAM, drawer = { m -> EspressoMachineArt(m) }))
    register(ObjectArt("latte_cup", R.string.object_latte_cup, idleAnimation = IdleAnimationKind.STEAM, drawer = { m -> LatteCupArt(m) }))
    register(ObjectArt("pastry_croissant", R.string.object_pastry_croissant, drawer = { m -> CroissantArt(m) }))
    register(ObjectArt("coffee_bean_bag", R.string.object_coffee_bean_bag, drawer = { m -> CoffeeBeanBagArt(m) }))
    register(ObjectArt("coffee_grinder", R.string.object_coffee_grinder, idleAnimation = IdleAnimationKind.ROTATE, drawer = { m -> CoffeeGrinderArt(m) }))
    register(ObjectArt("chalkboard_menu", R.string.object_chalkboard_menu, drawer = { m -> ChalkboardMenuArt(m) }))
    register(ObjectArt("bar_stool", R.string.object_bar_stool, drawer = { m -> BarStoolArt(m) }))
    register(ObjectArt("pendant_lamp", R.string.object_pendant_lamp, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> PendantLampArt(m) }))
    register(ObjectArt("milk_frother_pitcher", R.string.object_milk_frother_pitcher, drawer = { m -> MilkFrotherPitcherArt(m) }))
    register(ObjectArt("tip_jar", R.string.object_tip_jar, drawer = { m -> TipJarArt(m) }))
    register(ObjectArt("sugar_shaker", R.string.object_sugar_shaker, drawer = { m -> SugarShakerArt(m) }))
    register(ObjectArt("napkin_holder", R.string.object_napkin_holder, drawer = { m -> NapkinHolderArt(m) }))
}

@Composable
fun EspressoMachineArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.6f
        val h = size.height * 0.42f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.38f
        val bodyTL = Offset(left, top)
        val bodySize = Size(w, h)
        roundedBody(bodyTL, bodySize, Color(0xFF827A6F), Color(0xFF241D17), cornerRadius = w * 0.08f)
        roundedBody(Offset(left + w * 0.15f, top - h * 0.18f), Size(w * 0.7f, h * 0.2f), Color(0xFFD8D0C2), Color(0xFF948977), cornerRadius = w * 0.04f)
        drawRect(color = Color(0xFF211A14), topLeft = Offset(left + w * 0.35f, top + h * 0.9f), size = Size(w * 0.06f, h * 0.3f))
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.08f, alpha = 0.45f)
        highlight(Offset(left + w * 0.1f, top + h * 0.1f), Size(w * 0.25f, h * 0.3f), alpha = 0.2f)
    }
}

@Composable
fun LatteCupArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.4f
        val left = size.width * 0.5f - w / 2f
        val top = size.height * 0.44f
        val path = Path().apply {
            moveTo(left, top)
            lineTo(left + w, top)
            lineTo(left + w * 0.86f, top + size.height * 0.32f)
            lineTo(left + w * 0.14f, top + size.height * 0.32f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFFAF5EC), Color(0xFFB8AC97)), style = Fill)
        drawOval(brush = verticalBodyBrush(Color(0xFF9C6B3E), Color(0xFF6B4529)), topLeft = Offset(left, top - size.height * 0.03f), size = Size(w, size.height * 0.07f))
        drawOval(color = Color(0xFFFAF5EC).copy(alpha = 0.4f), topLeft = Offset(left + w * 0.1f, top - size.height * 0.02f), size = Size(w * 0.3f, size.height * 0.04f))
        drawArc(
            color = Color(0xFFB8AC97), startAngle = -70f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(left + w * 0.9f, top + size.height * 0.02f), size = Size(w * 0.4f, size.height * 0.18f), style = Stroke(width = w * 0.08f, cap = StrokeCap.Round),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(left + w * 0.2f, top + size.height * 0.06f), end = Offset(left + w * 0.16f, top + size.height * 0.26f),
            strokeWidth = w * 0.02f,
        )
    }
}

@Composable
fun CroissantArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.58f)
            quadraticTo(size.width * 0.3f, size.height * 0.32f, size.width * 0.5f, size.height * 0.36f)
            quadraticTo(size.width * 0.7f, size.height * 0.32f, size.width * 0.8f, size.height * 0.58f)
            quadraticTo(size.width * 0.6f, size.height * 0.5f, size.width * 0.5f, size.height * 0.56f)
            quadraticTo(size.width * 0.4f, size.height * 0.5f, size.width * 0.2f, size.height * 0.58f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush3(Color(0xFFF7DD7A), Color(0xFFF2C94C), Color(0xFF9C7148)), style = Fill)
        listOf(0.3f, 0.5f, 0.7f).forEach { fx ->
            drawLine(
                color = Color(0xFF9C7148).copy(alpha = 0.5f),
                start = Offset(size.width * fx, size.height * 0.4f), end = Offset(size.width * fx - size.width * 0.04f, size.height * 0.52f),
                strokeWidth = size.width * 0.012f,
            )
        }
        highlight(Offset(size.width * 0.32f, size.height * 0.36f), Size(size.width * 0.18f, size.height * 0.1f), alpha = 0.35f)
    }
}

@Composable
fun CoffeeBeanBagArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.5f
        val left = size.width * 0.5f - w / 2f
        val path = Path().apply {
            moveTo(left + w * 0.1f, size.height * 0.34f)
            lineTo(left + w * 0.9f, size.height * 0.34f)
            lineTo(left + w, size.height * 0.72f)
            lineTo(left, size.height * 0.72f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFF9C6B3E), Color(0xFF4A2E18)), style = Fill)
        fabricWeave(Offset(left, size.height * 0.34f), Size(w, size.height * 0.38f), color = Color.Black, alpha = 0.08f, lineCount = 5)
        drawRect(color = Color(0xFFD8B98C), topLeft = Offset(left + w * 0.2f, size.height * 0.28f), size = Size(w * 0.6f, size.height * 0.08f))
        listOf(-0.1f, 0.08f).forEach { dx ->
            drawOval(color = Color(0xFF241D17), topLeft = Offset(size.width * (0.5f + dx), size.height * 0.5f), size = Size(w * 0.14f, w * 0.1f))
        }
    }
}

@Composable
fun CoffeeGrinderArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val r = size.minDimension * 0.2f
        val topCenter = Offset(size.width * 0.5f, size.height * 0.36f)
        circleBody(topCenter, r, Color(0xFFC5C5CE), Color(0xFF6B6157))
        val baseTL = Offset(size.width * 0.5f - r * 0.7f, size.height * 0.44f)
        val baseSize = Size(r * 1.4f, size.height * 0.28f)
        roundedBody(baseTL, baseSize, Color(0xFF9C6B3E), Color(0xFF4A2E18), cornerRadius = r * 0.2f)
        drawCircle(color = Color(0xFF3A322A), radius = r * 0.12f, center = topCenter)
        rimLightCircle(topCenter, r, alpha = 0.5f)
        rimLight(baseTL, baseSize, cornerRadius = r * 0.2f, alpha = 0.35f)
    }
}

@Composable
fun ChalkboardMenuArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow(bottomInsetFraction = 0.06f)
        val frameTL = Offset(size.width * 0.16f, size.height * 0.16f)
        val frameSize = Size(size.width * 0.68f, size.height * 0.6f)
        roundedBody(frameTL, frameSize, Color(0xFF8B5E3C), Color(0xFF3A2818), cornerRadius = size.width * 0.05f)
        woodGrainLines(frameTL, frameSize, color = Color.Black, alpha = 0.14f, lineCount = 3)
        val innerLeft = size.width * 0.22f
        val innerTop = size.height * 0.22f
        drawRect(color = Color(0xFF2A2420), topLeft = Offset(innerLeft, innerTop), size = Size(size.width * 0.56f, size.height * 0.48f))
        for (i in 0..2) {
            drawLine(
                Color(0xFFF7F1E8).copy(alpha = 0.6f),
                Offset(innerLeft + size.width * 0.06f, innerTop + size.height * (0.14f + i * 0.13f)),
                Offset(innerLeft + size.width * (0.3f - i * 0.05f), innerTop + size.height * (0.14f + i * 0.13f)),
                strokeWidth = size.width * 0.015f,
                cap = StrokeCap.Round,
            )
        }
        rimLight(frameTL, frameSize, cornerRadius = size.width * 0.05f, alpha = 0.4f)
    }
}

@Composable
fun BarStoolArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val seatW = size.width * 0.5f
        val seatTL = Offset(size.width * 0.5f - seatW / 2f, size.height * 0.36f)
        val seatSize = Size(seatW, size.height * 0.14f)
        roundedBody(seatTL, seatSize, Color(0xFF9C6B3E), Color(0xFF4A2E18), cornerRadius = seatW * 0.3f)
        listOf(-0.2f, 0.2f).forEach { dx ->
            drawLine(
                Color(0xFF3A322A),
                Offset(size.width * (0.5f + dx), size.height * 0.48f),
                Offset(size.width * (0.5f + dx * 0.6f), size.height * 0.78f),
                strokeWidth = size.width * 0.03f,
                cap = StrokeCap.Round,
            )
        }
        drawLine(Color(0xFF3A322A), Offset(size.width * 0.34f, size.height * 0.64f), Offset(size.width * 0.66f, size.height * 0.64f), strokeWidth = size.width * 0.02f)
        rimLight(seatTL, seatSize, cornerRadius = seatW * 0.3f, alpha = 0.4f)
    }
}

@Composable
fun PendantLampArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        drawLine(Color(0xFF6B6157), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height * 0.32f), strokeWidth = size.width * 0.02f)
        val shadeTop = Offset(size.width * 0.32f, size.height * 0.32f)
        val path = Path().apply {
            moveTo(shadeTop.x, shadeTop.y)
            lineTo(shadeTop.x + size.width * 0.36f, shadeTop.y)
            lineTo(shadeTop.x + size.width * 0.28f, shadeTop.y + size.height * 0.2f)
            lineTo(shadeTop.x + size.width * 0.08f, shadeTop.y + size.height * 0.2f)
            close()
        }
        drawOval(
            brush = Brush.radialGradient(listOf(Color(0xFFFFD98A).copy(alpha = 0.5f), Color.Transparent), center = shadeTop + Offset(size.width * 0.18f, size.height * 0.24f), radius = size.width * 0.42f),
            topLeft = shadeTop + Offset(-size.width * 0.18f, size.height * 0.02f),
            size = Size(size.width * 0.72f, size.height * 0.42f),
        )
        drawPath(path, brush = verticalBodyBrush(Color(0xFFD8AC56), Color(0xFF7A4E2E)), style = Fill)
        rimLight(shadeTop, Size(size.width * 0.36f, size.height * 0.2f), cornerRadius = size.width * 0.03f, alpha = 0.5f)
        highlight(shadeTop + Offset(size.width * 0.14f, size.height * 0.24f), Size(size.width * 0.3f, size.height * 0.22f), color = Color(0xFFF2B441), alpha = 0.5f)
    }
}

@Composable
fun MilkFrotherPitcherArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val path = Path().apply {
            moveTo(size.width * 0.36f, size.height * 0.32f)
            lineTo(size.width * 0.64f, size.height * 0.32f)
            lineTo(size.width * 0.58f, size.height * 0.7f)
            lineTo(size.width * 0.42f, size.height * 0.7f)
            close()
        }
        drawPath(path, brush = verticalBodyBrush(Color(0xFFF4F4F7), Color(0xFF9C9CA8)), style = Fill)
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(size.width * 0.42f, size.height * 0.38f), end = Offset(size.width * 0.44f, size.height * 0.6f),
            strokeWidth = size.width * 0.012f,
        )
        drawLine(Color(0xFF9C9CA8), Offset(size.width * 0.64f, size.height * 0.4f), Offset(size.width * 0.78f, size.height * 0.32f), strokeWidth = size.width * 0.03f, cap = StrokeCap.Round)
        drawArc(
            color = Color(0xFF9C9CA8), startAngle = 100f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(size.width * 0.16f, size.height * 0.4f), size = Size(size.width * 0.2f, size.height * 0.24f), style = Stroke(width = size.width * 0.025f, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun TipJarArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.34f
        val left = size.width * 0.5f - w / 2f
        val jarTL = Offset(left, size.height * 0.4f)
        val jarSize = Size(w, size.height * 0.4f)
        roundedBody(jarTL, jarSize, Color(0xFFE8F0F7).copy(alpha = 0.55f), Color(0xFFAED4E8).copy(alpha = 0.55f), cornerRadius = w * 0.08f)
        listOf(0.32f, 0.5f, 0.65f).forEach { dx ->
            roundedBody(
                Offset(size.width * dx - w * 0.08f, size.height * 0.5f), Size(w * 0.16f, size.height * 0.04f),
                Color(0xFFF7D68C), Color(0xFFB88936), cornerRadius = w * 0.02f,
            )
        }
        rimLight(jarTL, jarSize, cornerRadius = w * 0.08f, alpha = 0.5f)
    }
}

@Composable
fun SugarShakerArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.26f
        val left = size.width * 0.5f - w / 2f
        val bodyTL = Offset(left, size.height * 0.42f)
        val bodySize = Size(w, size.height * 0.4f)
        roundedBody(bodyTL, bodySize, Color(0xFFFAF5EC), Color(0xFFB8AC97), cornerRadius = w * 0.12f)
        roundedBody(Offset(size.width * 0.5f - w * 0.18f, size.height * 0.32f), Size(w * 0.36f, size.height * 0.12f), Color(0xFFC5C5CE), Color(0xFF6B6157), cornerRadius = w * 0.04f)
        rimLight(bodyTL, bodySize, cornerRadius = w * 0.12f, alpha = 0.5f)
    }
}

@Composable
fun NapkinHolderArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        roundedBody(
            Offset(size.width * 0.28f, size.height * 0.5f), Size(size.width * 0.44f, size.height * 0.26f),
            Color(0xFFC5C5CE), Color(0xFF6B6157), cornerRadius = size.width * 0.03f,
        )
        val napkinTL = Offset(size.width * 0.34f, size.height * 0.4f)
        val napkinSize = Size(size.width * 0.32f, size.height * 0.16f)
        roundedBody(napkinTL, napkinSize, Color(0xFFFAF5EC), Color(0xFFB8AC97), cornerRadius = size.width * 0.02f)
        rimLight(napkinTL, napkinSize, cornerRadius = size.width * 0.02f, alpha = 0.5f)
    }
}
