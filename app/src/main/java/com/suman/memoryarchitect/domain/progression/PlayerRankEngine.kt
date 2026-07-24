package com.suman.memoryarchitect.domain.progression

/**
 * Competitive rank tiers — a second, parallel read of a player's standing alongside their raw XP
 * [com.suman.memoryarchitect.domain.progression.XpCurve] level. Deliberately requires *both* a
 * minimum level *and* a minimum lifetime [com.suman.memoryarchitect.domain.model.PlayerStatistics.averageAccuracy]
 * to advance - level alone rewards time spent, accuracy alone rewards a lucky early streak before
 * facing harder content, but both together is what "sustained progress and skill, not just time
 * played" (the design brief) actually means: a player who grinds levels sloppily caps out below a
 * player of the same level who's also been consistently accurate.
 */
enum class PlayerRank(
    val minLevel: Int,
    val minAverageAccuracy: Float,
) {
    BRONZE(minLevel = 1, minAverageAccuracy = 0f),
    SILVER(minLevel = 6, minAverageAccuracy = 0.50f),
    GOLD(minLevel = 14, minAverageAccuracy = 0.58f),
    PLATINUM(minLevel = 24, minAverageAccuracy = 0.65f),
    DIAMOND(minLevel = 36, minAverageAccuracy = 0.72f),
    MASTER(minLevel = 50, minAverageAccuracy = 0.78f),
    GRANDMASTER(minLevel = 68, minAverageAccuracy = 0.84f),
    LEGEND(minLevel = 90, minAverageAccuracy = 0.90f),
}

/** (current rank, the next rank up - null once already [PlayerRank.LEGEND], and how far through
 * that next rank's requirements the player already is, 0..1). [progressToNext] is the *minimum* of
 * the level-fraction and accuracy-fraction toward [next]'s thresholds - i.e. whichever requirement
 * is further behind is what actually gates the bar, so it never reads "almost there" while one of
 * the two numbers is nowhere close. */
data class RankStanding(
    val current: PlayerRank,
    val next: PlayerRank?,
    val progressToNext: Float,
)

object PlayerRankEngine {
    /** The highest rank whose *both* thresholds are already met - ranks are evaluated in
     * ascending order and the last one satisfied wins, so meeting Gold's level but not its
     * accuracy still correctly reports Silver (or whatever the accuracy alone qualifies for). */
    fun rankFor(level: Int, averageAccuracy: Float): PlayerRank =
        PlayerRank.entries.lastOrNull { level >= it.minLevel && averageAccuracy >= it.minAverageAccuracy }
            ?: PlayerRank.BRONZE

    fun standingFor(level: Int, averageAccuracy: Float): RankStanding {
        val current = rankFor(level, averageAccuracy)
        val next = PlayerRank.entries.getOrNull(current.ordinal + 1) ?: return RankStanding(current, null, 1f)
        val levelFraction = if (next.minLevel == current.minLevel) {
            1f
        } else {
            ((level - current.minLevel).toFloat() / (next.minLevel - current.minLevel)).coerceIn(0f, 1f)
        }
        val accuracyFraction = if (next.minAverageAccuracy == current.minAverageAccuracy) {
            1f
        } else {
            ((averageAccuracy - current.minAverageAccuracy) / (next.minAverageAccuracy - current.minAverageAccuracy)).coerceIn(0f, 1f)
        }
        return RankStanding(current, next, minOf(levelFraction, accuracyFraction))
    }
}
