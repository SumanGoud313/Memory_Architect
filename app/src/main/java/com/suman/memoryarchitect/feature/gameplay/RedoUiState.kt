package com.suman.memoryarchitect.feature.gameplay

/**
 * Redo budget for the current level attempt. [redosUsed] is loaded from persisted per-level
 * storage when the level loads and only ever increases for that level number - retrying the
 * same level does not refresh the budget, mirroring [HintUiState].
 */
data class RedoUiState(
    val maxRedos: Int,
    val redosUsed: Int,
) {
    val remaining: Int get() = (maxRedos - redosUsed).coerceAtLeast(0)
    val canRedo: Boolean get() = remaining > 0

    companion object {
        val Initial = RedoUiState(maxRedos = 0, redosUsed = 0)
    }
}
