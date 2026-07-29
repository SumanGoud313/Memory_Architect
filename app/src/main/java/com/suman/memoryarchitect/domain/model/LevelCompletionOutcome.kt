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
    /** `true` only the very first time this [levelNumber] is ever passed (no prior recorded best
     * stars) - `false` on every subsequent clear, no matter how much later or how much better the
     * result. Drives whether this round's completion awards XP at all (see
     * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.submitReconstruction]) - XP is
     * meant to reward campaign progress, not repeated farming of an already-cleared level. */
    val isFirstCompletion: Boolean,
)
