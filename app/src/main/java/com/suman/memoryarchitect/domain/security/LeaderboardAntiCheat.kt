package com.suman.memoryarchitect.domain.security

import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.scoring.ScoringRules

/**
 * The client-side half of leaderboard integrity: a cheap, pure sanity check run immediately
 * before any [com.suman.memoryarchitect.domain.repository.LeaderboardRepository] write, so a
 * corrupted/tampered local [ScoreResult] (a modified APK, a memory-edit tool, a bug) can never
 * even attempt to reach Firestore. This is a first line of defense, not the authoritative one - a
 * motivated attacker can still bypass client code entirely and write to Firestore directly with
 * forged credentials, which is exactly what `firestore.rules`' own shape/range bounds
 * (`isValidGlobalStats`/`isValidPeriodicEntry`) exist to close - this project runs no Cloud
 * Function to compare a write against a player's own history on top of that (an accepted trade-off
 * of staying on the free Firebase Spark plan). [isPlausible] deliberately never *lowers* what the
 * client already computed - it only ever rejects outright, so it can't be used to invent a better
 * score than [ScoringEngine] actually produced.
 */
object LeaderboardAntiCheat {

    fun isPlausible(score: ScoreResult, level: LevelSpec, timeTakenMs: Long, rules: ScoringRules = ScoringRules.Default): Boolean {
        if (score.sceneAccuracy < 0f || score.sceneAccuracy > 1f) return false
        if (score.finalScore < 0) return false
        if (score.comboCount < 0 || score.comboCount > level.objects.size) return false
        if (timeTakenMs < 0 || timeTakenMs > MAX_PLAUSIBLE_ROUND_DURATION_MS) return false
        return score.finalScore <= maxPlausibleScore(level, rules)
    }

    /** The theoretical ceiling every real, honestly-computed [ScoreResult.finalScore] for this
     * [level] must fall at or under: every object landing perfectly (base + placement bonus),
     * the maximum possible time bonus, and the maximum possible combo bonus (every object part of
     * one unbroken streak) - a score above this can only come from a value [ScoringEngine] itself
     * would never produce. */
    private fun maxPlausibleScore(level: LevelSpec, rules: ScoringRules): Int {
        val maxPerObject = rules.basePointsPerObject + rules.correctPlacementBonus
        val maxPlacementScore = level.objects.size * maxPerObject
        val maxTimeBonus = level.objects.size * rules.timeBonusPerObject
        val maxComboBonus = level.objects.size * rules.comboBonusPerStreakObject
        return maxPlacementScore + maxTimeBonus + maxComboBonus
    }

    /** No legitimate Reconstruct round runs longer than this - a generous ceiling (every level's
     * real time limit is well under this) that exists only to reject an obviously-impossible
     * value (a clock/timestamp bug, a tampered duration), not to police normal play speed. */
    private const val MAX_PLAUSIBLE_ROUND_DURATION_MS = 30 * 60_000L // 30 minutes
}
