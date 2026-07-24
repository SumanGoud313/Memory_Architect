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
import kotlin.random.Random

private val wall = Color(0xFF3A4552)
private val wallShadow = Color(0xFF232B34)
private val floor = Color(0xFF2A323C)
private val floorShadow = Color(0xFF181D24)
private val consoleColor = Color(0xFF4A5568)
private val consoleDark = Color(0xFF232B34)
private val neonCyan = Color(0xFF7CE0E8)
private val neonPurple = Color(0xFFB088E8)

// The coldest, most exotic room in the pool — a metallic hull instead of drywall, a round
// viewport instead of a window, floor grating instead of carpet. Deliberately the visual
// opposite of the other rooms' warm-wood palettes to prove the engine holds up either way.
private const val WALL_HEIGHT = 0.66f
private const val CONSOLE_LEFT = 0.06f
private const val CONSOLE_RIGHT = 0.46f
private const val CONSOLE_TOP = 0.56f
private const val RACK_LEFT = 0.62f
private const val RACK_RIGHT = 0.96f
private const val RACK_ROW_1 = 0.14f
private const val RACK_ROW_2 = 0.32f
private const val VIEWPORT_CENTER_X = 0.78f
private const val VIEWPORT_CENTER_Y = 0.5f
private const val VIEWPORT_R = 0.16f

val spaceStationRoomArt = RoomArt(
    id = "space_station",
    labelRes = R.string.scene_space_station,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = neonCyan),
    ambientMood = AmbientMood.COOL_FOCUS,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Control console — panel, communicator, star chart.
        RoomSlot(0.0987f, 0.4575f, 0.1300f), RoomSlot(0.2607f, 0.5239f, 0.1100f), RoomSlot(0.4205f, 0.4687f, 0.1200f),
        // Storage rack — oxygen tank, food pouch, satellite model.
        RoomSlot(0.6850f, 0.1900f, 0.1200f), RoomSlot(0.8550f, 0.1900f, 0.1100f), RoomSlot(0.7700f, 0.3700f, 0.1100f),
        // Floor — helmet, robot companion, boots.
        RoomSlot(0.1304f, 0.8775f, 0.1600f), RoomSlot(0.3418f, 0.8936f, 0.1400f), RoomSlot(0.5239f, 0.9340f, 0.1200f),
        // Windowsill — viewport ledge, hydroponic pod.
        RoomSlot(0.7800f, 0.6800f, 0.1300f),
        // Crew table — coffee mug, logbook.
        RoomSlot(0.5876f, 0.7713f, 0.1200f), RoomSlot(0.7134f, 0.8875f, 0.1100f),
    ),
    backdrop = { modifier -> SpaceStationBackdrop(modifier) },
)

@Composable
private fun SpaceStationBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        // Riveted hull panel seams.
        var px = w * 0.04f
        while (px < w * 0.96f) {
            drawLine(Color.Black.copy(alpha = 0.18f), Offset(px, 0f), Offset(px, wallHeight), strokeWidth = 1.5f)
            px += w * 0.12f
        }
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.Black, alpha = 0.05f, count = 30, seed = 601)

        drawRect(brush = Brush.verticalGradient(listOf(floor, floorShadow)), topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        // Floor grating lines.
        var gy = wallHeight + h * 0.04f
        while (gy < h) {
            drawLine(Color.Black.copy(alpha = 0.22f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1.5f)
            gy += h * 0.06f
        }
        drawLine(neonCyan.copy(alpha = 0.4f), Offset(0f, wallHeight), Offset(w, wallHeight), strokeWidth = 2f)

        // Round viewport onto a starfield + distant planet.
        val vpCenter = Offset(w * VIEWPORT_CENTER_X, h * VIEWPORT_CENTER_Y)
        val vpR = w * VIEWPORT_R
        drawCircle(color = consoleDark, radius = vpR + 14f, center = vpCenter)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF0D1420), Color(0xFF060A10)), center = vpCenter, radius = vpR), radius = vpR, center = vpCenter)
        val starRandom = Random(77)
        repeat(26) {
            val angle = starRandom.nextFloat() * (Math.PI * 2).toFloat()
            val dist = starRandom.nextFloat() * vpR * 0.92f
            val starPos = vpCenter + Offset((kotlin.math.cos(angle) * dist), (kotlin.math.sin(angle) * dist))
            drawCircle(Color.White.copy(alpha = 0.4f + starRandom.nextFloat() * 0.5f), radius = 1.2f + starRandom.nextFloat() * 1.4f, center = starPos)
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE0937F), Color(0xFF8B3A3A)), center = vpCenter + Offset(vpR * 0.35f, vpR * 0.3f), radius = vpR * 0.36f),
            radius = vpR * 0.3f, center = vpCenter + Offset(vpR * 0.35f, vpR * 0.3f),
        )
        drawCircle(color = consoleColor, radius = vpR + 14f, center = vpCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f))
        drawCircle(color = neonCyan.copy(alpha = 0.35f), radius = vpR + 18f, center = vpCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        // Storage rack, right of the viewport.
        val rackL = w * RACK_LEFT
        val rackR = w * RACK_RIGHT
        drawRoundRect(brush = Brush.verticalGradient(listOf(consoleColor, consoleDark)), topLeft = Offset(rackL - 10f, h * 0.08f), size = Size(rackR - rackL + 20f, wallHeight - h * 0.08f + 4f), cornerRadius = CornerRadius(4f, 4f))
        listOf(RACK_ROW_1, RACK_ROW_2, 0.5f).forEach { rowFraction ->
            val y = h * rowFraction
            drawRoundRect(consoleDark, topLeft = Offset(rackL - 6f, y), size = Size(rackR - rackL + 12f, 6f), cornerRadius = CornerRadius(2f, 2f))
            drawCircle(neonPurple.copy(alpha = 0.6f), radius = 2.5f, center = Offset(rackL - 2f, y - 4f))
        }

        // Control console, left — angled desk with a blinking instrument strip.
        val conL = w * CONSOLE_LEFT
        val conR = w * CONSOLE_RIGHT
        val conT = h * CONSOLE_TOP
        drawOval(Color.Black.copy(alpha = 0.24f), topLeft = Offset(conL - 6f, wallHeight - 6f), size = Size(conR - conL + 12f, 16f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(consoleColor, consoleDark)), topLeft = Offset(conL, conT), size = Size(conR - conL, wallHeight - conT + 6f), cornerRadius = CornerRadius(6f, 6f))
        drawRoundRect(consoleDark, topLeft = Offset(conL + (conR - conL) * 0.06f, conT + (wallHeight - conT) * 0.1f), size = Size((conR - conL) * 0.88f, (wallHeight - conT) * 0.16f), cornerRadius = CornerRadius(3f, 3f))
        val lightRandom = Random(88)
        repeat(8) { i ->
            val lx = conL + (conR - conL) * (0.1f + i * 0.1f)
            val ly = conT + (wallHeight - conT) * 0.18f
            val c = if (lightRandom.nextFloat() > 0.5f) neonCyan else neonPurple
            drawCircle(c.copy(alpha = 0.8f), radius = 2.5f, center = Offset(lx, ly))
        }

        // Cool vignette + a faint cyan glow spilling from the viewport.
        drawRect(
            brush = Brush.radialGradient(colors = listOf(neonCyan.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.22f)), center = vpCenter, radius = w * 0.95f),
            size = Size(w, h),
        )
    }
}
