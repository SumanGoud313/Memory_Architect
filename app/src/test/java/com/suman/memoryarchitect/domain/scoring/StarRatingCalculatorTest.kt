package com.suman.memoryarchitect.domain.scoring

import com.suman.memoryarchitect.domain.model.ScoreResult
import org.junit.Assert.assertEquals
import org.junit.Test

class StarRatingCalculatorTest {

    private fun result(sceneAccuracy: Float, timeBonus: Int = 0) = ScoreResult(
        objectScores = emptyList(),
        sceneAccuracy = sceneAccuracy,
        placementScore = 0,
        timeBonus = timeBonus,
        comboBonus = 0,
        finalScore = 0,
        comboCount = 0,
    )

    @Test
    fun `high accuracy and time bonus earns three stars`() {
        val stars = StarRatingCalculator.calculate(
            result(sceneAccuracy = 0.97f, timeBonus = (ScoringRules.Default.maxTimeBonusPoints * 0.8f).toInt()),
        )
        assertEquals(3, stars)
    }

    @Test
    fun `high accuracy but insufficient time bonus falls short of three stars`() {
        val stars = StarRatingCalculator.calculate(
            result(sceneAccuracy = 0.97f, timeBonus = (ScoringRules.Default.maxTimeBonusPoints * 0.4f).toInt()),
        )
        assertEquals(2, stars)
    }

    @Test
    fun `high accuracy with no time bonus at all still earns one star`() {
        val stars = StarRatingCalculator.calculate(
            result(sceneAccuracy = 0.97f, timeBonus = 0),
        )
        assertEquals(1, stars)
    }

    @Test
    fun `moderate accuracy earns two stars`() {
        val stars = StarRatingCalculator.calculate(
            result(sceneAccuracy = 0.9f, timeBonus = (ScoringRules.Default.maxTimeBonusPoints * 0.5f).toInt()),
        )
        assertEquals(2, stars)
    }

    @Test
    fun `a weak but passing attempt earns one star`() {
        val stars = StarRatingCalculator.calculate(result(sceneAccuracy = 0.6f))
        assertEquals(1, stars)
    }

    @Test
    fun `very low accuracy earns zero stars`() {
        val stars = StarRatingCalculator.calculate(result(sceneAccuracy = 0.2f))
        assertEquals(0, stars)
    }
}
