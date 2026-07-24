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

private val wall = Color(0xFFE6C9AE)
private val wallShadow = Color(0xFFC79A6F)
private val floor = Color(0xFF8B5E3C)
private val floorShadow = Color(0xFF6B4529)
private val shelf = Color(0xFF5C3A26)
private val counter = Color(0xFF3A2818)
private val counterTop = Color(0xFFD8B98C)
private val tableColor = Color(0xFF5C3A26)
private val windowSky = Color(0xFFE8D9C4)

private const val WALL_HEIGHT = 0.66f
private const val SHELF_1_Y = 0.16f
private const val SHELF_2_Y = 0.30f
private const val COUNTER_TOP_Y = 0.50f
private const val TABLE_LEFT = 0.60f
private const val TABLE_RIGHT = 0.90f
private const val TABLE_TOP_Y = 0.72f

val coffeeShopRoomArt = RoomArt(
    id = "coffee_shop",
    labelRes = R.string.scene_coffee_shop,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = Color(0xFFC79A46)),
    ambientMood = AmbientMood.WARM_GLOW,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Counter run — where the espresso machine, cups, and pastries live.
        RoomSlot(0.0300f, 0.4414f, 0.1500f),
        RoomSlot(0.2008f, 0.5553f, 0.1400f),
        RoomSlot(0.3854f, 0.5019f, 0.1300f),
        RoomSlot(0.4979f, 0.3591f, 0.1198f),
        RoomSlot(0.6369f, 0.4559f, 0.1106f),
        // Wall shelves — bean bags, menu, jars.
        RoomSlot(0.0797f, 0.1486f, 0.1100f),
        RoomSlot(0.2429f, 0.1647f, 0.1100f),
        RoomSlot(0.1486f, 0.2970f, 0.1100f),
        RoomSlot(0.3095f, 0.3277f, 0.1100f),
        // Table top (café seating area).
        RoomSlot(0.6430f, 0.6800f, 0.1400f),
        RoomSlot(0.8359f, 0.6772f, 0.1300f),
        // Windowsill.
        RoomSlot(0.4079f, 0.1281f, 0.1200f),
        RoomSlot(0.5809f, 0.1537f, 0.1200f),
        // Floor spots — bar stool base area, sacks, plant.
        RoomSlot(0.7136f, 0.9017f, 0.1500f),
        RoomSlot(0.9074f, 0.8511f, 0.1300f),
        RoomSlot(0.0500f, 0.9000f, 0.1400f),
    ),
    backdrop = { modifier -> CoffeeShopBackdrop(modifier) },
)

@Composable
private fun CoffeeShopBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        drawRect(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(w * 0.46f, h * 0.15f), radius = w * 0.45f),
            size = Size(w, wallHeight),
        )
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.Black, alpha = 0.03f, count = 40, seed = 103)
        // Wall seam filling the bare stretch of wall to the right of the window/shelves.
        drawLine(Color.Black.copy(alpha = 0.08f), Offset(w * 0.60f, h * 0.05f), Offset(w * 0.60f, wallHeight - 4f), strokeWidth = 2f)

        drawRect(brush = Brush.verticalGradient(listOf(floor, floorShadow)), topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        woodGrainLines(Offset(0f, wallHeight), Size(w, h - wallHeight), color = Color.Black, alpha = 0.07f, lineCount = 6)

        // Skirting board.
        val skirtH = h * 0.018f
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFE6C9AE), Color(0xFFC79A6F))), topLeft = Offset(0f, wallHeight - skirtH), size = Size(w, skirtH))
        drawLine(Color.Black.copy(alpha = 0.25f), Offset(0f, wallHeight), Offset(w, wallHeight), strokeWidth = 3f)

        // Floor rug beneath the café seating area.
        val rugCenter = Offset(w * 0.76f, h * 0.87f)
        val rugSize = Size(w * 0.38f, h * 0.15f)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(Color(0xFFC79A46).copy(alpha = 0.4f), Color(0xFF6B4529).copy(alpha = 0.35f)), center = rugCenter, radius = rugSize.width * 0.6f),
            topLeft = rugCenter - Offset(rugSize.width / 2f, rugSize.height / 2f), size = rugSize, cornerRadius = CornerRadius(rugSize.height * 0.4f, rugSize.height * 0.4f),
        )

        // Soft vignette for depth.
        drawRect(
            brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f),
            size = Size(w, h),
        )

        // Two wood shelf boards, evoking a café back-bar.
        listOf(SHELF_1_Y, SHELF_2_Y).forEach { yFraction ->
            val y = h * yFraction
            drawRoundRect(shelf, topLeft = Offset(w * 0.08f, y), size = Size(w * 0.30f, 8f), cornerRadius = CornerRadius(3f, 3f))
            drawOval(Color.Black.copy(alpha = 0.15f), topLeft = Offset(w * 0.08f, y + 8f), size = Size(w * 0.30f, 6f))
        }

        // Small window for warm daylight, center-wall.
        val winL = w * 0.38f
        val winR = w * 0.56f
        drawRoundRect(Color(0xFF3A2818), topLeft = Offset(winL - 6f, 0.06f * h - 6f), size = Size(winR - winL + 12f, 0.20f * h + 12f), cornerRadius = CornerRadius(6f, 6f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(windowSky, Color(0xFFC9A97D))), topLeft = Offset(winL, 0.06f * h), size = Size(winR - winL, 0.20f * h))

        // Counter along the bottom of the wall — the main serving surface.
        val counterH = h * 0.16f
        val counterTopY = h * COUNTER_TOP_Y
        drawRect(counter, topLeft = Offset(0f, counterTopY), size = Size(w * 0.60f, counterH))
        drawRoundRect(counterTop, topLeft = Offset(0f, counterTopY - 6f), size = Size(w * 0.60f, 8f), cornerRadius = CornerRadius(3f, 3f))

        // Café table on the floor, right side.
        val tableL = w * TABLE_LEFT
        val tableR = w * TABLE_RIGHT
        val tableTopY = h * TABLE_TOP_Y
        val tableBaseY = wallHeight + (h - wallHeight) * 0.55f
        drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset((tableL + tableR) / 2f - 24f, tableBaseY), size = Size(48f, 14f))
        drawLine(tableColor, Offset((tableL + tableR) / 2f, tableTopY + 6f), Offset((tableL + tableR) / 2f, tableBaseY), strokeWidth = 7f)
        drawOval(
            brush = Brush.verticalGradient(listOf(Color(0xFF9C7148), tableColor)),
            topLeft = Offset(tableL, tableTopY), size = Size(tableR - tableL, 14f),
        )
    }
}
