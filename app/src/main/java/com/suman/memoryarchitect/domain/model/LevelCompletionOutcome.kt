package com.suman.memoryarchitect.domain.model

data class LevelCompletionOutcome(
    val levelNumber: Int,
    val passed: Boolean,
    val timeTakenMs: Long,
    val bestTimeMs: Long?,
    val isNewBest: Boolean,
    val stars: Int,
    val bestStars: Int,
    val isNewBestStars: Boolean,
    val nextLevelUnlocked: Boolean,
    val isFinalLevel: Boolean,
)
