package com.suman.memoryarchitect.domain.model

/**
 * A fully resolved, ready-to-render puzzle. For scored modes this is exactly what the
 * server returned (the [seed] lets the server re-derive and validate the same layout from
 * a score submission); for Practice mode it comes from the on-device [seed]ed generator.
 */
data class LevelSpec(
    val seed: Long,
    val mode: GameMode,
    val difficultyTier: DifficultyTier,
    val sceneType: String,
    val objects: List<SceneObjectSpec>,
    val timeLimitMs: Long?,
    val memorizeDurationMs: Long,
    val orderModeEnabled: Boolean,
)