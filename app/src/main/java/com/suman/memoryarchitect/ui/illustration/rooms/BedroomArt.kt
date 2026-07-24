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

private val wall = Color(0xFFDDE0EE)
private val wallShadow = Color(0xFFC3C8DE)
private val floor = Color(0xFFC9A97D)
private val floorShadow = Color(0xFFB08F63)
private val bedFrame = Color(0xFF7A5230)
private val bedLinen = Color(0xFFF3ECE0)
private val pillowColor = Color(0xFFFFFFFF)
private val windowSky = Color(0xFFAED4E8)
private val windowFrame = Color(0xFF8B6A4A)
private val curtainColor = Color(0xFFC98B7A)
private val wardrobeColor = Color(0xFF6B4A32)
private val wardrobeDoor = Color(0xFF54381F)
private val nightstandColor = Color(0xFF8B5E3C)
private val shelfColor = Color(0xFF6B4A32)

// Layout constants (fractions of the square canvas) — furniture and slots are derived from the
// same numbers so the designed positions always line up with what's actually drawn.
private const val WALL_HEIGHT = 0.68f
private const val BED_LEFT = 0.03f
private const val BED_RIGHT = 0.42f
private const val BED_HEAD_TOP = 0.50f
private const val NIGHTSTAND_LEFT = 0.45f
private const val NIGHTSTAND_RIGHT = 0.58f
private const val NIGHTSTAND_TOP = 0.58f
private const val WINDOW_LEFT = 0.62f
private const val WINDOW_RIGHT = 0.85f
private const val WINDOW_TOP = 0.10f
private const val WINDOW_BOTTOM = 0.42f
private const val WARDROBE_LEFT = 0.87f
private const val WARDROBE_RIGHT = 0.99f
private const val WARDROBE_TOP = 0.12f
private const val SHELF_LEFT = 0.06f
private const val SHELF_RIGHT = 0.38f
private const val SHELF_1_Y = 0.16f
private const val SHELF_2_Y = 0.30f

val bedroomRoomArt = RoomArt(
    id = "bedroom",
    labelRes = R.string.scene_bedroom,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = Color(0xFFE0937F)),
    ambientMood = AmbientMood.WARM_GLOW,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Shelf boards (left wall, above the bed) — 6 small trinket spots across two boards.
        RoomSlot(0.0300f, 0.1980f, 0.1100f),
        RoomSlot(0.1920f, 0.1982f, 0.1100f),
        RoomSlot(0.3541f, 0.1979f, 0.1100f),
        RoomSlot(0.0300f, 0.3619f, 0.1100f),
        RoomSlot(0.1927f, 0.3624f, 0.1100f),
        RoomSlot(0.3553f, 0.3618f, 0.1100f),
        // Nightstand top — small items only.
        RoomSlot(0.4881f, 0.4943f, 0.1300f),
        RoomSlot(0.6472f, 0.5516f, 0.1000f),
        // Windowsill.
        RoomSlot(0.6409f, 0.3786f, 0.1300f),
        RoomSlot(0.8137f, 0.4593f, 0.1300f),
        // Wardrobe top.
        RoomSlot(0.8071f, 0.0700f, 0.1239f),
        RoomSlot(0.9700f, 0.0700f, 0.0973f),
        // Bed — corner and pillow area.
        RoomSlot(0.3025f, 0.5689f, 0.1500f),
        RoomSlot(0.1000f, 0.5300f, 0.1300f),
        // Floor rug spots.
        RoomSlot(0.4935f, 0.8370f, 0.1700f),
        RoomSlot(0.7051f, 0.9073f, 0.1500f),
        RoomSlot(0.2814f, 0.9056f, 0.1500f),
        // Wall hook beside the wardrobe (coat hanger).
        RoomSlot(0.8381f, 0.2729f, 0.1300f),
    ),
    backdrop = { modifier -> BedroomBackdrop(modifier) },
)

@Composable
private fun BedroomBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        // Wall with a soft directional-light gradient (brighter near the window) + floor.
        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(w * 0.75f, h * 0.2f),
                radius = w * 0.5f,
            ),
            size = Size(w, wallHeight),
        )
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.Black, alpha = 0.03f, count = 40, seed = 101)
        // Wall panel seam filling the otherwise-bare stretch between window and wardrobe.
        drawLine(Color.Black.copy(alpha = 0.08f), Offset(w * 0.58f, h * 0.05f), Offset(w * 0.58f, wallHeight - 4f), strokeWidth = 2f)

        drawRect(brush = Brush.verticalGradient(listOf(floor, floorShadow)), topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        woodGrainLines(Offset(0f, wallHeight), Size(w, h - wallHeight), color = Color.Black, alpha = 0.06f, lineCount = 6)

        // Skirting board — a proper baseboard strip instead of a flat seam line.
        val skirtH = h * 0.018f
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFF3ECE0), Color(0xFFC9BFAF))), topLeft = Offset(0f, wallHeight - skirtH), size = Size(w, skirtH))
        drawLine(Color.Black.copy(alpha = 0.22f), Offset(0f, wallHeight), Offset(w, wallHeight), strokeWidth = 3f)

        // Floor rug — soft-edged, purely decorative, anchored beneath the bed-side floor spots.
        val rugCenter = Offset(w * 0.52f, h * 0.87f)
        val rugSize = Size(w * 0.42f, h * 0.16f)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(Color(0xFFE0937F).copy(alpha = 0.5f), Color(0xFFB8623A).copy(alpha = 0.4f)), center = rugCenter, radius = rugSize.width * 0.6f),
            topLeft = rugCenter - Offset(rugSize.width / 2f, rugSize.height / 2f), size = rugSize, cornerRadius = CornerRadius(rugSize.height * 0.4f, rugSize.height * 0.4f),
        )
        drawRoundRect(
            color = Color(0xFFF3ECE0).copy(alpha = 0.3f),
            topLeft = rugCenter - Offset(rugSize.width * 0.42f, rugSize.height * 0.34f), size = Size(rugSize.width * 0.84f, rugSize.height * 0.68f),
            cornerRadius = CornerRadius(rugSize.height * 0.32f, rugSize.height * 0.32f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
        )

        // Soft vignette for depth — darkens the corners without touching the room's mid-tones.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.85f,
            ),
            size = Size(w, h),
        )

        // Window: frame, sky, sill, curtains.
        val winL = w * WINDOW_LEFT
        val winR = w * WINDOW_RIGHT
        val winT = h * WINDOW_TOP
        val winB = h * WINDOW_BOTTOM
        drawRoundRect(windowFrame, topLeft = Offset(winL - 8f, winT - 8f), size = Size(winR - winL + 16f, winB - winT + 16f), cornerRadius = CornerRadius(10f, 10f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(windowSky, Color(0xFF8FA3C7))),
            topLeft = Offset(winL, winT), size = Size(winR - winL, winB - winT), cornerRadius = CornerRadius(6f, 6f),
        )
        drawLine(windowFrame, Offset((winL + winR) / 2f, winT), Offset((winL + winR) / 2f, winB), strokeWidth = 5f)
        // Curtains draping either side.
        drawRoundRect(curtainColor.copy(alpha = 0.85f), topLeft = Offset(winL - 26f, winT - 14f), size = Size(24f, winB - winT + 30f), cornerRadius = CornerRadius(10f, 10f))
        drawRoundRect(curtainColor.copy(alpha = 0.85f), topLeft = Offset(winR + 2f, winT - 14f), size = Size(24f, winB - winT + 30f), cornerRadius = CornerRadius(10f, 10f))
        // Sill.
        drawRoundRect(windowFrame, topLeft = Offset(winL - 12f, winB), size = Size(winR - winL + 24f, 10f), cornerRadius = CornerRadius(4f, 4f))

        // Wardrobe — tall cabinet with two visible doors + a soft grounding shadow.
        val wardL = w * WARDROBE_LEFT
        val wardR = w * WARDROBE_RIGHT
        val wardT = h * WARDROBE_TOP
        drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(wardL - 6f, wallHeight - 10f), size = Size(wardR - wardL + 12f, 22f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(wardrobeColor, Color(0xFF3F2A16))),
            topLeft = Offset(wardL, wardT), size = Size(wardR - wardL, wallHeight - wardT + 4f), cornerRadius = CornerRadius(6f, 6f),
        )
        val doorMid = (wardL + wardR) / 2f
        drawLine(Color.Black.copy(alpha = 0.4f), Offset(doorMid, wardT + 6f), Offset(doorMid, wallHeight - 4f), strokeWidth = 3f)
        listOf(wardL + (wardR - wardL) * 0.25f, wardL + (wardR - wardL) * 0.75f).forEach { knobX ->
            drawCircle(wardrobeDoor, radius = 4f, center = Offset(knobX, (wardT + wallHeight) / 2f))
        }

        // Shelf — two wall-mounted boards on the left, above the bed.
        listOf(SHELF_1_Y, SHELF_2_Y).forEach { shelfYFraction ->
            val shelfY = h * shelfYFraction
            drawRoundRect(shelfColor, topLeft = Offset(w * SHELF_LEFT, shelfY), size = Size(w * (SHELF_RIGHT - SHELF_LEFT), 8f), cornerRadius = CornerRadius(3f, 3f))
            drawOval(Color.Black.copy(alpha = 0.12f), topLeft = Offset(w * SHELF_LEFT, shelfY + 8f), size = Size(w * (SHELF_RIGHT - SHELF_LEFT), 6f))
        }

        // Bed — frame, linen, headboard, pillow silhouette.
        val bedL = w * BED_LEFT
        val bedR = w * BED_RIGHT
        val bedHeadTop = h * BED_HEAD_TOP
        drawRoundRect(bedFrame, topLeft = Offset(bedL, h * 0.62f), size = Size(bedR - bedL, wallHeight - h * 0.62f + 6f), cornerRadius = CornerRadius(8f, 8f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(bedLinen, Color(0xFFD8CBB5))),
            topLeft = Offset(bedL + 4f, h * 0.58f), size = Size(bedR - bedL - 8f, h * 0.62f - h * 0.58f + 6f), cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(bedFrame, topLeft = Offset(bedL - 6f, bedHeadTop), size = Size(18f, wallHeight - bedHeadTop), cornerRadius = CornerRadius(6f, 6f))
        drawOval(
            brush = Brush.radialGradient(listOf(pillowColor, Color(0xFFEDE6DA))),
            topLeft = Offset(bedL + 10f, h * 0.58f - 14f), size = Size((bedR - bedL) * 0.4f, 26f),
        )

        // Nightstand beside the bed.
        val nsL = w * NIGHTSTAND_LEFT
        val nsR = w * NIGHTSTAND_RIGHT
        val nsTop = h * NIGHTSTAND_TOP
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(nightstandColor, Color(0xFF5C3A26))),
            topLeft = Offset(nsL, nsTop), size = Size(nsR - nsL, wallHeight - nsTop + 4f), cornerRadius = CornerRadius(4f, 4f),
        )
        drawOval(Color.Black.copy(alpha = 0.18f), topLeft = Offset(nsL - 4f, wallHeight - 6f), size = Size(nsR - nsL + 8f, 14f))
    }
}
