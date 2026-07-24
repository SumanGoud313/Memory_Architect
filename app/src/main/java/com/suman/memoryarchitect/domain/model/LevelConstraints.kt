package com.suman.memoryarchitect.domain.model

/** The difficulty-engine outputs that parameterize procedural level generation. */
data class LevelConstraints(
    val objectCount: Int,
    val distractorRatio: Float,
    val memorizeDurationMs: Long,
    val rotationEnabled: Boolean,
    val rotationStepDegrees: Int,
    val orderModeEnabled: Boolean,
    val themeComplexity: Int,
    val timeLimitMs: Long?,
)