package com.suman.memoryarchitect.ui.illustration

import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.R
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomArtTest {

    private val art = RoomArt(
        id = "test_room",
        labelRes = R.string.scene_unknown,
        palette = RoomPalette(wall = Color.Black, wallShadow = Color.Black, floor = Color.Black, accent = Color.Black),
        slots = listOf(
            RoomSlot(xFraction = 0.2f, yFraction = 0.3f, footprintFraction = 0.15f),
            RoomSlot(xFraction = 0.8f, yFraction = 0.7f, footprintFraction = 0.1f),
            RoomSlot(xFraction = 0.5f, yFraction = 0.5f, footprintFraction = 0.12f),
        ),
        backdrop = { },
    )

    @Test
    fun `mirrored false returns the same slot geometry`() {
        val result = art.mirrored(false)

        assertEquals(art.slots, result.slots)
    }

    @Test
    fun `mirrored true flips xFraction only, leaving yFraction and footprint untouched`() {
        val result = art.mirrored(true)

        art.slots.zip(result.slots).forEach { (original, mirroredSlot) ->
            assertEquals(1f - original.xFraction, mirroredSlot.xFraction, 0.0001f)
            assertEquals(original.yFraction, mirroredSlot.yFraction, 0.0001f)
            assertEquals(original.footprintFraction, mirroredSlot.footprintFraction, 0.0001f)
        }
    }

    @Test
    fun `mirroring twice returns to the original x positions`() {
        val roundTripped = art.mirrored(true).mirrored(true)

        art.slots.zip(roundTripped.slots).forEach { (original, twice) ->
            assertEquals(original.xFraction, twice.xFraction, 0.0001f)
        }
    }
}
