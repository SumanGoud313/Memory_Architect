package com.suman.memoryarchitect.domain.scoring

import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.ObjectScore
import com.suman.memoryarchitect.domain.model.PlacedObject
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import com.suman.memoryarchitect.domain.model.ScoreResult
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Client-side implementation of the documented scoring formula. Pure function, no I/O —
 * computed for instant feedback only; scored modes still defer to the server's independent
 * recomputation from the level seed + submitted placements as authoritative.
 */
class ScoringEngine(private val rules: ScoringRules = ScoringRules.Default) {

    fun score(
        level: LevelSpec,
        placements: List<PlacedObject>,
        placementOrder: List<String>,
        remainingReconstructMs: Long?,
    ): ScoreResult {
        val placedById = placements.associateBy { it.objectId }
        val targetRanks = level.objects.filterNot { it.isDistractor }
            .mapIndexed { index, spec -> spec.objectId to index }
            .toMap()
        val placedRanks = placementOrder.mapIndexed { index, objectId -> objectId to index }.toMap()

        val objectScores = level.objects.map { target ->
            val placed = placedById[target.objectId]
            if (target.isDistractor) {
                scoreDistractor(target.objectId, placed)
            } else {
                scoreTarget(level, target, placed, targetRanks, placedRanks)
            }
        }

        val totalPoints = objectScores.sumOf { it.points }
        val maxPossiblePerObject = rules.basePointsPerObject + rules.correctPlacementBonus
        val maxPossibleTotal = level.objects.size * maxPossiblePerObject
        val sceneAccuracy = if (maxPossibleTotal > 0) {
            (totalPoints.toFloat() / maxPossibleTotal).coerceIn(0f, 1f)
        } else {
            0f
        }

        val timeBonus = computeTimeBonus(level.objects.size, level.timeLimitMs, remainingReconstructMs)
        val comboCount = longestCorrectPlacementStreak(objectScores, placementOrder)
        val comboBonus = if (comboCount >= rules.comboBonusMinStreak) comboCount * rules.comboBonusPerStreakObject else 0

        return ScoreResult(
            objectScores = objectScores,
            sceneAccuracy = sceneAccuracy,
            placementScore = totalPoints,
            timeBonus = timeBonus,
            comboBonus = comboBonus,
            finalScore = totalPoints + timeBonus + comboBonus,
            comboCount = comboCount,
        )
    }

    /**
     * Longest run of consecutive *correct* placements in the order the player actually placed
     * them — computed only from the final submitted result, never surfaced live during drag, so
     * it can't turn into trial-and-error correctness feedback mid-Reconstruct.
     */
    private fun longestCorrectPlacementStreak(objectScores: List<ObjectScore>, placementOrder: List<String>): Int {
        val correctById = objectScores.associate { it.objectId to it.correctPlacementBonusAwarded }
        var longest = 0
        var current = 0
        placementOrder.forEach { objectId ->
            if (correctById[objectId] == true) {
                current += 1
                longest = maxOf(longest, current)
            } else {
                current = 0
            }
        }
        return longest
    }

    private fun scoreTarget(
        level: LevelSpec,
        target: SceneObjectSpec,
        placed: PlacedObject?,
        targetRanks: Map<String, Int>,
        placedRanks: Map<String, Int>,
    ): ObjectScore {
        if (placed == null) {
            return ObjectScore(target.objectId, positionAccuracy = 0f, rotationAccuracy = 0f, orderAccuracy = 0f, points = 0, correctPlacementBonusAwarded = false)
        }
        val positionAccuracy = positionAccuracyFactor(target, placed)
        val rotationAccuracy = rotationAccuracyFactor(target.rotationDegrees, placed.rotationDegrees)
        val orderAccuracy = if (level.orderModeEnabled) {
            orderAccuracyFactor(target.objectId, targetRanks, placedRanks)
        } else {
            1f
        }
        val correctPlacement = positionAccuracy >= rules.correctPlacementThreshold
        val basePoints = (rules.basePointsPerObject * positionAccuracy * rotationAccuracy * orderAccuracy).roundToInt()
        val points = basePoints + if (correctPlacement) rules.correctPlacementBonus else 0
        return ObjectScore(target.objectId, positionAccuracy, rotationAccuracy, orderAccuracy, points, correctPlacement)
    }

    /** Distractors correctly left unplaced earn full credit; placing one is penalized to zero. */
    private fun scoreDistractor(objectId: String, placed: PlacedObject?): ObjectScore = if (placed == null) {
        ObjectScore(objectId, positionAccuracy = 1f, rotationAccuracy = 1f, orderAccuracy = 1f, points = rules.basePointsPerObject, correctPlacementBonusAwarded = false)
    } else {
        ObjectScore(objectId, positionAccuracy = 0f, rotationAccuracy = 0f, orderAccuracy = 0f, points = 0, correctPlacementBonusAwarded = false)
    }

    /**
     * How close the player's placement order for this object was to its reveal rank — targets
     * never placed have no rank in [placedRanks] and score zero order accuracy.
     */
    private fun orderAccuracyFactor(objectId: String, targetRanks: Map<String, Int>, placedRanks: Map<String, Int>): Float {
        val targetRank = targetRanks[objectId] ?: return 1f
        val placedRank = placedRanks[objectId] ?: return 0f
        val maxRank = (targetRanks.size - 1).coerceAtLeast(1)
        return (1f - abs(targetRank - placedRank).toFloat() / maxRank).coerceIn(0f, 1f)
    }

    /**
     * Slots are designed furniture positions, not a uniform grid — there's no meaningful
     * "distance" between two of them, so placement is either the exact correct slot or it
     * isn't. (Magnetic snapping means every placement lands exactly on some slot anyway.)
     */
    private fun positionAccuracyFactor(target: SceneObjectSpec, placed: PlacedObject): Float =
        if (target.slotIndex == placed.slotIndex) 1f else 0f

    private fun rotationAccuracyFactor(targetDegrees: Int, placedDegrees: Int): Float {
        val delta = circularDelta(targetDegrees, placedDegrees)
        return when {
            delta <= rules.rotationToleranceDegrees -> 1f
            delta >= rules.maxRotationErrorDegrees -> 0f
            else -> {
                val range = rules.maxRotationErrorDegrees - rules.rotationToleranceDegrees
                (1f - (delta - rules.rotationToleranceDegrees) / range).coerceIn(0f, 1f)
            }
        }
    }

    private fun circularDelta(a: Int, b: Int): Float {
        val diff = ((a - b) % 360 + 360) % 360
        return min(diff, 360 - diff).toFloat()
    }

    /** [objectCount]-scaled, not a flat pool - see [ScoringRules.timeBonusPerObject]'s doc for why.
     * `sqrt(ratio)` (rather than a linear ratio) rewards finishing with *any* real time to spare
     * fairly generously, only tailing off sharply right at the very end of the clock - a player
     * who finishes with half the time left already earns ~71% of the max bonus, not 50%. */
    private fun computeTimeBonus(objectCount: Int, timeLimitMs: Long?, remainingMs: Long?): Int {
        if (timeLimitMs == null || timeLimitMs <= 0 || remainingMs == null) return 0
        val ratio = (remainingMs.toFloat() / timeLimitMs).coerceIn(0f, 1f)
        return (objectCount * rules.timeBonusPerObject * sqrt(ratio)).roundToInt()
    }
}
