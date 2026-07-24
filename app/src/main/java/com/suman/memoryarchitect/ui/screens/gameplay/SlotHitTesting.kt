package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.ui.geometry.Offset
import com.suman.memoryarchitect.ui.illustration.RoomSlot
import kotlin.math.hypot

/**
 * Pure magnetic-snap math — replaces the old grid cell hit-test. Only ever considers the
 * *nearest empty slot*, never the object's actual correct slot, so the drag assist is a
 * precision aid rather than a warmer/colder hint about where the object belongs.
 */
object SlotHitTesting {
    private const val DEFAULT_MAX_SNAP_DISTANCE_FRACTION = 0.13f

    fun nearestEmptySlotIndex(
        pointerFraction: Offset,
        slots: List<RoomSlot>,
        occupiedIndices: Set<Int>,
        maxSnapDistanceFraction: Float = DEFAULT_MAX_SNAP_DISTANCE_FRACTION,
    ): Int? {
        var bestIndex: Int? = null
        var bestDistance = Float.MAX_VALUE
        slots.forEachIndexed { index, slot ->
            if (index in occupiedIndices) return@forEachIndexed
            val distance = hypot(slot.xFraction - pointerFraction.x, slot.yFraction - pointerFraction.y)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex.takeIf { bestDistance <= maxSnapDistanceFraction }
    }
}
