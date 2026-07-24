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

private val wall = Color(0xFFB9C4D1)
private val wallShadow = Color(0xFF9FACBC)
private val floor = Color(0xFFC9BFA8)
private val floorShadow = Color(0xFFAFA284)
private val deskColor = Color(0xFF8B6A4A)
private val deskDark = Color(0xFF5C4530)
private val shelfColor = Color(0xFF6B5238)
private val windowSky = Color(0xFFCEE0EC)
private val windowFrame = Color(0xFF7A8A9A)
private val cabinetColor = Color(0xFF7A8A9A)
private val cabinetDark = Color(0xFF54626F)

private const val WALL_HEIGHT = 0.68f
private const val DESK_LEFT = 0.05f
private const val DESK_RIGHT = 0.52f
private const val DESK_TOP = 0.58f
private const val SHELF_LEFT = 0.62f
private const val SHELF_RIGHT = 0.96f
private const val SHELF_1_Y = 0.16f
private const val SHELF_2_Y = 0.30f
private const val WINDOW_LEFT = 0.08f
private const val WINDOW_RIGHT = 0.32f
private const val WINDOW_TOP = 0.10f
private const val WINDOW_BOTTOM = 0.40f
private const val CABINET_LEFT = 0.60f
private const val CABINET_RIGHT = 0.76f
private const val CABINET_TOP = 0.42f

val officeRoomArt = RoomArt(
    id = "office",
    labelRes = R.string.scene_office,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = Color(0xFF5C7A9C)),
    ambientMood = AmbientMood.COOL_FOCUS,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Desk surface — laptop + coffee mug (TABLE), lamp/pen cup/stapler (DESK).
        RoomSlot(0.1596f, 0.4631f, 0.1500f),
        RoomSlot(0.3260f, 0.5719f, 0.1300f),
        RoomSlot(0.4333f, 0.4162f, 0.1200f),
        RoomSlot(0.0422f, 0.6082f, 0.1100f),
        RoomSlot(0.4959f, 0.5773f, 0.1000f),
        // Shelf boards — binder, award plaque, book.
        RoomSlot(0.6769f, 0.2137f, 0.1200f),
        RoomSlot(0.8440f, 0.2359f, 0.1100f),
        RoomSlot(0.7501f, 0.3703f, 0.1100f),
        // Wall — clock and calendar.
        RoomSlot(0.9325f, 0.0995f, 0.1100f),
        RoomSlot(0.5266f, 0.1506f, 0.1000f),
        // Windowsill.
        RoomSlot(0.2431f, 0.2831f, 0.1300f),
        // Floor — chair, waste bin, backpack.
        RoomSlot(0.3400f, 0.8600f, 0.1600f),
        RoomSlot(0.5800f, 0.8800f, 0.1200f),
        RoomSlot(0.9000f, 0.8600f, 0.1400f),
    ),
    backdrop = { modifier -> OfficeBackdrop(modifier) },
)

@Composable
private fun OfficeBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.15f),
                radius = w * 0.5f,
            ),
            size = Size(w, wallHeight),
        )
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.Black, alpha = 0.03f, count = 36, seed = 301)

        drawRect(brush = Brush.verticalGradient(listOf(floor, floorShadow)), topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        woodGrainLines(Offset(0f, wallHeight), Size(w, h - wallHeight), color = Color.Black, alpha = 0.05f, lineCount = 5)
        drawLine(Color.Black.copy(alpha = 0.2f), Offset(0f, wallHeight), Offset(w, wallHeight), strokeWidth = 3f)

        // Window with a soft overcast-office sky.
        val winL = w * WINDOW_LEFT
        val winR = w * WINDOW_RIGHT
        val winT = h * WINDOW_TOP
        val winB = h * WINDOW_BOTTOM
        drawRoundRect(windowFrame, topLeft = Offset(winL - 8f, winT - 8f), size = Size(winR - winL + 16f, winB - winT + 16f), cornerRadius = CornerRadius(8f, 8f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(windowSky, Color(0xFFA9C2D6))),
            topLeft = Offset(winL, winT), size = Size(winR - winL, winB - winT), cornerRadius = CornerRadius(4f, 4f),
        )
        drawLine(windowFrame, Offset((winL + winR) / 2f, winT), Offset((winL + winR) / 2f, winB), strokeWidth = 4f)
        drawLine(windowFrame, Offset(winL, (winT + winB) / 2f), Offset(winR, (winT + winB) / 2f), strokeWidth = 4f)
        drawRoundRect(windowFrame, topLeft = Offset(winL - 10f, winB), size = Size(winR - winL + 20f, 8f), cornerRadius = CornerRadius(3f, 3f))

        // Filing cabinet against the right wall.
        val cabL = w * CABINET_LEFT
        val cabR = w * CABINET_RIGHT
        val cabT = h * CABINET_TOP
        drawOval(Color.Black.copy(alpha = 0.18f), topLeft = Offset(cabL - 4f, wallHeight - 8f), size = Size(cabR - cabL + 8f, 16f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(cabinetColor, cabinetDark)),
            topLeft = Offset(cabL, cabT), size = Size(cabR - cabL, wallHeight - cabT + 4f), cornerRadius = CornerRadius(4f, 4f),
        )
        listOf(0.28f, 0.55f, 0.82f).forEach { frac ->
            drawLine(Color.Black.copy(alpha = 0.25f), Offset(cabL + 4f, cabT + (wallHeight - cabT) * frac), Offset(cabR - 4f, cabT + (wallHeight - cabT) * frac), strokeWidth = 2f)
            drawCircle(Color(0xFFDCD2BE), radius = 3f, center = Offset((cabL + cabR) / 2f, cabT + (wallHeight - cabT) * frac - 8f))
        }

        // Shelf unit — two wall-mounted boards, right side.
        listOf(SHELF_1_Y, SHELF_2_Y).forEach { shelfYFraction ->
            val shelfY = h * shelfYFraction
            drawRoundRect(shelfColor, topLeft = Offset(w * SHELF_LEFT, shelfY), size = Size(w * (SHELF_RIGHT - SHELF_LEFT), 8f), cornerRadius = CornerRadius(3f, 3f))
            drawOval(Color.Black.copy(alpha = 0.12f), topLeft = Offset(w * SHELF_LEFT, shelfY + 8f), size = Size(w * (SHELF_RIGHT - SHELF_LEFT), 6f))
        }

        // Desk — a broad oak surface with a soft grounding shadow, left side.
        val deskL = w * DESK_LEFT
        val deskR = w * DESK_RIGHT
        val deskT = h * DESK_TOP
        drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(deskL - 6f, wallHeight - 6f), size = Size(deskR - deskL + 12f, 18f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(deskColor, deskDark)),
            topLeft = Offset(deskL, deskT), size = Size(deskR - deskL, wallHeight - deskT + 6f), cornerRadius = CornerRadius(6f, 6f),
        )
        woodGrainLines(Offset(deskL, deskT), Size(deskR - deskL, wallHeight - deskT), color = Color.Black, alpha = 0.08f, lineCount = 3)
        drawRoundRect(Color(0xFFA9825C), topLeft = Offset(deskL, deskT), size = Size(deskR - deskL, 6f), cornerRadius = CornerRadius(2f, 2f))

        // Soft vignette for depth.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.85f,
            ),
            size = Size(w, h),
        )
    }
}
