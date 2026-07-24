package com.suman.memoryarchitect.domain.motivation

import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.progression.PlayerRankEngine
import kotlin.math.roundToInt

/**
 * Every insight is framed around the player's own improvement, never a loss/pressure comparison
 * ("you fell behind," "hurry up") - see the design brief's "healthy competition" requirement.
 * Deliberately not a single "pick one" API: a results screen or the Profile/Statistics dashboard
 * calls whichever [MotivationEngine] function fits the data it already has, and shows whatever
 * comes back (often nothing at all - a quiet round with no milestone is not itself surfaced as a
 * message, which is the point: these are moments worth celebrating, not a message on every screen).
 */
sealed interface MotivationInsight {
    data object NewPersonalBest : MotivationInsight
    data class LevelsToNextRank(val levels: Int, val nextRank: com.suman.memoryarchitect.domain.progression.PlayerRank) : MotivationInsight
    data class AccuracyImproved(val percentPoints: Int) : MotivationInsight
    data class ObjectsMemorizedMilestone(val total: Int) : MotivationInsight
    data class TopPercentThisWeek(val percent: Int) : MotivationInsight
}

object MotivationEngine {
    /** Round-number milestones worth calling out - deliberately sparse (not every 10) so it stays
     * a genuine "moment," not background noise on every other round. */
    private val OBJECT_MILESTONES = listOf(50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000)

    /** Called right after a scored round resolves - [statisticsAfter] is what
     * [com.suman.memoryarchitect.domain.usecase.SubmitScoreUseCase] just returned, [thisRoundScore]
     * is the same [ScoreResult] that was submitted. Both "accuracy improved" and "milestone
     * crossed" are reconstructed algebraically from before/after totals already on hand - no
     * separate "snapshot before the round" plumbing needed anywhere upstream. */
    fun insightsForCompletedRound(
        statisticsAfter: PlayerStatistics,
        thisRoundScore: ScoreResult,
        isNewPersonalBest: Boolean,
    ): List<MotivationInsight> {
        val insights = mutableListOf<MotivationInsight>()
        if (isNewPersonalBest) insights += MotivationInsight.NewPersonalBest

        accuracyImprovedInsight(statisticsAfter, thisRoundScore.sceneAccuracy)?.let { insights += it }

        val objectsBefore = statisticsAfter.objectsMemorized - thisRoundScore.objectScores.size
        milestoneInsight(objectsBefore, statisticsAfter.objectsMemorized)?.let { insights += it }

        return insights
    }

    /** This round's accuracy vs. the player's lifetime average *before* this round - reconstructed
     * from [statisticsAfter]'s running sum rather than needing a separately captured "before"
     * snapshot: `averageBefore = (sumAfter - thisRoundAccuracy) / (gamesAfter - 1)`. Only reported
     * once at least one prior round exists (a first-ever round has no "before" to compare against)
     * and only when the improvement is large enough to be a genuine signal, not rounding noise. */
    private fun accuracyImprovedInsight(statisticsAfter: PlayerStatistics, thisRoundAccuracy: Float): MotivationInsight? {
        val gamesBefore = statisticsAfter.gamesPlayed - 1
        if (gamesBefore < 1) return null
        val sumBefore = statisticsAfter.totalAccuracySum - thisRoundAccuracy
        val averageBefore = (sumBefore / gamesBefore).toFloat()
        val deltaPoints = ((thisRoundAccuracy - averageBefore) * 100).roundToInt()
        return if (deltaPoints >= MIN_NOTABLE_ACCURACY_DELTA_POINTS) MotivationInsight.AccuracyImproved(deltaPoints) else null
    }

    private fun milestoneInsight(before: Int, after: Int): MotivationInsight? {
        val crossed = OBJECT_MILESTONES.lastOrNull { it in (before + 1)..after } ?: return null
        return MotivationInsight.ObjectsMemorizedMilestone(crossed)
    }

    /** Purely a function of current standing (level + lifetime average accuracy) - safe to call
     * anytime (Profile, Statistics dashboard), no round-completion context needed. Only surfaced
     * when genuinely close (1-2 levels away), matching the "you're only 2 levels away" framing -
     * further out reads as a demand, not an encouragement. */
    fun levelsToNextRankInsight(level: Int, averageAccuracy: Float): MotivationInsight? {
        val standing = PlayerRankEngine.standingFor(level, averageAccuracy)
        val next = standing.next ?: return null
        val levelsAway = next.minLevel - level
        return if (levelsAway in 1..2) MotivationInsight.LevelsToNextRank(levelsAway, next) else null
    }

    /** [rank]/[totalParticipants] come from a Leaderboard fetch - surfaced only for a genuine
     * top-10% result. */
    fun topPercentInsight(rank: Int, totalParticipants: Int): MotivationInsight? {
        if (totalParticipants <= 0) return null
        val percent = ((rank.toFloat() / totalParticipants) * 100).roundToInt().coerceAtLeast(1)
        return if (percent <= TOP_PERCENT_THRESHOLD) MotivationInsight.TopPercentThisWeek(percent) else null
    }

    private const val MIN_NOTABLE_ACCURACY_DELTA_POINTS = 2
    private const val TOP_PERCENT_THRESHOLD = 10
}
