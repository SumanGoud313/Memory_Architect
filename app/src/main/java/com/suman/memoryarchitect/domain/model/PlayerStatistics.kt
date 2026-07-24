package com.suman.memoryarchitect.domain.model

/**
 * Local-only, single-row cumulative lifetime stats (see `StatisticsCacheEntity` /
 * `ProgressionRepositoryImpl.updateStatistics`) — evaluated/updated on-device on every score
 * submission, never server-verified (they're recognition/dashboard numbers, not the leaderboard's
 * source of truth, which is what `LeaderboardRepository` writes to Firestore instead).
 *
 * [averageAccuracy]/[averageCompletionTimeMs] are derived from running sums ([totalAccuracySum]/
 * [totalTimeTakenMs]) rather than stored as a naively-updated running average, so they can never
 * drift from re-deriving the true mean regardless of how many times they're recomputed.
 */
data class PlayerStatistics(
    val gamesPlayed: Int,
    val totalScore: Long,
    val bestAccuracy: Float,
    val bestScore: Int,
    /** Sum of every round's [ScoreResult.sceneAccuracy] ever submitted — divide by [gamesPlayed]
     * for [averageAccuracy]. `Double` to avoid `Float` precision loss accumulating over hundreds
     * of additions. */
    val totalAccuracySum: Double = 0.0,
    /** Sum of every scored round's completion time — divide by [gamesPlayed] for
     * [averageCompletionTimeMs]. Practice/untimed rounds contribute 0, same as [gamesPlayed]
     * counting them but [fastestCompletionMs] never being set by one. */
    val totalTimeTakenMs: Long = 0L,
    /** Fastest Reconstruct completion time across every scored round ever — `null` until the
     * first round with a real, positive completion time is submitted. */
    val fastestCompletionMs: Long? = null,
    /** Rounds finished with 100% scene accuracy. */
    val perfectGames: Int = 0,
    /** Longest [ScoreResult.comboCount] ever achieved in a single round. */
    val highestCombo: Int = 0,
    /** Sum of [ScoreResult.objectScores] sizes across every round — total objects the player has
     * ever placed a memory judgement on. */
    val objectsMemorized: Int = 0,
    /** Count of distinct calendar days (by [lastStatsUpdateEpochDay]-style epoch day) a round has
     * ever been submitted on — lifetime, unlike [PlayerProfile.currentStreak]/`longestStreak`
     * which only track *consecutive* days. */
    val daysPlayed: Int = 0,
    /** The epoch day [daysPlayed] was last incremented for — internal bookkeeping only, not
     * itself a displayed stat. */
    val lastStatsUpdateEpochDay: Long? = null,
    /** Daily/Weekly Challenge wins ever (accuracy at/above the challenge-win threshold — same
     * condition [ProgressionRepositoryImpl.coinsAwardedFor] already uses to grant the win bonus). */
    val dailyChallengesWon: Int = 0,
    val weeklyChallengesWon: Int = 0,
    /** Best (lowest) Daily/Weekly leaderboard rank ever observed - `null` until the Leaderboard
     * screen has successfully fetched at least once (see [com.suman.memoryarchitect.domain.usecase.RecordLeaderboardRankUseCase]).
     * Updated opportunistically, never blocks/gates anything else - a player who never opens the
     * Leaderboard screen simply never has one, same "recognition, not a requirement" spirit as
     * every other stat here. */
    val dailyBestRank: Int? = null,
    val weeklyBestRank: Int? = null,
    /** Consecutive rounds *won* in a row (distinct from [PlayerProfile.currentStreak], which counts
     * consecutive *days played*, not consecutive passes). Every [ProgressionRepositoryImpl.submitScore]
     * call now only ever fires on a pass, so this increments unconditionally on every call and is
     * reset to 0 only by an explicit failed round (see [ProgressionRepository.resetWinStreak]). */
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
) {
    val averageAccuracy: Float get() = if (gamesPlayed == 0) 0f else (totalAccuracySum / gamesPlayed).toFloat()
    val averageCompletionTimeMs: Long get() = if (gamesPlayed == 0) 0L else totalTimeTakenMs / gamesPlayed

    companion object {
        val EMPTY = PlayerStatistics(gamesPlayed = 0, totalScore = 0L, bestAccuracy = 0f, bestScore = 0)
    }
}
