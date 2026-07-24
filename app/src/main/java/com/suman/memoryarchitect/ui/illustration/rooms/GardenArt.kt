package com.suman.memoryarchitect.ui.illustration.rooms

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.AmbientMood
import com.suman.memoryarchitect.ui.illustration.RoomArt
import com.suman.memoryarchitect.ui.illustration.RoomPalette
import com.suman.memoryarchitect.ui.illustration.RoomSlot
import com.suman.memoryarchitect.ui.illustration.objects.matteNoise
import com.suman.memoryarchitect.ui.illustration.objects.woodGrainLines

private val sky = Color(0xFFAED4E8)
private val skyHorizon = Color(0xFFDCEBE0)
private val grass = Color(0xFF7CA271)
private val grassShadow = Color(0xFF4E6647)
private val fenceColor = Color(0xFFC9A97D)
private val fenceDark = Color(0xFF8B6A4A)
private val benchColor = Color(0xFF6B4A32)

// No walls or ceiling here — sky replaces the wall band, grass replaces the floor, and a low
// picket fence plays the "wall" furniture's role of anchoring the horizon line.
private const val SKY_HEIGHT = 0.36f
private const val FENCE_TOP = 0.30f
private const val BENCH_LEFT = 0.06f
private const val BENCH_RIGHT = 0.42f
private const val BENCH_TOP = 0.58f
private const val TABLE_LEFT = 0.62f
private const val TABLE_RIGHT = 0.92f
private const val TABLE_TOP = 0.62f

val gardenRoomArt = RoomArt(
    id = "garden",
    labelRes = R.string.scene_garden,
    palette = RoomPalette(wall = sky, wallShadow = skyHorizon, floor = grass, accent = Color(0xFFE0937F)),
    ambientMood = AmbientMood.MORNING_LIGHT,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Potting bench — flower pot, trowel, gloves.
        RoomSlot(0.0721f, 0.5114f, 0.1300f), RoomSlot(0.2433f, 0.5428f, 0.1100f), RoomSlot(0.4046f, 0.5258f, 0.1100f),
        // Picnic table — coffee mug, book, picnic basket.
        RoomSlot(0.6564f, 0.5227f, 0.1300f), RoomSlot(0.8039f, 0.6280f, 0.1200f), RoomSlot(0.9197f, 0.4892f, 0.1300f),
        // Fence hook — sunhat.
        RoomSlot(0.5000f, 0.2400f, 0.1100f),
        // Grass — potted plant, watering can, gnome, bird bath, wheelbarrow.
        RoomSlot(0.0618f, 0.9061f, 0.1500f), RoomSlot(0.2551f, 0.8619f, 0.1300f), RoomSlot(0.4401f, 0.9147f, 0.1400f),
        RoomSlot(0.6393f, 0.8971f, 0.1400f), RoomSlot(0.8437f, 0.9002f, 0.1500f),
    ),
    backdrop = { modifier -> GardenBackdrop(modifier) },
)

@Composable
private fun GardenBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val skyHeight = h * SKY_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(sky, skyHorizon), endY = skyHeight), size = Size(w, skyHeight))
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color(0xFFFFE9A8).copy(alpha = 0.6f), Color.Transparent), center = Offset(w * 0.82f, h * 0.1f), radius = w * 0.3f),
            radius = w * 0.3f, center = Offset(w * 0.82f, h * 0.1f),
        )
        // A few soft, rounded clouds.
        listOf(Offset(w * 0.2f, h * 0.12f) to w * 0.12f, Offset(w * 0.4f, h * 0.08f) to w * 0.09f).forEach { (c, r) ->
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = r, center = c)
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = r * 0.7f, center = c + Offset(r * 0.9f, r * 0.15f))
        }

        drawRect(brush = Brush.verticalGradient(listOf(grass, grassShadow)), topLeft = Offset(0f, skyHeight), size = Size(w, h - skyHeight))
        matteNoise(Offset(0f, skyHeight), Size(w, h - skyHeight), color = Color(0xFF34492F), alpha = 0.06f, count = 60, seed = 501)
        drawLine(Color.Black.copy(alpha = 0.14f), Offset(0f, skyHeight), Offset(w, skyHeight), strokeWidth = 2f)

        // Low picket fence along the horizon.
        val fenceY = h * FENCE_TOP
        var x = 0f
        while (x < w) {
            drawRoundRect(brush = Brush.verticalGradient(listOf(fenceColor, fenceDark)), topLeft = Offset(x, fenceY), size = Size(w * 0.02f, skyHeight - h * FENCE_TOP + h * 0.06f), cornerRadius = CornerRadius(2f, 2f))
            x += w * 0.05f
        }
        drawRect(fenceDark, topLeft = Offset(0f, fenceY + h * 0.03f), size = Size(w, h * 0.012f))

        // Flower beds along the fence base — decorative, not placeable.
        val flowerColors = listOf(Color(0xFFE8674B), Color(0xFFF0C875), Color(0xFF8FA383), Color(0xFFC98B7A))
        var fx = w * 0.02f
        var i = 0
        while (fx < w * 0.98f) {
            drawCircle(flowerColors[i % flowerColors.size].copy(alpha = 0.75f), radius = w * 0.008f, center = Offset(fx, skyHeight + h * 0.02f))
            fx += w * 0.035f
            i++
        }

        // Potting bench, left.
        val benchL = w * BENCH_LEFT
        val benchR = w * BENCH_RIGHT
        val benchT = h * BENCH_TOP
        drawOval(Color.Black.copy(alpha = 0.16f), topLeft = Offset(benchL - 4f, h * 0.9f), size = Size(benchR - benchL + 8f, 14f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(benchColor, Color(0xFF3F2E1E))), topLeft = Offset(benchL, benchT), size = Size(benchR - benchL, h * 0.32f), cornerRadius = CornerRadius(4f, 4f))
        woodGrainLines(Offset(benchL, benchT), Size(benchR - benchL, h * 0.06f), color = Color.Black, alpha = 0.1f, lineCount = 2)
        listOf(0.06f, 0.94f).forEach { frac -> drawRect(Color(0xFF3F2E1E), topLeft = Offset(benchL + (benchR - benchL) * frac - 4f, benchT), size = Size(8f, h * 0.32f)) }

        // Picnic table, right.
        val tblL = w * TABLE_LEFT
        val tblR = w * TABLE_RIGHT
        val tblT = h * TABLE_TOP
        drawOval(Color.Black.copy(alpha = 0.16f), topLeft = Offset(tblL - 4f, h * 0.92f), size = Size(tblR - tblL + 8f, 12f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFFC9A97D), fenceDark)), topLeft = Offset(tblL, tblT), size = Size(tblR - tblL, h * 0.28f), cornerRadius = CornerRadius(4f, 4f))
        woodGrainLines(Offset(tblL, tblT), Size(tblR - tblL, h * 0.05f), color = Color.Black, alpha = 0.08f, lineCount = 3)
        listOf(0.1f, 0.9f).forEach { frac -> drawLine(fenceDark, Offset(tblL + (tblR - tblL) * frac, tblT + h * 0.02f), Offset(tblL + (tblR - tblL) * frac, h * 0.92f), strokeWidth = w * 0.012f) }

        // Soft morning-light vignette.
        drawRect(
            brush = Brush.radialGradient(colors = listOf(Color(0xFFFFE9A8).copy(alpha = 0.1f), Color.Transparent), center = Offset(w * 0.8f, h * 0.15f), radius = w * 0.9f),
            size = Size(w, h),
        )
    }
}
