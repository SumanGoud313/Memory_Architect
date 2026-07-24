package com.suman.memoryarchitect.ui.illustration.objects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.IdleAnimationKind
import com.suman.memoryarchitect.ui.illustration.ObjectArt
import com.suman.memoryarchitect.ui.illustration.register

/** Objects reused across multiple rooms — registered once so the id stays globally unique. */
fun MutableMap<String, ObjectArt>.registerSharedObjects() {
    register(ObjectArt("book", R.string.object_book, drawer = { m -> BookArt(m) }))
    register(ObjectArt("coffee_mug", R.string.object_coffee_mug, idleAnimation = IdleAnimationKind.STEAM, drawer = { m -> CoffeeMugArt(m) }))
    register(ObjectArt("laptop", R.string.object_laptop, idleAnimation = IdleAnimationKind.PULSE, drawer = { m -> LaptopArt(m) }))
    register(ObjectArt("potted_plant", R.string.object_potted_plant, idleAnimation = IdleAnimationKind.SWAY, drawer = { m -> PottedPlantArt(m) }))
}

@Composable
fun BookArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val w = size.width * 0.66f
        val h = size.height * 0.14f
        val left = size.width * 0.17f
        val top = size.height * 0.62f
        roundedBody(Offset(left, top), Size(w, h), Color(0xFFE8674B), Color(0xFFA83E28), cornerRadius = h * 0.26f)
        roundedBody(Offset(left + w * 0.06f, top - h * 0.9f), Size(w * 0.9f, h * 0.9f), Color(0xFF8FA383), Color(0xFF4E6647), cornerRadius = h * 0.26f)
        val topBookTL = Offset(left + w * 0.12f, top - h * 1.7f)
        val topBookSize = Size(w * 0.8f, h * 0.9f)
        roundedBody(topBookTL, topBookSize, Color(0xFFF0C875), Color(0xFFB88936), cornerRadius = h * 0.26f)
        matteNoise(topBookTL, topBookSize, color = Color(0xFF7A5A1E), alpha = 0.1f, count = 8, seed = 11)
        rimLight(topBookTL, topBookSize, cornerRadius = h * 0.26f, alpha = 0.55f)
        highlight(Offset(left + w * 0.15f, top - h * 1.6f), Size(w * 0.35f, h * 0.5f))
    }
}

@Composable
fun CoffeeMugArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val bodyW = size.width * 0.42f
        val bodyH = size.height * 0.4f
        val left = size.width * 0.5f - bodyW / 2f
        val top = size.height * 0.42f
        val bodyTL = Offset(left, top)
        val bodySize = Size(bodyW, bodyH)
        roundedBody(bodyTL, bodySize, Color(0xFFF7F1E8), Color(0xFFB8AC97), cornerRadius = bodyW * 0.16f)
        drawArc(
            color = Color(0xFFB8AC97),
            startAngle = -70f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(left + bodyW * 0.78f, top + bodyH * 0.12f),
            size = Size(bodyW * 0.5f, bodyH * 0.55f),
            style = Stroke(width = bodyW * 0.1f),
        )
        drawOval(color = Color(0xFFDCD2BE), topLeft = Offset(left + bodyW * 0.08f, top - bodyH * 0.03f), size = Size(bodyW * 0.84f, bodyH * 0.1f))
        rimLight(bodyTL, bodySize, cornerRadius = bodyW * 0.16f, alpha = 0.6f)
        highlight(Offset(left + bodyW * 0.1f, top + bodyH * 0.08f), Size(bodyW * 0.28f, bodyH * 0.4f))
    }
}

@Composable
fun LaptopArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val baseW = size.width * 0.72f
        val baseH = size.height * 0.08f
        val left = size.width * 0.14f
        val baseTop = size.height * 0.66f
        roundedBody(Offset(left, baseTop), Size(baseW, baseH), Color(0xFFDCD2BE), Color(0xFF88806F), cornerRadius = baseH * 0.4f)
        drawOval(
            brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width * 0.5f, baseTop + baseH), radius = baseW * 0.5f),
            topLeft = Offset(left, baseTop + baseH * 0.4f),
            size = Size(baseW, baseH * 0.6f),
        )
        val screenW = baseW * 0.86f
        val screenH = size.height * 0.42f
        val screenLeft = left + (baseW - screenW) / 2f
        val screenTop = baseTop - screenH
        val screenTL = Offset(screenLeft, screenTop)
        val screenSize = Size(screenW, screenH)
        roundedBody(screenTL, screenSize, Color(0xFF463B31), Color(0xFF1A1510), cornerRadius = screenW * 0.06f)
        roundedBody(
            Offset(screenLeft + screenW * 0.08f, screenTop + screenH * 0.1f),
            Size(screenW * 0.84f, screenH * 0.72f),
            Color(0xFFF0A876),
            Color(0xFFB8623A),
            cornerRadius = screenW * 0.03f,
        )
        rimLight(screenTL, screenSize, cornerRadius = screenW * 0.06f, alpha = 0.4f)
    }
}

@Composable
fun PottedPlantArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        contactShadow()
        val potW = size.width * 0.4f
        val potH = size.height * 0.24f
        val potLeft = size.width * 0.5f - potW / 2f
        val potTop = size.height * 0.66f
        val potTL = Offset(potLeft, potTop)
        val potSize = Size(potW, potH)
        roundedBody(potTL, potSize, Color(0xFFC9713F), Color(0xFF6E3D22), cornerRadius = potW * 0.08f)
        drawLine(Color.Black.copy(alpha = 0.14f), Offset(potLeft + potW * 0.06f, potTop + potH * 0.36f), Offset(potLeft + potW * 0.94f, potTop + potH * 0.36f), strokeWidth = potH * 0.05f)
        val leafColors = listOf(
            Color(0xFF9CB48D) to Color(0xFF4E6647),
            Color(0xFF7CA271) to Color(0xFF34492F),
            Color(0xFF8FA383) to Color(0xFF3F5A3A),
        )
        val stemBaseX = size.width * 0.5f
        val stemBaseY = potTop
        listOf(-0.32f, -0.1f, 0.14f, 0.34f).forEachIndexed { i, dx ->
            val tipX = stemBaseX + dx * size.width
            val tipY = stemBaseY - size.height * (0.42f + (i % 2) * 0.08f)
            val (light, dark) = leafColors[i % leafColors.size]
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(stemBaseX, stemBaseY)
                    quadraticTo(tipX - size.width * 0.06f, (stemBaseY + tipY) / 2f, tipX, tipY)
                    quadraticTo(tipX + size.width * 0.06f, (stemBaseY + tipY) / 2f, stemBaseX, stemBaseY)
                    close()
                },
                brush = verticalBodyBrush(light, dark),
                style = Fill,
            )
        }
        rimLight(potTL, potSize, cornerRadius = potW * 0.08f, alpha = 0.4f)
        highlight(Offset(potLeft + potW * 0.1f, potTop + potH * 0.15f), Size(potW * 0.3f, potH * 0.4f))
    }
}
