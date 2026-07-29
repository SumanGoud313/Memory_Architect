package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.LevelConstraints
import kotlin.math.roundToInt

/**
 * Pure functions mapping a campaign level number (1..[LevelCampaignRules.maxLevel]) to
 * difficulty and unlock decisions. No I/O, no Android dependency. The actual object layout
 * for a level is never derived here: it stays randomized per attempt (see
 * [com.suman.memoryarchitect.data.repository.LevelRepositoryImpl]) so a level can't be
 * solved from memory of a previous attempt — only the difficulty constraints are stable per
 * level number.
 */
class LevelCampaignEngine(private val rules: LevelCampaignRules = LevelCampaignRules.Default) {

    /** Coarse difficulty label for display/stats only — generation itself uses [constraintsFor]. */
    fun tierFor(levelNumber: Int): DifficultyTier {
        val index = (progressFor(levelNumber) * DifficultyTier.entries.lastIndex).roundToInt()
        return DifficultyTier.entries[index.coerceIn(0, DifficultyTier.entries.lastIndex)]
    }

    fun streakFor(levelNumber: Int): Int = levelNumber - 1

    fun passed(sceneAccuracy: Float): Boolean = sceneAccuracy >= rules.passAccuracyThreshold

    fun isFinalLevel(levelNumber: Int): Boolean = levelNumber >= rules.maxLevel

    /** True exactly on the one level number where rotation first becomes required - the single
     * moment a "here's a new mechanic" callout is warranted, never on a replay of a later level
     * that also happens to have rotation on. See [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]. */
    fun isRotationDebutLevel(levelNumber: Int): Boolean = levelNumber == rules.rotationUnlockLevel

    /** Same reasoning as [isRotationDebutLevel], for order mode. */
    fun isOrderModeDebutLevel(levelNumber: Int): Boolean = levelNumber == rules.orderUnlockLevel

    /**
     * The frontier only ever advances by exactly one, never past [LevelCampaignRules.maxLevel],
     * and only when the level just completed *is* the frontier — replaying an already-cleared
     * earlier level must never skip levels or regress the frontier.
     */
    fun nextMaxUnlocked(currentMax: Int, completedLevel: Int, passed: Boolean): Int =
        if (passed && completedLevel == currentMax) (currentMax + 1).coerceAtMost(rules.maxLevel) else currentMax

    /**
     * The whole 1..[LevelCampaignRules.maxLevel] curve as one smooth, monotonic ramp. Object
     * count and distractor ratio grow gradually (the room's fixed slot list absorbs the extra
     * objects, no grid to resize); memorize time only ever grows alongside object count (never
     * shrinks, however far into the campaign); the reconstruct time budget also scales directly
     * with object count, so a level with more objects always gets proportionally more time in
     * both phases — an unhurried player who knows the scene can always finish, no matter how far
     * into the campaign.
     */
    fun constraintsFor(levelNumber: Int): LevelConstraints {
        val progress = progressFor(levelNumber)

        val objectCount = (rules.minObjectCount + progress * (rules.maxObjectCount - rules.minObjectCount))
            .roundToInt()
            .coerceIn(rules.minObjectCount, rules.maxObjectCount)

        val rotationEnabled = levelNumber >= rules.rotationUnlockLevel
        val orderModeEnabled = levelNumber >= rules.orderUnlockLevel

        // Fixed setup time plus a per-object study budget - grows with objectCount rather than
        // shrinking with level, then tops up per object again the instant rotation/order mode add
        // a further memory dimension to track for each one. See [LevelCampaignRules.memorizePerObjectMs].
        val baseMemorizeDurationMs = rules.memorizeSetupMs + objectCount * rules.memorizePerObjectMs
        val memorizeDurationMs = baseMemorizeDurationMs +
            (if (rotationEnabled) objectCount * rules.rotationMemorizePerObjectMs else 0L) +
            (if (orderModeEnabled) objectCount * rules.orderModeMemorizePerObjectMs else 0L)

        val distractorRatio = rules.minDistractorRatio + progress * (rules.maxDistractorRatio - rules.minDistractorRatio)

        val timeLimitMs = rules.baseTimeLimitMs + objectCount * rules.perObjectTimeLimitMs

        return LevelConstraints(
            objectCount = objectCount,
            distractorRatio = distractorRatio,
            memorizeDurationMs = memorizeDurationMs,
            rotationEnabled = rotationEnabled,
            rotationStepDegrees = rules.rotationStepDegrees,
            orderModeEnabled = orderModeEnabled,
            themeComplexity = (progress * 4).roundToInt(),
            timeLimitMs = timeLimitMs,
        )
    }

    private fun progressFor(levelNumber: Int): Float =
        ((levelNumber - 1).toFloat() / (rules.maxLevel - 1)).coerceIn(0f, 1f)
}
