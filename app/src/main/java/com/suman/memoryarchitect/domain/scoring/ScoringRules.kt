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
    val maxTimeBonusPoints: Int = 200,
    val timeBonusWeight: Float = 0.5f,
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
