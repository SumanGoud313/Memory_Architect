package com.suman.memoryarchitect.data.remote

import com.suman.memoryarchitect.data.remote.dto.ChallengeResponseDto
import com.suman.memoryarchitect.data.remote.dto.LevelSpecDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LevelApi {
    @GET("v1/levels/generate")
    suspend fun generateLevel(
        @Query("mode") mode: String,
        @Query("difficulty") difficulty: String,
    ): LevelSpecDto

    /** The same puzzle for every player today — a shared seed, not per-player generation. */
    @GET("v1/challenges/daily")
    suspend fun getDailyChallenge(): ChallengeResponseDto

    @GET("v1/challenges/weekly")
    suspend fun getWeeklyChallenge(): ChallengeResponseDto
}
