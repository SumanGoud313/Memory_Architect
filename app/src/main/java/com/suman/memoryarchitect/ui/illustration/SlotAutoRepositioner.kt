package com.suman.memoryarchitect.ui.illustration

import kotlin.math.hypot

/**
 * Generic, reusable fix for [findOverlaps] violations: nudges overlapping slot pairs apart along
 * the vector between them, then - only if a pair still can't fit within its drift budget - shrinks
 * the offending slots' [RoomSlot.footprintFraction] slightly. Used once, offline, to correct all 8
 * rooms' hand-authored slot lists (see `RoomLayoutOverlapTest` for the permanent regression guard
 * this feeds into); not called at runtime.
 *
 * Two constraints keep a room's art direction intact rather than just scattering slots until
 * nothing touches: [maxDriftFraction] caps how far any slot may move from its *originally
 * authored* position (a shelf trinket can't drift onto the bed), and positions are clamped to
 * [canvasMargin]..[1-canvasMargin] so nothing drifts off the room canvas. If a pair still overlaps
 * after both position and size adjustments are exhausted, it's left as-is - callers should re-run
 * [findOverlaps] on the result and treat any survivors as a structurally-overcrowded cluster that
 * needs a human decision (fewer slots, or a wider piece of furniture in the art), not a silently
 * forced fix.
 */
fun autoResolveOverlaps(
    slots: List<RoomSlot>,
    scaleFactor: Float = MAX_OBJECT_SIZE_SCALE,
    safetyMarginFraction: Float = DEFAULT_SAFETY_MARGIN,
    maxDriftFraction: Float = 0.05f,
    minFootprintFraction: Float = 0.07f,
    canvasMargin: Float = 0.03f,
    maxIterations: Int = 400,
): List<RoomSlot> {
    if (slots.isEmpty()) return slots
    val originalX = slots.map { it.xFraction }
    val originalY = slots.map { it.yFraction }
    val x = slots.map { it.xFraction }.toFloatArray()
    val y = slots.map { it.yFraction }.toFloatArray()
    val footprint = slots.map { it.footprintFraction }.toFloatArray()

    fun current() = List(slots.size) { i -> RoomSlot(x[i], y[i], footprint[i]) }

    // Pass 1: reposition within each slot's drift budget.
    repeat(maxIterations) {
        val overlaps = current().findOverlaps(scaleFactor, safetyMarginFraction)
        if (overlaps.isEmpty()) return current()
        for (overlap in overlaps) {
            val i = overlap.indexA
            val j = overlap.indexB
            val dx = x[j] - x[i]
            val dy = y[j] - y[i]
            val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            // Coincident slots (distance 0) get an arbitrary push axis so they don't get stuck.
            val (ux, uy) = if (distance > 1e-6f) dx / distance to dy / distance else 1f to 0f
            val push = overlap.penetration / 2f + 0.001f

            val driftBudgetI = maxDriftFraction - hypot((x[i] - originalX[i]).toDouble(), (y[i] - originalY[i]).toDouble()).toFloat()
            val driftBudgetJ = maxDriftFraction - hypot((x[j] - originalX[j]).toDouble(), (y[j] - originalY[j]).toDouble()).toFloat()
            val moveI = push.coerceAtMost(driftBudgetI.coerceAtLeast(0f))
            val moveJ = push.coerceAtMost(driftBudgetJ.coerceAtLeast(0f))

            x[i] = (x[i] - ux * moveI).coerceIn(canvasMargin, 1f - canvasMargin)
            y[i] = (y[i] - uy * moveI).coerceIn(canvasMargin, 1f - canvasMargin)
            x[j] = (x[j] + ux * moveJ).coerceIn(canvasMargin, 1f - canvasMargin)
            y[j] = (y[j] + uy * moveJ).coerceIn(canvasMargin, 1f - canvasMargin)
        }
    }

    // Pass 2: any pair still overlapping has exhausted its drift budget - shrink footprints instead.
    repeat(maxIterations) {
        val overlaps = current().findOverlaps(scaleFactor, safetyMarginFraction)
        if (overlaps.isEmpty()) return current()
        for (overlap in overlaps) {
            val i = overlap.indexA
            val j = overlap.indexB
            // Shrink whichever of the pair has more headroom above the floor, split evenly when equal.
            val shrinkable = listOfNotNull(
                i.takeIf { footprint[i] > minFootprintFraction },
                j.takeIf { footprint[j] > minFootprintFraction },
            )
            if (shrinkable.isEmpty()) continue // both already at floor - leave as a reported residual
            for (idx in shrinkable) {
                footprint[idx] = (footprint[idx] * 0.96f).coerceAtLeast(minFootprintFraction)
            }
        }
    }

    return current()
}
