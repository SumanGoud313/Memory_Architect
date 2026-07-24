package com.suman.memoryarchitect.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row cumulative lifetime stats. Evaluated/updated locally on each score submission - see
 * [com.suman.memoryarchitect.domain.model.PlayerStatistics] for what each field means. */
@Entity(tableName = "statistics_cache")
data class StatisticsCacheEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val gamesPlayed: Int,
    val totalScore: Long,
    val bestAccuracy: Float,
    val bestScore: Int,
    val totalAccuracySum: Double = 0.0,
    val totalTimeTakenMs: Long = 0L,
    val fastestCompletionMs: Long? = null,
    val perfectGames: Int = 0,
    val highestCombo: Int = 0,
    val objectsMemorized: Int = 0,
    val daysPlayed: Int = 0,
    val lastStatsUpdateEpochDay: Long? = null,
    val dailyChallengesWon: Int = 0,
    val weeklyChallengesWon: Int = 0,
    val dailyBestRank: Int? = null,
    val weeklyBestRank: Int? = null,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
