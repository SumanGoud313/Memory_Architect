package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.data.remote.ProgressionApi
import com.suman.memoryarchitect.data.remote.dto.ClaimDailyRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.data.remote.dto.ScoreSubmissionRequestDto
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.ScoreResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps `mock-backend/`'s REST API exactly as [ProgressionRepositoryImpl] talked to it before
 * [ProgressionRemoteSource] existed - same requests, same response mapping, byte-for-byte the
 * original behavior. See [ProgressionRemoteSource]'s doc for why this is dev-only: this backend
 * has no concept of "which player" at all, one process-wide profile shared by every caller.
 */
@Singleton
class MockBackendProgressionRemoteSource @Inject constructor(
    private val api: ProgressionApi,
) : ProgressionRemoteSource {

    override suspend fun getProfile(): PlayerProfile = api.getProfile().toDomain()

    override suspend fun submitScore(
        mode: GameMode,
        score: ScoreResult,
        levelSeed: Long,
        playedOnEpochDay: Long,
        submissionNonce: String,
        newlyUnlockedAchievementCount: Int,
    ): PlayerProfile {
        // submissionNonce is intentionally unused here - this dev-only backend has no per-submission
        // replay protection (see ProgressionRemoteSource's doc), same as its single-shared-profile
        // design has no per-player identity at all.
        val request = ScoreSubmissionRequestDto(
            mode.name, levelSeed, score.finalScore, score.sceneAccuracy, score.comboCount, playedOnEpochDay, newlyUnlockedAchievementCount,
        )
        return api.submitScore(request).toDomain()
    }

    override suspend fun getDailyRewardStatus(todayEpochDay: Long): DailyRewardStatus {
        val dto = api.getDailyRewardStatus(todayEpochDay)
        return DailyRewardStatus(dto.cycleDay, dto.canClaimToday, dto.lastClaimedEpochDay)
    }

    override suspend fun claimDailyReward(claimedOnEpochDay: Long): DailyRewardClaimResult {
        val dto = api.claimDailyReward(ClaimDailyRewardRequestDto(claimedOnEpochDay))
        return DailyRewardClaimResult(dto.cycleDay, dto.coinsAwarded, dto.xpAwarded, dto.profile.toDomain(), dto.shieldAwarded)
    }

    private fun PlayerProfileDto.toDomain() = PlayerProfile(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        streakShields = streakShields,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        journeyPoints = journeyPoints,
    )
}
