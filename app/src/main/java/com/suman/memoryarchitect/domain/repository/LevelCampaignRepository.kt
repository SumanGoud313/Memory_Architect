package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome

interface LevelCampaignRepository {
    suspend fun getMaxUnlockedLevel(): Int
    suspend fun getAllBestTimes(): Map<Int, Long>
    suspend fun getAllBestStars(): Map<Int, Int>
    suspend fun recordCompletion(levelNumber: Int, timeTakenMs: Long, passed: Boolean, stars: Int): LevelCompletionOutcome
}
