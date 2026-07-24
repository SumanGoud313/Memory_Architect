package com.suman.memoryarchitect.domain.model

data class PlayerProfile(
    val xp: Long,
    val coins: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastPlayedEpochDay: Long?,
    /** Epoch-second timestamp of the player's most recent Daily/Weekly Challenge win, or null if
     * never won (or never won since the last local DB wipe) - see
     * [com.suman.memoryarchitect.core.common.challengeLockDurationSeconds] for the resulting
     * lock window this feeds on Mode Select. */
    val dailyChallengeWonAtEpochSecond: Long? = null,
    val weeklyChallengeWonAtEpochSecond: Long? = null,
) {
    companion object {
        val EMPTY = PlayerProfile(xp = 0L, coins = 0L, currentStreak = 0, longestStreak = 0, lastPlayedEpochDay = null)
    }
}
