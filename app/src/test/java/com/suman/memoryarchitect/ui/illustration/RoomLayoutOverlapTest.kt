package com.suman.memoryarchitect.ui.illustration

import com.suman.memoryarchitect.ui.illustration.rooms.bedroomRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.coffeeShopRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.gamingRoomRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.gardenRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.kitchenRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.libraryRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.officeRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.spaceStationRoomArt
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every level in every mode (Classic's 100 levels, Practice, Daily, Weekly) only ever places
 * objects into one of these 8 rooms' designed slots - [LevelGenerator][com.suman.memoryarchitect.domain.generation.LevelGenerator]
 * only guarantees a unique slot *index* per object, never geometric spacing (see
 * [SlotLayoutValidator]/[SlotAutoRepositioner]'s docs for why). So the only way any level can ever
 * render two visually overlapping objects is if two of a room's own slots are too close together
 * in the first place - this test is the permanent guard against that, covering every mode at once
 * without needing per-mode or per-level checks. If this ever fails, a room's slots (or its
 * `RoomSlot.footprintFraction`s, or `objectSizeScale`'s max multiplier) were changed without
 * re-validating - fix with [autoResolveOverlaps], don't just relax the assertion.
 */
class RoomLayoutOverlapTest {

    private val rooms = listOf(
        bedroomRoomArt, kitchenRoomArt, coffeeShopRoomArt, gamingRoomRoomArt,
        officeRoomArt, libraryRoomArt, gardenRoomArt, spaceStationRoomArt,
    )

    @Test
    fun `no room has overlapping slots at the maximum objectSizeScale`() {
        for (room in rooms) {
            val overlaps = room.slots.findOverlaps()
            assertTrue(
                "Room \"${room.id}\" has overlapping slot pairs: $overlaps",
                overlaps.isEmpty(),
            )
        }
    }

    @Test
    fun `mirrored rooms are also overlap-free (mirroring only flips xFraction)`() {
        for (room in rooms) {
            val overlaps = room.mirrored(true).slots.findOverlaps()
            assertTrue(
                "Mirrored room \"${room.id}\" has overlapping slot pairs: $overlaps",
                overlaps.isEmpty(),
            )
        }
    }
}
