package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.ChallengePreview
import com.suman.memoryarchitect.domain.model.Outcome

interface ChallengeRepository {
    suspend fun getDailyChallenge(): Outcome<ChallengePreview>
    suspend fun getWeeklyChallenge(): Outcome<ChallengePreview>
}
