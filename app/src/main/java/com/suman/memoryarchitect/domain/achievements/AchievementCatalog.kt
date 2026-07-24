package com.suman.memoryarchitect.domain.achievements

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics

data class ProgressSnapshot(
    val statistics: PlayerStatistics,
    val profile: PlayerProfile,
    val level: Int,
    /** True once level 100 (the Classic campaign's final level) has been passed at least once -
     * not merely unlocked, since the frontier can reach 100 without the player having actually
     * beaten it yet. Defaults false so every existing caller/test is unaffected. */
    val hasCompletedCampaign: Boolean = false,
    /** True once any Classic level has ever been cleared with a 3-star rating. */
    val hasThreeStarClear: Boolean = false,
)

data class AchievementDefinition(
    val id: AchievementId,
    val isSatisfiedBy: (ProgressSnapshot) -> Boolean,
)

/** Static, client-known catalog — achievements are recognition, evaluated locally. */
object AchievementCatalog {
    val definitions: List<AchievementDefinition> = listOf(
        AchievementDefinition(AchievementId.FIRST_STEPS) { it.statistics.gamesPlayed >= 1 },
        AchievementDefinition(AchievementId.DEDICATED) { it.statistics.gamesPlayed >= 10 },
        AchievementDefinition(AchievementId.CENTURY) { it.statistics.gamesPlayed >= 100 },
        AchievementDefinition(AchievementId.SHARP_EYE) { it.statistics.bestAccuracy >= 0.9f },
        AchievementDefinition(AchievementId.PERFECTIONIST) { it.statistics.bestAccuracy >= 1f },
        AchievementDefinition(AchievementId.WEEK_STREAK) { it.profile.longestStreak >= 7 },
        AchievementDefinition(AchievementId.MONTH_STREAK) { it.profile.longestStreak >= 30 },
        AchievementDefinition(AchievementId.RISING_STAR) { it.level >= 5 },
        AchievementDefinition(AchievementId.ARCHITECT) { it.level >= 10 },
        AchievementDefinition(AchievementId.FLAWLESS) { it.hasThreeStarClear },
        AchievementDefinition(AchievementId.GRAND_ARCHITECT) { it.hasCompletedCampaign },
        AchievementDefinition(AchievementId.FIRST_PERFECT_ROOM) { it.statistics.perfectGames >= 1 },
        AchievementDefinition(AchievementId.PERFECT_ROOMS_10) { it.statistics.perfectGames >= 10 },
        AchievementDefinition(AchievementId.PERFECT_ROOMS_100) { it.statistics.perfectGames >= 100 },
        AchievementDefinition(AchievementId.LEVEL_25) { it.level >= 25 },
        AchievementDefinition(AchievementId.LEVEL_50) { it.level >= 50 },
        AchievementDefinition(AchievementId.LEVEL_100) { it.level >= 100 },
        AchievementDefinition(AchievementId.TOP_100_DAILY) { (it.statistics.dailyBestRank ?: Int.MAX_VALUE) <= 100 },
        AchievementDefinition(AchievementId.TOP_10_WEEKLY) { (it.statistics.weeklyBestRank ?: Int.MAX_VALUE) <= 10 },
        AchievementDefinition(AchievementId.MEMORY_MASTER) { it.statistics.objectsMemorized >= 1000 },
        AchievementDefinition(AchievementId.SPEED_RUNNER) { (it.statistics.fastestCompletionMs ?: Long.MAX_VALUE) <= SPEED_RUNNER_THRESHOLD_MS },
        AchievementDefinition(AchievementId.PRECISION_EXPERT) { it.statistics.gamesPlayed >= 20 && it.statistics.averageAccuracy >= 0.85f },
    )

    /** Reconstruct completed in 15 seconds or less - fast enough to be a genuine feat rather than
     * a trivially short/early level, since most levels' time limits run well past this. */
    private const val SPEED_RUNNER_THRESHOLD_MS = 15_000L
}
