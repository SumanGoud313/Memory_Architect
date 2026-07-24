package com.suman.memoryarchitect.feature.leaderboard

import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.LeaderboardResult
import com.suman.memoryarchitect.domain.model.LeaderboardType

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Error(val selectedTab: LeaderboardType, val error: AppError) : LeaderboardUiState
    data class Content(val selectedTab: LeaderboardType, val result: LeaderboardResult) : LeaderboardUiState
}
