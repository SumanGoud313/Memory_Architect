package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.LevelConstraints
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
     * objects, no grid to resize); memorize time only ever eases down slightly (never to
     * anything punishing); the reconstruct time budget scales directly with object count, so a
     * level with more objects always gets proportionally more time — an unhurried player who
     * knows the scene can always finish, no matter how far into the campaign.
     */
    fun constraintsFor(levelNumber: Int): LevelConstraints {
        val progress = progressFor(levelNumber)

        val objectCount = (rules.minObjectCount + progress * (rules.maxObjectCount - rules.minObjectCount))
            .roundToInt()
            .coerceIn(rules.minObjectCount, rules.maxObjectCount)

        val rotationEnabled = levelNumber >= rules.rotationUnlockLevel
        val orderModeEnabled = levelNumber >= rules.orderUnlockLevel

        // The base curve alone (tuned against object count only) plus a flat top-up the instant
        // rotation/order mode are active - see [LevelCampaignRules.rotationMemorizeBonusMs].
        val baseMemorizeDurationMs = (rules.maxMemorizeDurationMs -
            progress * (rules.maxMemorizeDurationMs - rules.minMemorizeDurationMs))
            .roundToLong()
        val memorizeDurationMs = baseMemorizeDurationMs +
            (if (rotationEnabled) rules.rotationMemorizeBonusMs else 0L) +
            (if (orderModeEnabled) rules.orderModeMemorizeBonusMs else 0L)

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
