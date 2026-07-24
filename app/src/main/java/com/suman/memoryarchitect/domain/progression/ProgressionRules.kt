package com.suman.memoryarchitect.domain.progression

/**
 * Data-driven progression tuning, mirroring [com.suman.memoryarchitect.domain.generation.GenerationRules]
 * and [com.suman.memoryarchitect.domain.scoring.ScoringRules] — swappable for a
 * Remote-Config-backed source later without touching [XpCurve].
 */
data class ProgressionRules(
    val baseXpPerLevel: Long = 100L,
    val levelCurveExponent: Double = 1.4,
    val xpPerScorePoint: Double = 1.0,
    val coinsPerScorePoint: Double = 0.2,
    /** Extra coins per combo step beyond the first — a 3-run combo earns 2 bonus steps. */
    val comboBonusCoinsPerStep: Long = 5L,
    /** Daily/Weekly Challenge don't use the score-based coin formula above at all - a "win"
     * (see [challengeWinAccuracyThreshold]) pays this flat amount instead, and anything short of
     * a win pays nothing. Same reasoning as the fixed object count/timer for these two modes: a
     * simple, predictable, guaranteed payout rather than a variable one. */
    val dailyChallengeWinCoins: Long = 200L,
    val weeklyChallengeWinCoins: Long = 500L,
    /** Reuses [com.suman.memoryarchitect.domain.progression.LevelCampaignRules
     * .passAccuracyThreshold]'s own value (0.7) rather than a fresh number - "cleared it" already
     * means the same thing everywhere else in the game. */
    val challengeWinAccuracyThreshold: Float = 0.7f,
) {
    companion object {
        val Default = ProgressionRules()
    }
}
