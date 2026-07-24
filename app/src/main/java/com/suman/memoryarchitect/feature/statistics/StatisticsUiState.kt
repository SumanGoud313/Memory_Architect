package com.suman.memoryarchitect.feature.statistics

import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.motivation.MotivationInsight
import com.suman.memoryarchitect.domain.progression.RankStanding

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState

    data class Content(
        // General
        val currentLevel: Int,
        val highestCampaignLevel: Int,
        val totalXp: Long,
        val totalScore: Long,
        val totalStars: Int,
        val globalRank: Int?,
        val rankStanding: RankStanding,
        // Performance / Activity / Challenges come straight off PlayerStatistics + these extras:
        val statistics: PlayerStatistics,
        val loginStreak: Int,
        val longestLoginStreak: Int,
        val totalSessions: Int,
        val totalPlayTimeMs: Long,
        // Collection
        val unlockedThemeCount: Int,
        val totalThemeCount: Int,
        val unlockedAchievementCount: Int,
        val totalAchievementCount: Int,
        // A quiet, optional encouragement banner - null most of the time on purpose, see
        // MotivationEngine's doc.
        val insight: MotivationInsight?,
    ) : StatisticsUiState
}
