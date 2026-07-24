package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class SubmitScoreUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(
        mode: GameMode,
        levelSeed: Long,
        score: ScoreResult,
        playedOnEpochDay: Long,
        timeTakenMs: Long = 0L,
        submissionNonce: String,
    ): Outcome<ScoreSubmissionResult> = repository.submitScore(mode, levelSeed, score, playedOnEpochDay, timeTakenMs, submissionNonce)
}
