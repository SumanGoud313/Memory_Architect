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
    /**
     * Fixed overhead every Memorize phase gets regardless of object count - the moment to orient
     * to a new room before any per-object study time is even needed.
     */
    val memorizeSetupMs: Long = 4_000L,
    /**
     * Per-target study time, scaled by [LevelCampaignEngine.constraintsFor]'s objectCount rather
     * than a flat/shrinking duration - a real human can't encode more objects (up to 12 by L100)
     * in *less* time than fewer objects needed at L1, which the old curve did (it shrank from
     * 11s to 9.5s as object count nearly quadrupled). Total memorize time now only ever grows
     * with how much there actually is to remember. See the level-design audit on realistic human
     * recall limits.
     */
    val memorizePerObjectMs: Long = 900L,
    val minDistractorRatio: Float = 0.1f,
    val maxDistractorRatio: Float = 0.3f,
    val rotationUnlockLevel: Int = 30,
    val rotationStepDegrees: Int = 90,
    val orderUnlockLevel: Int = 55,
    /**
     * Per-object memorize-time top-ups the instant rotation/order mode become required, scaled by
     * objectCount rather than a flat bonus. Each one adds a real memory *dimension* per object
     * (angle, then serial position), so the compensation has to grow with how many objects are
     * carrying that extra dimension - a flat bonus left a 12-object L100 just as time-starved per
     * object as an 8-object L55 despite tracking half again as many. See the level-design audit.
     */
    val rotationMemorizePerObjectMs: Long = 150L,
    val orderModeMemorizePerObjectMs: Long = 250L,
    val baseTimeLimitMs: Long = 15_000L,
    val perObjectTimeLimitMs: Long = 5_500L,
) {
    companion object {
        val Default = LevelCampaignRules()
    }
}
