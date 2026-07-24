package com.suman.memoryarchitect.feature.gameplay

import com.suman.memoryarchitect.domain.model.HintReveal

/**
 * Hint budget + selection-mode state for the current level attempt. [hintsUsed] is loaded from
 * persisted per-level storage when the level loads and only ever increases for that level number
 * - retrying the same level does not refresh the budget, so hints stay a genuinely scarce
 * resource rather than something a player can reset by replaying.
 *
 * [isArmed] is "the player tapped Hint and is now expected to tap a tray object" - it is not the
 * hint itself. [activeReveal] is the live 2-second reveal driving the scene-panel glow/pulse/
 * arrow/sparkle highlight; it is cleared automatically by the ViewModel once that window elapses.
 */
data class HintUiState(
    val maxHints: Int,
    val hintsUsed: Int,
    val isArmed: Boolean = false,
    val activeReveal: HintReveal? = null,
) {
    val remaining: Int get() = (maxHints - hintsUsed).coerceAtLeast(0)
    val canUseHint: Boolean get() = remaining > 0

    companion object {
        val Initial = HintUiState(maxHints = 0, hintsUsed = 0)
    }
}
