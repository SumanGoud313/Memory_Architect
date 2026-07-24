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

private val wall = Color(0xFFF0E6D2)
private val wallShadow = Color(0xFFDFCEAE)
private val floor = Color(0xFFC9A87E)
private val floorShadow = Color(0xFFB08F5E)
private val cabinet = Color(0xFF8B5E3C)
private val cabinetDoor = Color(0xFF6B4529)
private val counter = Color(0xFF5C3A26)
private val counterTop = Color(0xFFE8D9C4)
private val tableColor = Color(0xFF7A5230)
private val windowSky = Color(0xFFAED4E8)

private const val CABINET_H = 0.20f
private const val OPEN_SHELF_Y = 0.26f
private const val WALL_HEIGHT = 0.60f
private const val COUNTER_TOP_Y = 0.46f
private const val TABLE_LEFT = 0.62f
private const val TABLE_RIGHT = 0.94f
private const val TABLE_TOP_Y = 0.68f

val kitchenRoomArt = RoomArt(
    id = "kitchen",
    labelRes = R.string.scene_kitchen,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = Color(0xFFE8674B)),
    ambientMood = AmbientMood.MORNING_LIGHT,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Counter run — the main prep surface, left-to-center.
        RoomSlot(0.0300f, 0.3807f, 0.1150f),
        RoomSlot(0.1451f, 0.5077f, 0.1198f),
        RoomSlot(0.2835f, 0.3915f, 0.1300f),
        RoomSlot(0.4373f, 0.4908f, 0.1248f),
        RoomSlot(0.5143f, 0.3370f, 0.1060f),
        // Open shelf below the cabinets.
        RoomSlot(0.0611f, 0.2211f, 0.1056f),
        RoomSlot(0.2211f, 0.2259f, 0.1100f),
        RoomSlot(0.3836f, 0.2437f, 0.1100f),
        // Windowsill above the sink end of the counter.
        RoomSlot(0.5225f, 0.1471f, 0.1200f),
        RoomSlot(0.6967f, 0.1605f, 0.1200f),
        // Kitchen table top.
        RoomSlot(0.5850f, 0.6400f, 0.1400f),
        RoomSlot(0.7840f, 0.6400f, 0.1400f),
        RoomSlot(0.9700f, 0.6400f, 0.1200f),
        // Far counter end, near the table.
        RoomSlot(0.6415f, 0.4255f, 0.1019f),
        // Floor near the table (a basket, a plant).
        RoomSlot(0.7000f, 0.9000f, 0.1500f),
        RoomSlot(0.9000f, 0.9000f, 0.1300f),
        // Cabinet-top display spots (above the doors).
        RoomSlot(0.1490f, 0.0853f, 0.1000f),
        RoomSlot(0.3176f, 0.0993f, 0.1000f),
    ),
    backdrop = { modifier -> KitchenBackdrop(modifier) },
)

@Composable
private fun KitchenBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        drawRect(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent), center = Offset(w * 0.6f, h * 0.1f), radius = w * 0.6f),
            size = Size(w, wallHeight),
        )
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.Black, alpha = 0.03f, count = 40, seed = 102)
        // Wall seam filling the bare stretch of wall to the right of the window.
        drawLine(Color.Black.copy(alpha = 0.08f), Offset(w * 0.84f, h * 0.04f), Offset(w * 0.84f, wallHeight - 4f), strokeWidth = 2f)

        drawRect(brush = Brush.verticalGradient(listOf(floor, floorShadow)), topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        woodGrainLines(Offset(0f, wallHeight), Size(w, h - wallHeight), color = Color.Black, alpha = 0.06f, lineCount = 6)

        // Skirting board.
        val skirtH = h * 0.018f
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFF0E6D2), Color(0xFFDFCEAE))), topLeft = Offset(0f, wallHeight - skirtH), size = Size(w, skirtH))
        drawLine(Color.Black.copy(alpha = 0.2f), Offset(0f, wallHeight), Offset(w, wallHeight), strokeWidth = 3f)

        // Floor rug under the kitchen table.
        val rugCenter = Offset(w * 0.78f, h * 0.86f)
        val rugSize = Size(w * 0.34f, h * 0.14f)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(Color(0xFFE8674B).copy(alpha = 0.35f), Color(0xFFB08F5E).copy(alpha = 0.3f)), center = rugCenter, radius = rugSize.width * 0.6f),
            topLeft = rugCenter - Offset(rugSize.width / 2f, rugSize.height / 2f), size = rugSize, cornerRadius = CornerRadius(rugSize.height * 0.4f, rugSize.height * 0.4f),
        )

        // Soft vignette for depth.
        drawRect(
            brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f),
            size = Size(w, h),
        )

        // Upper cabinet run with visible door seams + handles, spanning the left half.
        val cabinetH = h * CABINET_H
        val cabinetRight = w * 0.52f
        drawRect(brush = Brush.verticalGradient(listOf(cabinet, Color(0xFF5C3A26))), topLeft = Offset(0f, 0f), size = Size(cabinetRight, cabinetH))
        val doorCount = 3
        for (i in 0 until doorCount) {
            val doorX = cabinetRight / doorCount * i
            drawRoundRect(cabinetDoor, topLeft = Offset(doorX + 4f, 4f), size = Size(cabinetRight / doorCount - 8f, cabinetH - 8f), cornerRadius = CornerRadius(4f, 4f))
            drawCircle(Color(0xFFD8C39A), radius = 3f, center = Offset(doorX + cabinetRight / doorCount - 12f, cabinetH / 2f))
        }
        // Open shelf just below the cabinets.
        val shelfY = h * OPEN_SHELF_Y
        drawRoundRect(counter, topLeft = Offset(0f, shelfY), size = Size(cabinetRight, 7f), cornerRadius = CornerRadius(3f, 3f))
        drawOval(Color.Black.copy(alpha = 0.12f), topLeft = Offset(0f, shelfY + 7f), size = Size(cabinetRight, 6f))

        // Small window to the right of the cabinets.
        val winL = w * 0.55f
        val winR = w * 0.72f
        drawRoundRect(Color(0xFF8B6A4A), topLeft = Offset(winL - 6f, 0.08f * h - 6f), size = Size(winR - winL + 12f, 0.20f * h + 12f), cornerRadius = CornerRadius(6f, 6f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(windowSky, Color(0xFF8FA3C7))), topLeft = Offset(winL, 0.08f * h), size = Size(winR - winL, 0.20f * h))

        // Counter run along the wall/floor seam, with a lighter countertop strip on top.
        val counterH = h * 0.14f
        val counterTopY = h * COUNTER_TOP_Y
        drawRect(counter, topLeft = Offset(0f, counterTopY), size = Size(w * 0.58f, counterH))
        drawRoundRect(counterTop, topLeft = Offset(0f, counterTopY - 6f), size = Size(w * 0.58f, 8f), cornerRadius = CornerRadius(3f, 3f))

        // Kitchen table with two simple chair backs, on the floor to the right.
        val tableL = w * TABLE_LEFT
        val tableR = w * TABLE_RIGHT
        val tableTopY = h * TABLE_TOP_Y
        drawOval(Color.Black.copy(alpha = 0.18f), topLeft = Offset(tableL - 6f, wallHeight + (h - wallHeight) * 0.6f), size = Size(tableR - tableL + 12f, 16f))
        listOf(tableL + 10f, tableR - 10f).forEach { legX ->
            drawLine(tableColor, Offset(legX, tableTopY + 6f), Offset(legX, wallHeight + (h - wallHeight) * 0.6f), strokeWidth = 6f)
        }
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF9C7148), tableColor)),
            topLeft = Offset(tableL, tableTopY), size = Size(tableR - tableL, 12f), cornerRadius = CornerRadius(4f, 4f),
        )
    }
}
