package com.suman.memoryarchitect.feature.modeselect

/** Just enough of the player's progress to know whether Daily/Weekly Challenge are still serving
 * their post-win lock — see [ModeSelectViewModel] for why this is never a Loading/Error sealed
 * state. */
data class ModeSelectProgressUiState(
    /** Null when not locked - see [com.suman.memoryarchitect.core.common.challengeLockDurationSeconds]. */
    val dailyChallengeUnlockAtEpochSecond: Long? = null,
    val weeklyChallengeUnlockAtEpochSecond: Long? = null,
)
