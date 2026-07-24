package com.suman.memoryarchitect.ui.illustration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.rooms.bedroomRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.coffeeShopRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.gamingRoomRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.gardenRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.kitchenRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.libraryRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.officeRoomArt
import com.suman.memoryarchitect.ui.illustration.rooms.spaceStationRoomArt

/** A room's ambient "mood" — informs which particle-field tint plays over the scene, if any. */
enum class AmbientMood { NEUTRAL, WARM_GLOW, COOL_FOCUS, MORNING_LIGHT }

data class RoomPalette(
    val wall: Color,
    val wallShadow: Color,
    val floor: Color,
    val accent: Color,
)

/**
 * A single designed position a movable object can appear at — sitting on a real piece of
 * furniture (nightstand top, shelf board, windowsill, desk surface, floor rug spot) rather
 * than a floating grid cell. Fractional coordinates (0..1) across the room canvas;
 * [footprintFraction] is the object's rendered size relative to the room's width, so a
 * windowsill trinket can sit smaller than something on the floor.
 */
data class RoomSlot(
    val xFraction: Float,
    val yFraction: Float,
    val footprintFraction: Float = 0.16f,
)

/** A single room's illustration entry — mirrors [ObjectArt]'s shape. */
data class RoomArt(
    val id: String,
    val labelRes: Int,
    val palette: RoomPalette,
    val ambientMood: AmbientMood = AmbientMood.NEUTRAL,
    /** Order *is* the slot index — must have exactly [com.suman.memoryarchitect.domain.generation.GenerationRules.roomSlotCountByScene]'s
     * entry for this room's [id]. */
    val slots: List<RoomSlot>,
    val backdrop: @Composable (Modifier) -> Unit,
) {
    /** Safe against a count mismatch (wraps via modulo + logs) rather than crashing on-screen. */
    fun slotAt(index: Int): RoomSlot {
        if (slots.isEmpty()) return RoomSlot(0.5f, 0.5f)
        if (index !in slots.indices) {
            android.util.Log.w("RoomArt", "slotIndex=$index out of range for room=\"$id\" (${slots.size} slots) — wrapping")
        }
        return slots[index.mod(slots.size)]
    }

    /**
     * A left-right mirrored view of this room's *slot positions* only — every caller of
     * [slotAt]/[slots] (object placement, drag hit-testing, empty-slot hints) sees the flipped
     * geometry for free once they read through this instance instead of the original. The
     * backdrop's actual pixels are mirrored separately, by the caller applying a horizontal
     * `graphicsLayer` flip to the [backdrop] composable itself (see [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScenePanel]) —
     * kept as two halves of the same effect rather than baked in here, since [backdrop] is a
     * plain non-interactive Canvas draw and flipping it via a rendering transform carries none of
     * the touch-coordinate risk that flipping interactive object placement would.
     */
    fun mirrored(enabled: Boolean): RoomArt = if (!enabled) this else copy(slots = slots.map { it.copy(xFraction = 1f - it.xFraction) })
}

/**
 * Data-driven sceneType -> [RoomArt] lookup, replacing `ObjectArtCatalog.toSceneBackdropRes`.
 * Same registration pattern as [ObjectArtRegistry] — a future room adds one `rooms/<Room>Art.kt`
 * file and one entry below, nothing else changes.
 */
object RoomArtRegistry {
    private val byId: Map<String, RoomArt> by lazy {
        buildMap {
            register(bedroomRoomArt)
            register(kitchenRoomArt)
            register(coffeeShopRoomArt)
            register(gamingRoomRoomArt)
            register(officeRoomArt)
            register(libraryRoomArt)
            register(gardenRoomArt)
            register(spaceStationRoomArt)
        }
    }

    private val fallback = RoomArt(
        id = "unknown",
        labelRes = R.string.scene_unknown,
        palette = RoomPalette(Color(0xFF3A322A), Color(0xFF2E2721), Color(0xFF524537), Color(0xFF8FA383)),
        slots = emptyList(),
        backdrop = { modifier -> UnknownRoomBackdrop(modifier) },
    )

    fun get(sceneType: String): RoomArt = byId[sceneType] ?: fallback

    private fun MutableMap<String, RoomArt>.register(art: RoomArt) {
        this[art.id] = art
    }
}

@Composable
private fun UnknownRoomBackdrop(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawRect(color = Color(0xFF2E2721))
    }
}
