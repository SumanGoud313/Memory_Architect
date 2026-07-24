package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.Outcome

interface LevelRepository {
    suspend fun generateLevel(mode: GameMode, difficultyTier: DifficultyTier, streak: Int): Outcome<LevelSpec>
}