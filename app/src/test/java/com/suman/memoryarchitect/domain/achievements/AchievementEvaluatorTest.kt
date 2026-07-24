package com.suman.memoryarchitect.domain.achievements

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {

    private val evaluator = AchievementEvaluator()

    @Test
    fun `first game unlocks first steps only`() {
        val snapshot = ProgressSnapshot(
            statistics = PlayerStatistics.EMPTY.copy(gamesPlayed = 1),
            profile = PlayerProfile.EMPTY,
            level = 1,
        )

        val unlocked = evaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked = emptySet())

        assertEquals(listOf(AchievementId.FIRST_STEPS), unlocked)
    }

    @Test
    fun `already unlocked achievements are never returned again`() {
        val snapshot = ProgressSnapshot(
            statistics = PlayerStatistics.EMPTY.copy(gamesPlayed = 1),
            profile = PlayerProfile.EMPTY,
            level = 1,
        )

        val unlocked = evaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked = setOf(AchievementId.FIRST_STEPS))

        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `crossing multiple thresholds at once unlocks all of them together`() {
        val snapshot = ProgressSnapshot(
            statistics = PlayerStatistics(gamesPlayed = 100, totalScore = 0L, bestAccuracy = 1f, bestScore = 500),
            profile = PlayerProfile.EMPTY.copy(longestStreak = 30),
            level = 10,
        )

        val unlocked = evaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked = emptySet())

        assertEquals(
            setOf(
                AchievementId.FIRST_STEPS, AchievementId.DEDICATED, AchievementId.CENTURY,
                AchievementId.SHARP_EYE, AchievementId.PERFECTIONIST,
                AchievementId.WEEK_STREAK, AchievementId.MONTH_STREAK,
                AchievementId.RISING_STAR, AchievementId.ARCHITECT,
            ),
            unlocked.toSet(),
        )
    }

    @Test
    fun `below every threshold unlocks nothing`() {
        val snapshot = ProgressSnapshot(
            statistics = PlayerStatistics.EMPTY,
            profile = PlayerProfile.EMPTY,
            level = 1,
        )

        val unlocked = evaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked = emptySet())

        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `a first three-star clear unlocks flawless only, not grand architect`() {
        val snapshot = ProgressSnapshot(
            statistics = PlayerStatistics.EMPTY,
            profile = PlayerProfile.EMPTY,
            level = 1,
            hasThreeStarClear = true,
        )

        val unlocked = evaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked = emptySet())

        assertEquals(listOf(AchievementId.FLAWLESS), unlocked)
    }

    @Test
    fun `beating the final campaign level unlocks grand architect only, not flawless`() {
        val snapshot = ProgressSnapshot(
            statistics = PlayerStatistics.EMPTY,
            profile = PlayerProfile.EMPTY,
            level = 1,
            hasCompletedCampaign = true,
        )

        val unlocked = evaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked = emptySet())

        assertEquals(listOf(AchievementId.GRAND_ARCHITECT), unlocked)
    }
}
