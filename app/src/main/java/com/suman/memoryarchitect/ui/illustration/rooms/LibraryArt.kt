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

private val wall = Color(0xFF6B4A32)
private val wallShadow = Color(0xFF4A3220)
private val floor = Color(0xFF5C4530)
private val floorShadow = Color(0xFF3F2E1E)
private val shelfWood = Color(0xFF7A5230)
private val shelfWoodDark = Color(0xFF4A3220)
private val rugColor = Color(0xFF8B3A3A)
private val chairColor = Color(0xFF6E3D22)

// The whole back wall is one dense built-in bookshelf — 3 rows x 3 columns of shelf slots is
// where "more shelves" lives visually, in contrast to Bedroom's two small boards.
private const val WALL_HEIGHT = 0.70f
private const val SHELF_LEFT = 0.06f
private const val SHELF_RIGHT = 0.66f
private const val SHELF_ROW_1 = 0.14f
private const val SHELF_ROW_2 = 0.32f
private const val SHELF_ROW_3 = 0.50f
private const val TABLE_LEFT = 0.70f
private const val TABLE_RIGHT = 0.97f
private const val TABLE_TOP = 0.56f
private const val WINDOW_LEFT = 0.70f
private const val WINDOW_RIGHT = 0.90f
private const val WINDOW_TOP = 0.08f
private const val WINDOW_BOTTOM = 0.32f

val libraryRoomArt = RoomArt(
    id = "library",
    labelRes = R.string.scene_library,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = rugColor),
    ambientMood = AmbientMood.WARM_GLOW,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Built-in bookshelf — 3 rows x 3 columns (9 shelf-preferred objects live here).
        RoomSlot(0.1400f, 0.1800f, 0.1000f), RoomSlot(0.2920f, 0.1798f, 0.1000f), RoomSlot(0.4440f, 0.1842f, 0.1000f),
        RoomSlot(0.1400f, 0.3600f, 0.1000f), RoomSlot(0.3000f, 0.3600f, 0.1000f), RoomSlot(0.4600f, 0.3600f, 0.1000f),
        RoomSlot(0.1400f, 0.5400f, 0.1000f), RoomSlot(0.3000f, 0.5400f, 0.1000f), RoomSlot(0.4600f, 0.5400f, 0.1000f),
        // Reading table — coffee mug, magnifying glass, hourglass, journal.
        RoomSlot(0.7121f, 0.4363f, 0.1152f), RoomSlot(0.9436f, 0.5752f, 0.0960f),
        RoomSlot(0.7033f, 0.5962f, 0.1000f), RoomSlot(0.8694f, 0.4488f, 0.0973f),
        // Windowsill.
        RoomSlot(0.8053f, 0.2905f, 0.1200f),
        // Floor — armchair, telescope.
        RoomSlot(0.2000f, 0.8800f, 0.1800f), RoomSlot(0.6000f, 0.8600f, 0.1300f),
        // Wall — map.
        RoomSlot(0.6040f, 0.1560f, 0.1200f),
        // Reading desk — lamp.
        RoomSlot(0.9700f, 0.3431f, 0.0960f),
    ),
    backdrop = { modifier -> LibraryBackdrop(modifier) },
)

@Composable
private fun LibraryBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.Black, alpha = 0.04f, count = 40, seed = 401)

        drawRect(brush = Brush.verticalGradient(listOf(floor, floorShadow)), topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        woodGrainLines(Offset(0f, wallHeight), Size(w, h - wallHeight), color = Color.Black, alpha = 0.07f, lineCount = 6)
        drawLine(Color.Black.copy(alpha = 0.24f), Offset(0f, wallHeight), Offset(w, wallHeight), strokeWidth = 3f)

        // Floor rug beneath the armchair.
        val rugCenter = Offset(w * 0.22f, h * 0.87f)
        val rugSize = Size(w * 0.32f, h * 0.14f)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(rugColor.copy(alpha = 0.55f), rugColor.copy(alpha = 0.3f)), center = rugCenter, radius = rugSize.width * 0.6f),
            topLeft = rugCenter - Offset(rugSize.width / 2f, rugSize.height / 2f), size = rugSize, cornerRadius = CornerRadius(rugSize.height * 0.4f, rugSize.height * 0.4f),
        )

        // Built-in bookshelf spanning most of the back wall — outer frame + 3 shelf boards + sides.
        val shL = w * SHELF_LEFT
        val shR = w * SHELF_RIGHT
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(shelfWood, shelfWoodDark)),
            topLeft = Offset(shL - 10f, h * 0.06f), size = Size(shR - shL + 20f, wallHeight - h * 0.06f + 4f), cornerRadius = CornerRadius(4f, 4f),
        )
        listOf(SHELF_ROW_1, SHELF_ROW_2, SHELF_ROW_3, 0.66f).forEach { rowFraction ->
            val y = h * rowFraction
            drawRoundRect(Color(0xFF3F2E1E), topLeft = Offset(shL - 6f, y), size = Size(shR - shL + 12f, 8f), cornerRadius = CornerRadius(2f, 2f))
            drawOval(Color.Black.copy(alpha = 0.16f), topLeft = Offset(shL - 6f, y + 8f), size = Size(shR - shL + 12f, 6f))
        }
        // A faint scatter of unplaceable spine-colored bands behind every shelf row — reads as
        // "a full library," not just the movable objects that land on top.
        listOf(SHELF_ROW_1, SHELF_ROW_2, SHELF_ROW_3).forEach { rowFraction ->
            val y = h * rowFraction
            val spineColors = listOf(Color(0xFF8B3A3A), Color(0xFF4E6647), Color(0xFFC79A46), Color(0xFF3F5A8A))
            var x = shL + 6f
            var i = 0
            while (x < shR - 14f) {
                val spineW = 8f + (i % 3) * 4f
                drawRect(spineColors[i % spineColors.size].copy(alpha = 0.5f), topLeft = Offset(x, y - h * 0.1f), size = Size(spineW, h * 0.1f))
                x += spineW + 2f
                i++
            }
        }

        // Reading table, right side.
        val tblL = w * TABLE_LEFT
        val tblR = w * TABLE_RIGHT
        val tblT = h * TABLE_TOP
        drawOval(Color.Black.copy(alpha = 0.18f), topLeft = Offset(tblL - 4f, wallHeight - 6f), size = Size(tblR - tblL + 8f, 14f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(shelfWood, shelfWoodDark)),
            topLeft = Offset(tblL, tblT), size = Size(tblR - tblL, wallHeight - tblT + 4f), cornerRadius = CornerRadius(4f, 4f),
        )
        woodGrainLines(Offset(tblL, tblT), Size(tblR - tblL, wallHeight - tblT), color = Color.Black, alpha = 0.08f, lineCount = 2)

        // Window above the table.
        val winL = w * WINDOW_LEFT
        val winR = w * WINDOW_RIGHT
        val winT = h * WINDOW_TOP
        val winB = h * WINDOW_BOTTOM
        drawRoundRect(shelfWoodDark, topLeft = Offset(winL - 6f, winT - 6f), size = Size(winR - winL + 12f, winB - winT + 12f), cornerRadius = CornerRadius(6f, 6f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFF0C875).copy(alpha = 0.6f), Color(0xFFC96A38).copy(alpha = 0.4f))),
            topLeft = Offset(winL, winT), size = Size(winR - winL, winB - winT), cornerRadius = CornerRadius(4f, 4f),
        )
        drawLine(shelfWoodDark, Offset((winL + winR) / 2f, winT), Offset((winL + winR) / 2f, winB), strokeWidth = 3f)

        // Armchair silhouette, foreground left, on the rug.
        val chairW = w * 0.2f
        val chairL = w * 0.12f
        val chairTop = h * 0.62f
        drawRoundRect(brush = Brush.verticalGradient(listOf(chairColor, Color(0xFF3F2312))), topLeft = Offset(chairL, chairTop), size = Size(chairW, wallHeight - chairTop + 4f), cornerRadius = CornerRadius(chairW * 0.2f, chairW * 0.2f))
        drawRoundRect(chairColor, topLeft = Offset(chairL - chairW * 0.16f, chairTop + chairW * 0.3f), size = Size(chairW * 0.2f, chairW * 0.4f), cornerRadius = CornerRadius(6f, 6f))
        drawRoundRect(chairColor, topLeft = Offset(chairL + chairW * 0.96f, chairTop + chairW * 0.3f), size = Size(chairW * 0.2f, chairW * 0.4f), cornerRadius = CornerRadius(6f, 6f))

        // Warm vignette — the reading lamp's glow should feel like the dominant light source.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF0C875).copy(alpha = 0.08f), Color.Black.copy(alpha = 0.18f)),
                center = Offset(w * 0.85f, h * 0.5f),
                radius = w * 0.9f,
            ),
            size = Size(w, h),
        )
    }
}
