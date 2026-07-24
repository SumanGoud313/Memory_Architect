package com.suman.memoryarchitect.feature.gameplay

/**
 * Free-tier Rewatch budget for the current level attempt. [rewatchesUsed] is loaded from the same
 * persisted per-level storage the always-on ad flow already tracked (see [RewatchRepository]) -
 * retrying the same level does not refresh the budget, mirroring [HintUiState]/[RedoUiState].
 */
data class RewatchUiState(
    val maxRewatches: Int,
    val rewatchesUsed: Int,
) {
    val remaining: Int get() = (maxRewatches - rewatchesUsed).coerceAtLeast(0)
    val canRewatchFree: Boolean get() = remaining > 0

    companion object {
        val Initial = RewatchUiState(maxRewatches = 0, rewatchesUsed = 0)
    }
}
