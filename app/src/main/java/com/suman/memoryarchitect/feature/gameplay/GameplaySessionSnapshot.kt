package com.suman.memoryarchitect.feature.gameplay

import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.PlacedObject

/**
 * Everything needed to resume an in-progress Memorize/Hidden/Reconstruct round exactly where it
 * left off after process death — the one persistence gap standard ViewModel/SavedStateHandle
 * scoping doesn't already close for free (hints/redos survive via Room re-reads in
 * [GameplayViewModel.restoreFromSnapshot]/[GameplayViewModel.loadLevel], current level/mode/
 * difficulty survive via Navigation-Compose's own SavedStateHandle route args). Serialized to a
 * single JSON string under one SavedStateHandle key rather than one key per field, so every piece
 * of the round is read and written atomically together.
 *
 * [phaseDeadlineEpochMs] is a wall-clock deadline (via the injected [java.time.Clock], not a raw
 * remaining-ms count) so the actual remaining time can be recomputed from real elapsed time on
 * restore, rather than resuming with however much was left when the process happened to die.
 *
 * The `paused*` fields mirror [GameplayViewModel.PausedReconstruct] and are only non-null while a
 * Rewatch replay is in flight (mid-Memorize-replay or the brief Hidden gap that follows it) — the
 * one scenario where the visible [phase]/[placements]/[trayObjectIds] alone aren't enough to
 * resume correctly, since the interrupted Reconstruct round they'll return to lives separately.
 */
data class GameplaySessionSnapshot(
    val phase: GamePhase,
    val level: LevelSpec,
    val placements: Map<String, PlacedObject>,
    val trayObjectIds: List<String>,
    val placementOrder: List<String>,
    val phaseDeadlineEpochMs: Long?,
    val reconstructStartedAtEpochMs: Long?,
    val pausedPlacements: Map<String, PlacedObject>? = null,
    val pausedTrayObjectIds: List<String>? = null,
    val pausedPlacementOrder: List<String>? = null,
    val pausedRemainingMs: Long? = null,
)
