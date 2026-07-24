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

private val wall = Color(0xFF241D2E)
private val wallShadow = Color(0xFF17121E)
private val floor = Color(0xFF14101A)
private val floorShadow = Color(0xFF0A0810)
private val neonA = Color(0xFF60A5FA)
private val neonB = Color(0xFFE8674B)
private val deskColor = Color(0xFF2E2438)
private val deskTop = Color(0xFF3E3350)
private val shelfColor = Color(0xFF3E3350)

private const val WALL_HEIGHT = 0.68f
private const val DESK_LEFT = 0.05f
private const val DESK_RIGHT = 0.62f
private const val DESK_TOP_Y = 0.56f
private const val SHELF_Y = 0.18f

val gamingRoomRoomArt = RoomArt(
    id = "gaming_room",
    labelRes = R.string.scene_gaming_room,
    palette = RoomPalette(wall = wall, wallShadow = wallShadow, floor = floor, accent = neonA),
    ambientMood = AmbientMood.COOL_FOCUS,
    // Slot positions below are auto-validated to have zero overlapping pairs at the largest
    // objectSizeScale GameplayScenePanel ever applies (see SlotLayoutValidator/SlotAutoRepositioner)
    // - nudged from their originally-authored positions (kept close to the same furniture) rather
    // than expressed as offsets from the backdrop constants above, since that's what was validated.
    slots = listOf(
        // Desk surface — monitor, PC tower, keyboard, mouse, headset all live here.
        RoomSlot(0.0300f, 0.5025f, 0.1500f),
        RoomSlot(0.2361f, 0.4577f, 0.1500f),
        RoomSlot(0.4041f, 0.5738f, 0.1400f),
        RoomSlot(0.5115f, 0.4290f, 0.1104f),
        RoomSlot(0.6382f, 0.5252f, 0.1019f),
        // Wall shelf — collectibles, trophy, figurine.
        RoomSlot(0.6369f, 0.2023f, 0.1200f),
        RoomSlot(0.8103f, 0.1799f, 0.1200f),
        RoomSlot(0.9700f, 0.2332f, 0.1100f),
        // Wall / poster area.
        RoomSlot(0.7457f, 0.3718f, 0.1600f),
        RoomSlot(0.9487f, 0.4079f, 0.1300f),
        // Floor space — chair, backpack, energy drink, controller nearby.
        RoomSlot(0.1683f, 0.8528f, 0.1600f),
        RoomSlot(0.3682f, 0.8972f, 0.1300f),
        RoomSlot(0.6986f, 0.8507f, 0.1400f),
        RoomSlot(0.8834f, 0.9067f, 0.1300f),
        RoomSlot(0.5415f, 0.9526f, 0.1200f),
    ),
    backdrop = { modifier -> GamingRoomBackdrop(modifier) },
)

@Composable
private fun GamingRoomBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wallHeight = h * WALL_HEIGHT

        drawRect(brush = Brush.verticalGradient(listOf(wall, wallShadow), endY = wallHeight), size = Size(w, wallHeight))
        drawRect(brush = Brush.horizontalGradient(listOf(neonA.copy(alpha = 0.18f), Color.Transparent)), topLeft = Offset(0f, 0f), size = Size(w * 0.4f, wallHeight))
        drawRect(brush = Brush.horizontalGradient(listOf(Color.Transparent, neonB.copy(alpha = 0.16f))), topLeft = Offset(w * 0.6f, 0f), size = Size(w * 0.4f, wallHeight))
        matteNoise(Offset(0f, 0f), Size(w, wallHeight), color = Color.White, alpha = 0.025f, count = 36, seed = 104)
        drawRect(color = floorShadow, topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight))
        drawRect(
            brush = Brush.radialGradient(listOf(floor, floorShadow), center = Offset(w * 0.3f, h * 0.85f), radius = w * 0.5f),
            topLeft = Offset(0f, wallHeight), size = Size(w, h - wallHeight),
        )
        // Fine dark tile grid on the floor — cheap, low-draw-call texture.
        val tileStep = w * 0.09f
        var tx = tileStep
        while (tx < w) {
            drawLine(Color.White.copy(alpha = 0.025f), Offset(tx, wallHeight), Offset(tx, h), strokeWidth = 1.5f)
            tx += tileStep
        }
        var ty = wallHeight + tileStep
        while (ty < h) {
            drawLine(Color.White.copy(alpha = 0.025f), Offset(0f, ty), Offset(w, ty), strokeWidth = 1.5f)
            ty += tileStep
        }

        // Skirting board.
        val skirtH = h * 0.016f
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF322943), Color(0xFF17121E))), topLeft = Offset(0f, wallHeight - skirtH), size = Size(w, skirtH))
        drawRect(brush = Brush.horizontalGradient(listOf(neonA, neonB)), topLeft = Offset(0f, wallHeight - 4f), size = Size(w, 4f))

        // Floor rug beneath the gaming chair.
        val rugCenter = Offset(w * 0.24f, h * 0.9f)
        val rugSize = Size(w * 0.3f, h * 0.12f)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(neonA.copy(alpha = 0.22f), Color(0xFF17121E).copy(alpha = 0.3f)), center = rugCenter, radius = rugSize.width * 0.6f),
            topLeft = rugCenter - Offset(rugSize.width / 2f, rugSize.height / 2f), size = rugSize, cornerRadius = CornerRadius(rugSize.height * 0.4f, rugSize.height * 0.4f),
        )

        // Soft vignette for depth, layered on top of the neon wash.
        drawRect(
            brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)), center = Offset(w * 0.5f, h * 0.4f), radius = w * 0.9f),
            size = Size(w, h),
        )

        // Wall shelf for collectibles.
        val shelfY = h * SHELF_Y
        drawRoundRect(shelfColor, topLeft = Offset(w * 0.68f, shelfY), size = Size(w * 0.30f, 8f), cornerRadius = CornerRadius(3f, 3f))
        drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(w * 0.68f, shelfY + 8f), size = Size(w * 0.30f, 6f))

        // Poster frame on the back wall.
        drawRoundRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(w * 0.70f, h * 0.28f), size = Size(w * 0.24f, h * 0.20f), cornerRadius = CornerRadius(4f, 4f))
        drawRect(brush = Brush.linearGradient(listOf(neonA.copy(alpha = 0.5f), neonB.copy(alpha = 0.5f))), topLeft = Offset(w * 0.715f, h * 0.29f), size = Size(w * 0.21f, h * 0.18f))

        // Gaming desk — legs, a soft grounding shadow, and an RGB-lit front edge.
        val deskL = w * DESK_LEFT
        val deskR = w * DESK_RIGHT
        val deskTopY = h * DESK_TOP_Y
        val deskBaseY = wallHeight + (h - wallHeight) * 0.5f
        drawOval(Color.Black.copy(alpha = 0.3f), topLeft = Offset(deskL, deskBaseY), size = Size(deskR - deskL, 18f))
        listOf(deskL + 12f, deskR - 12f).forEach { legX ->
            drawLine(deskColor, Offset(legX, deskTopY + 10f), Offset(legX, deskBaseY), strokeWidth = 7f)
        }
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(deskTop, deskColor)),
            topLeft = Offset(deskL, deskTopY), size = Size(deskR - deskL, 14f), cornerRadius = CornerRadius(4f, 4f),
        )
        drawRect(brush = Brush.horizontalGradient(listOf(neonA, neonB)), topLeft = Offset(deskL, deskTopY + 12f), size = Size(deskR - deskL, 2.5f))
    }
}
