package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult

interface ProgressionRepository {
    suspend fun getProfile(): Outcome<PlayerProfile>

    /** Local-only: statistics have no server endpoint, they're tracked purely on-device. */
    suspend fun getStatistics(): PlayerStatistics

    /** Local-only, same reasoning as [getStatistics]. */
    suspend fun getUnlockedAchievementIds(): Set<AchievementId>

    /** Local-only, same reasoning as [getStatistics]. */
    suspend fun getUnlockedRewardIds(): Set<RewardId>

    /** [timeTakenMs] is the wall-clock Reconstruct completion time (0 for a mode/round with no
     * meaningful timing, e.g. an untimed Practice round) - purely additive to [PlayerStatistics]'
     * average/fastest-completion tracking, never used for scoring itself (that's already folded
     * into [score] before this is called).
     *
     * [submissionNonce] identifies this specific finished round - minted once by the caller and
     * reused across any retry of *that same round*, never regenerated per network attempt (a fresh
     * nonce per attempt would defeat its own purpose: it exists so a retried/duplicated network
     * call can never grant XP/coins twice for one round). A resubmission with an already-seen
     * nonce fails with a [com.suman.memoryarchitect.domain.model.AppError.Server] (code 409),
     * exactly like [claimDailyReward]'s existing double-claim rejection. */
    suspend fun submitScore(
        mode: GameMode,
        levelSeed: Long,
        score: ScoreResult,
        playedOnEpochDay: Long,
        timeTakenMs: Long = 0L,
        submissionNonce: String,
    ): Outcome<ScoreSubmissionResult>

    /** Server-authoritative, same reasoning as [getProfile] — needs a source of truth to prevent
     * double-claiming across devices/reinstalls, so unlike campaign progress this doesn't fall
     * back to a purely local, no-server-counterpart model. */
    suspend fun getDailyRewardStatus(todayEpochDay: Long): Outcome<DailyRewardStatus>

    suspend fun claimDailyReward(todayEpochDay: Long): Outcome<DailyRewardClaimResult>

    /** Local-only (see [getStatistics]) - breaks [PlayerStatistics.currentWinStreak] back to 0.
     * Called whenever a scored round is explicitly *failed* (not merely never attempted), since
     * every [submitScore] call now only ever fires on a pass and has no other opportunity to see a
     * failure happen. A no-op if the streak is already 0. */
    suspend fun resetWinStreak()

    /** Called opportunistically whenever the Leaderboard screen successfully fetches the
     * player's own Daily/Weekly rank - caches whichever is better than what's already recorded
     * (see [PlayerStatistics.dailyBestRank]/`weeklyBestRank`) and evaluates the two rank-gated
     * achievements ([AchievementId.TOP_100_DAILY]/[AchievementId.TOP_10_WEEKLY]) against it.
     * Local-only, same reasoning as [getStatistics] - a no-op (returns an empty list) if neither
     * rank improves on what was already cached. */
    suspend fun recordLeaderboardRank(dailyRank: Int?, weeklyRank: Int?, todayEpochDay: Long): List<AchievementId>
}
