package com.suman.memoryarchitect.feature.levelselect

sealed interface LevelSelectUiState {
    data object Loading : LevelSelectUiState
    data class Loaded(val maxUnlockedLevel: Int, val bestTimes: Map<Int, Long>, val bestStars: Map<Int, Int>) : LevelSelectUiState
}
