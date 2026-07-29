package com.suman.memoryarchitect.domain.progression

/**
 * Data-driven progression tuning, mirroring [com.suman.memoryarchitect.domain.generation.GenerationRules]
 * and [com.suman.memoryarchitect.domain.scoring.ScoringRules] — swappable for a
 * Remote-Config-backed source later without touching [XpCurve].
 */
data class ProgressionRules(
    // Was 100L/1.4 - that curve let a single well-played round (a good round can easily score
    // 900-1500+ XP, per ScoringRules' per-object bonuses) blow past level 10 (2,040 XP) in 1-2
    // rounds. 500L/1.5 needs ~9-14 good rounds for the same level 10, and scales up faster at
    // higher levels too (level 20 goes from ~4.8k XP to ~41k XP) - leveling now tracks sustained
    // play across many sessions instead of being trivially maxed out in one sitting.
    val baseXpPerLevel: Long = 500L,
    val levelCurveExponent: Double = 1.5,
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
