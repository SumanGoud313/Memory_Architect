package com.suman.memoryarchitect.ui.illustration

import kotlin.math.hypot

/**
 * Two slots whose rendered footprints would visually overlap at a given [RoomSlot.footprintFraction]
 * / scale combination - purely a geometry check, independent of which objects (if any) actually
 * occupy them. See [findOverlaps].
 */
data class SlotOverlap(
    val indexA: Int,
    val indexB: Int,
    /** How much closer than the minimum safe spacing these two slots are, as a canvas fraction -
     * always positive. Larger means a worse overlap. */
    val penetration: Float,
)

/**
 * Every room's designed slots must stay far enough apart that two simultaneously-occupied
 * neighbors never visually overlap, at the largest scale [GameplayScenePanel]'s `objectSizeScale`
 * ever applies (1.2x) plus a small safety margin for comfortable tap targets. This is a pure
 * geometry check - it knows nothing about which slots a given level actually fills, since two
 * slots that are *ever* both reachable must be safe together, not just the ones any one seed
 * happens to pick.
 *
 * [scaleFactor] should match (or exceed) the real render-time multiplier so this stays accurate
 * as that value changes; [safetyMarginFraction] is extra breathing room beyond exact edge-to-edge
 * contact, on top of the radii themselves.
 */
fun List<RoomSlot>.findOverlaps(
    scaleFactor: Float = MAX_OBJECT_SIZE_SCALE,
    safetyMarginFraction: Float = DEFAULT_SAFETY_MARGIN,
): List<SlotOverlap> {
    val overlaps = mutableListOf<SlotOverlap>()
    for (i in indices) {
        for (j in i + 1 until size) {
            val a = this[i]
            val b = this[j]
            val distance = hypot((a.xFraction - b.xFraction).toDouble(), (a.yFraction - b.yFraction).toDouble()).toFloat()
            val minSpacing = radiusOf(a, scaleFactor) + radiusOf(b, scaleFactor) + safetyMarginFraction
            if (distance < minSpacing) {
                overlaps += SlotOverlap(i, j, penetration = minSpacing - distance)
            }
        }
    }
    return overlaps
}

private fun radiusOf(slot: RoomSlot, scaleFactor: Float): Float = slot.footprintFraction * scaleFactor * 0.5f

/** The largest multiplier [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScenePanel]'s
 * `objectSizeScale(objectCount)` ever applies (at objectCount <= 6 - exactly where Daily/Weekly/
 * Practice live). Kept here, not imported from there, since that function is private to its file;
 * duplicated as a named, documented constant rather than a magic number. */
const val MAX_OBJECT_SIZE_SCALE = 1.2f

/** Extra breathing room beyond exact edge-to-edge contact, as a canvas-width fraction. */
const val DEFAULT_SAFETY_MARGIN = 0.015f
