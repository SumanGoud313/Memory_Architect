package com.suman.memoryarchitect.domain.progression

/**
 * Data-driven tuning knobs for the 1..[maxLevel] Classic campaign. Plain data class (not
 * hardcoded logic) so balancing is a data change, not a code change — mirrors
 * [com.suman.memoryarchitect.domain.generation.GenerationRules]. The whole curve is a
 * smooth, monotonic ramp from level 1 (gentle introduction) to [maxLevel] (the hardest the
 * generator ever produces) — no single level spikes in difficulty, and the reconstruct-phase
 * time budget always scales with object count so a careful player can realistically finish
 * every level, however far into the campaign.
 */
data class LevelCampaignRules(
    val maxLevel: Int = 100,
    val passAccuracyThreshold: Float = 0.7f,
    val minObjectCount: Int = 3,
    val maxObjectCount: Int = 12,
    val maxMemorizeDurationMs: Long = 11_000L,
    // Softened on purpose: difficulty should come from more objects/distractors/rotation and
    // richer room layouts, not a shrinking memorize window.
    val minMemorizeDurationMs: Long = 9_500L,
    val minDistractorRatio: Float = 0.1f,
    val maxDistractorRatio: Float = 0.3f,
    val rotationUnlockLevel: Int = 30,
    val rotationStepDegrees: Int = 90,
    val orderUnlockLevel: Int = 55,
    /**
     * Flat memorize-time top-ups the instant rotation/order mode become required, on top of the
     * base curve above. Each one adds a real memory *dimension* per object (angle, then serial
     * position) rather than just more objects - the base curve alone was tuned against object
     * count only, so seconds-of-memorize-time-per-object was falling well below what's realistic
     * by the time both stack on an already-tight base window. See the level-design audit.
     */
    val rotationMemorizeBonusMs: Long = 1_500L,
    val orderModeMemorizeBonusMs: Long = 2_000L,
    val baseTimeLimitMs: Long = 15_000L,
    val perObjectTimeLimitMs: Long = 5_500L,
) {
    companion object {
        val Default = LevelCampaignRules()
    }
}
