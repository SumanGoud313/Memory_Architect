package com.suman.memoryarchitect.domain.scoring

import com.suman.memoryarchitect.domain.model.ScoreResult

/**
 * Pure function from a [ScoreResult] to a 0-3 star rating, blending accuracy with how much of
 * the time-bonus window was earned — so a slow-but-perfect rebuild and a fast-but-sloppy one
 * both fall short of 3 stars. No I/O, thresholds live in [ScoringRules] so balancing is a data
 * change, not a code change.
 */
object StarRatingCalculator {

    fun calculate(result: ScoreResult, rules: ScoringRules = ScoringRules.Default): Int {
        val timeBonusRatio = if (rules.maxTimeBonusPoints > 0) {
            (result.timeBonus.toFloat() / rules.maxTimeBonusPoints).coerceIn(0f, 1f)
        } else {
            0f
        }

        return when {
            result.sceneAccuracy >= rules.threeStarAccuracy && timeBonusRatio >= rules.threeStarTimeBonusRatio -> 3
            result.sceneAccuracy >= rules.twoStarAccuracy && timeBonusRatio >= rules.twoStarTimeBonusRatio -> 2
            result.sceneAccuracy >= rules.oneStarAccuracy -> 1
            else -> 0
        }
    }
}
