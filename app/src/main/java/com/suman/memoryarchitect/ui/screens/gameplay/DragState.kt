package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.ui.geometry.Offset

/**
 * Tracks a tray object currently being dragged toward the scene panel. [positionInRoot] and
 * [trayOriginInRoot] are expressed relative to [GameplayScreen]'s outermost Box so both the
 * tray and the scene panel (which live in different subtrees) can convert them into their own
 * local coordinate space via `LayoutCoordinates.localPositionOf`. [trayOriginInRoot] is
 * captured once at drag start so a drop with no snap target can animate smoothly back to it
 * instead of just vanishing. [snappedSlotIndex] is the live magnetic-snap target (nearest empty
 * slot within range), recomputed on every drag update — never the object's actual correct slot.
 */
data class DragState(
    val objectId: String,
    val positionInRoot: Offset,
    val trayOriginInRoot: Offset,
    val snappedSlotIndex: Int? = null,
)
