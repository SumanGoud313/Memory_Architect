package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.ReturningPlayerGiftClaimResult
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
    /** [awardXp] is `false` only for a repeat clear of an already-completed Classic level (see
     * [com.suman.memoryarchitect.domain.model.LevelCompletionOutcome.isFirstCompletion]) - XP
     * rewards campaign progress, not farming the same level over and over, so a repeat clear still
     * submits normally (coins, stars, leaderboard rank, streak, achievements all unaffected) but
     * contributes zero XP. Always `true` for Daily/Weekly Challenge (already naturally rate-limited
     * to once per day/week) and for a level's genuine first clear. */
    suspend fun submitScore(
        mode: GameMode,
        levelSeed: Long,
        score: ScoreResult,
        playedOnEpochDay: Long,
        timeTakenMs: Long = 0L,
        submissionNonce: String,
        awardXp: Boolean = true,
    ): Outcome<ScoreSubmissionResult>

    /** Server-authoritative, same reasoning as [getProfile] — needs a source of truth to prevent
     * double-claiming across devices/reinstalls, so unlike campaign progress this doesn't fall
     * back to a purely local, no-server-counterpart model. */
    suspend fun getDailyRewardStatus(todayEpochDay: Long): Outcome<DailyRewardStatus>

    suspend fun claimDailyReward(todayEpochDay: Long): Outcome<DailyRewardClaimResult>

    /** Flushes every queued [com.suman.memoryarchitect.core.database.PendingScoreSubmissionEntity]
     * (oldest first) by resending each through [submitScore]'s same server-authoritative call,
     * reconciling the local cache with whatever the server returns and removing the entry on
     * success. A submission the server already has (its original response just never reached the
     * client) is treated as a success too, not a failure - the cache is reconciled against a fresh
     * [getProfile] instead. Throws (rather than skipping ahead) on the first entry that still can't
     * reach the server, so a background retry scheduler can back off and try the whole queue again
     * later instead of silently reordering it. Called by
     * [com.suman.memoryarchitect.core.sync.PendingScoreSyncWorker], never directly by UI code. */
    suspend fun retryPendingSubmissions()

    /** Server-authoritative for the same double-claim reason [claimDailyReward] is - see
     * [com.suman.memoryarchitect.domain.usecase.GetReturningPlayerWelcomeUseCase]'s doc for the
     * (purely local) eligibility check callers should already have run before ever calling this;
     * this call still independently re-derives eligibility server-side rather than trusting that
     * check, the same "recognize, don't just trust" posture [claimDailyReward] already has. */
    suspend fun claimReturningPlayerGift(todayEpochDay: Long): Outcome<ReturningPlayerGiftClaimResult>

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
