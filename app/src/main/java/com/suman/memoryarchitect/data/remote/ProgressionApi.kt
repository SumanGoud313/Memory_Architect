package com.suman.memoryarchitect.data.remote

import com.suman.memoryarchitect.data.remote.dto.ClaimDailyRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimDailyRewardResponseDto
import com.suman.memoryarchitect.data.remote.dto.ClaimReturningPlayerGiftRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimReturningPlayerGiftResponseDto
import com.suman.memoryarchitect.data.remote.dto.DailyRewardStatusDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.data.remote.dto.ScoreSubmissionRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ProgressionApi {
    @GET("v1/profile")
    suspend fun getProfile(): PlayerProfileDto

    @POST("v1/scores/submit")
    suspend fun submitScore(@Body body: ScoreSubmissionRequestDto): PlayerProfileDto

    @GET("v1/rewards/daily/status")
    suspend fun getDailyRewardStatus(@Query("todayEpochDay") todayEpochDay: Long): DailyRewardStatusDto

    @POST("v1/rewards/daily/claim")
    suspend fun claimDailyReward(@Body body: ClaimDailyRewardRequestDto): ClaimDailyRewardResponseDto

    @POST("v1/rewards/returning-player/claim")
    suspend fun claimReturningPlayerGift(@Body body: ClaimReturningPlayerGiftRequestDto): ClaimReturningPlayerGiftResponseDto

    /** Dev-only counterpart to [com.suman.memoryarchitect.data.repository.LocalProgressResetRepositoryImpl]'s local wipe - without this,
     * the next online-first [getProfile] call would silently re-fetch and restore the
     * pre-reset xp/coins/streak/challenge-lock state from the server. A real backend would
     * likely gate this behind auth rather than exposing it unconditionally, which is exactly
     * why the caller treats this as best-effort and never fails the reset if it 404s/errors. */
    @POST("v1/profile/reset")
    suspend fun resetProfile(): PlayerProfileDto
}
