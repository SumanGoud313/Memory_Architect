package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.LevelRepository
import javax.inject.Inject

class GenerateLevelUseCase @Inject constructor(
    private val repository: LevelRepository,
) {
    suspend operator fun invoke(mode: GameMode, difficultyTier: DifficultyTier, streak: Int = 0): Outcome<LevelSpec> =
        repository.generateLevel(mode, difficultyTier, streak)
}