package com.suman.memoryarchitect.domain.model

/**
 * Client-side score, computed for instant feedback. For scored modes this is provisional
 * only — the server independently recomputes the score from the level's seed and the
 * submitted placements, and its number is authoritative for progression/leaderboards.
 */
data class ScoreResult(
    val objectScores: List<ObjectScore>,
    val sceneAccuracy: Float,
    /** Sum of per-object points before time/combo bonuses — the "placement score" line item. */
    val placementScore: Int,
    val timeBonus: Int,
    /** Bonus points from [comboCount], folded into [finalScore]. */
    val comboBonus: Int,
    val finalScore: Int,
    /** Longest run of consecutive correct placements, in the order the player placed them. */
    val comboCount: Int,
)
