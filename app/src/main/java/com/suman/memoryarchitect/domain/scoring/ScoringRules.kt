package com.suman.memoryarchitect.domain.scoring

/**
 * Data-driven scoring weights, mirroring [com.suman.memoryarchitect.domain.generation.GenerationRules]
 * — swappable for a Remote-Config-backed source later without touching [ScoringEngine].
 */
data class ScoringRules(
    val basePointsPerObject: Int = 100,
    val correctPlacementBonus: Int = 50,
    val correctPlacementThreshold: Float = 0.9f,
    val rotationToleranceDegrees: Float = 15f,
    val maxRotationErrorDegrees: Float = 90f,
    /** Time bonus scales with object count (like [comboBonusPerStreakObject]) rather than a flat
     * pool - a fixed pool would shrink to a negligible fraction of the total score on larger,
     * later-campaign levels (e.g. ~5% of the max possible score at 12 objects vs ~17% at 3),
     * making completion speed feel like it stopped mattering the further a player progressed.
     * Scaling per-object keeps time's share of the total roughly constant (~13%) at every level
     * size, so a fast, skilled run always visibly outscores a slow one regardless of difficulty. */
    val timeBonusPerObject: Int = 25,
    val threeStarAccuracy: Float = 0.95f,
    val threeStarTimeBonusRatio: Float = 0.6f,
    val twoStarAccuracy: Float = 0.85f,
    val twoStarTimeBonusRatio: Float = 0.3f,
    val oneStarAccuracy: Float = 0.5f,
    /** Points per object in the streak once a combo reaches [comboBonusMinStreak]. */
    val comboBonusPerStreakObject: Int = 15,
    val comboBonusMinStreak: Int = 2,
) {
    companion object {
        val Default = ScoringRules()
    }
}
