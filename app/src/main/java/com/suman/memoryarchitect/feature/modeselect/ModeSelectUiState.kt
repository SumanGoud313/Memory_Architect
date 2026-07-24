package com.suman.memoryarchitect.feature.modeselect

/** Just enough of the player's progress to keep Mode Select's footer from ever going empty —
 * see [ModeSelectViewModel] for why this is never a Loading/Error sealed state. */
data class ModeSelectProgressUiState(
    val level: Int,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long,
    val coins: Long,
    val currentStreak: Int,
    /** Null when not locked - see [com.suman.memoryarchitect.core.common.challengeLockDurationSeconds]. */
    val dailyChallengeUnlockAtEpochSecond: Long? = null,
    val weeklyChallengeUnlockAtEpochSecond: Long? = null,
)
