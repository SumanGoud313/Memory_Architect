package com.suman.memoryarchitect.domain.scoring

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.PlacedObject
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {

    private val engine = ScoringEngine()

    private val target = SceneObjectSpec(
        objectId = "vase",
        slotIndex = 2,
        rotationDegrees = 90,
        scale = 1f,
        isDistractor = false,
    )
    private val distractor = SceneObjectSpec(
        objectId = "keys",
        slotIndex = 0,
        rotationDegrees = 0,
        scale = 1f,
        isDistractor = true,
    )

    private fun level(objects: List<SceneObjectSpec>, timeLimitMs: Long? = null, orderModeEnabled: Boolean = false) = LevelSpec(
        seed = 1L,
        mode = GameMode.CLASSIC,
        difficultyTier = DifficultyTier.MEDIUM,
        sceneType = "kitchen",
        objects = objects,
        timeLimitMs = timeLimitMs,
        memorizeDurationMs = 5_000L,
        orderModeEnabled = orderModeEnabled,
    )

    @Test
    fun `exact position and rotation earns full points plus the correct-placement bonus`() {
        val result = engine.score(
            level = level(listOf(target)),
            placements = listOf(PlacedObject("vase", 2, 90)),
            placementOrder = listOf("vase"),
            remainingReconstructMs = null,
        )

        val score = result.objectScores.single()
        assertEquals(1f, score.positionAccuracy, 0.001f)
        assertEquals(1f, score.rotationAccuracy, 0.001f)
        assertTrue(score.correctPlacementBonusAwarded)
        assertEquals(ScoringRules.Default.basePointsPerObject + ScoringRules.Default.correctPlacementBonus, score.points)
    }

    @Test
    fun `missing an object scores zero for that object`() {
        val result = engine.score(level(listOf(target)), placements = emptyList(), placementOrder = emptyList(), remainingReconstructMs = null)

        val score = result.objectScores.single()
        assertEquals(0, score.points)
        assertEquals(0f, result.sceneAccuracy, 0.001f)
    }

    @Test
    fun `distractor correctly left unplaced earns full credit`() {
        val result = engine.score(level(listOf(distractor)), placements = emptyList(), placementOrder = emptyList(), remainingReconstructMs = null)

        val score = result.objectScores.single()
        assertEquals(ScoringRules.Default.basePointsPerObject, score.points)
    }

    @Test
    fun `placing a distractor is penalized to zero`() {
        val result = engine.score(
            level = level(listOf(distractor)),
            placements = listOf(PlacedObject("keys", 0, 0)),
            placementOrder = listOf("keys"),
            remainingReconstructMs = null,
        )

        val score = result.objectScores.single()
        assertEquals(0, score.points)
    }

    @Test
    fun `wrong slot earns zero position accuracy`() {
        val wrongSlot = PlacedObject("vase", slotIndex = target.slotIndex + 1, rotationDegrees = target.rotationDegrees)

        val result = engine.score(level(listOf(target)), placements = listOf(wrongSlot), placementOrder = listOf("vase"), remainingReconstructMs = null)

        assertEquals(0f, result.objectScores.single().positionAccuracy, 0.001f)
    }

    @Test
    fun `rotation within tolerance still earns full rotation accuracy`() {
        val nearlyRight = PlacedObject("vase", target.slotIndex, rotationDegrees = target.rotationDegrees + 5)

        val result = engine.score(level(listOf(target)), placements = listOf(nearlyRight), placementOrder = listOf("vase"), remainingReconstructMs = null)

        assertEquals(1f, result.objectScores.single().rotationAccuracy, 0.001f)
    }

    @Test
    fun `finishing with more time remaining earns a larger time bonus`() {
        val placements = listOf(PlacedObject("vase", 2, 90))
        val levelWithTimer = level(listOf(target), timeLimitMs = 60_000L)

        val fastFinish = engine.score(levelWithTimer, placements, placementOrder = listOf("vase"), remainingReconstructMs = 50_000L)
        val slowFinish = engine.score(levelWithTimer, placements, placementOrder = listOf("vase"), remainingReconstructMs = 5_000L)

        assertTrue(fastFinish.timeBonus > slowFinish.timeBonus)
        assertTrue(fastFinish.timeBonus <= (ScoringRules.Default.maxTimeBonusPoints * ScoringRules.Default.timeBonusWeight).toInt() + 1)
    }

    @Test
    fun `zen and practice levels with no time limit never award a time bonus`() {
        val result = engine.score(
            level = level(listOf(target), timeLimitMs = null),
            placements = listOf(PlacedObject("vase", 2, 90)),
            placementOrder = listOf("vase"),
            remainingReconstructMs = null,
        )

        assertEquals(0, result.timeBonus)
    }

    @Test
    fun `order mode disabled keeps order accuracy neutral regardless of placement order`() {
        val second = target.copy(objectId = "lamp", slotIndex = 0, rotationDegrees = 0)
        val result = engine.score(
            level = level(listOf(target, second), orderModeEnabled = false),
            placements = listOf(PlacedObject("vase", 2, 90), PlacedObject("lamp", 0, 0)),
            placementOrder = listOf("lamp", "vase"),
            remainingReconstructMs = null,
        )

        assertTrue(result.objectScores.all { it.orderAccuracy == 1f })
    }

    @Test
    fun `order mode enabled penalizes out-of-sequence placement`() {
        val second = target.copy(objectId = "lamp", slotIndex = 0, rotationDegrees = 0)
        val inOrder = engine.score(
            level = level(listOf(target, second), orderModeEnabled = true),
            placements = listOf(PlacedObject("vase", 2, 90), PlacedObject("lamp", 0, 0)),
            placementOrder = listOf("vase", "lamp"),
            remainingReconstructMs = null,
        )
        val outOfOrder = engine.score(
            level = level(listOf(target, second), orderModeEnabled = true),
            placements = listOf(PlacedObject("vase", 2, 90), PlacedObject("lamp", 0, 0)),
            placementOrder = listOf("lamp", "vase"),
            remainingReconstructMs = null,
        )

        assertTrue(inOrder.objectScores.all { it.orderAccuracy == 1f })
        assertTrue(outOfOrder.objectScores.all { it.orderAccuracy == 0f })
        assertTrue(outOfOrder.sceneAccuracy < inOrder.sceneAccuracy)
    }

    @Test
    fun `combo count is the longest streak of consecutive correct placements, in placement order`() {
        val lamp = target.copy(objectId = "lamp", slotIndex = 0, rotationDegrees = 0)
        val clock = target.copy(objectId = "clock", slotIndex = 5, rotationDegrees = 0)

        val result = engine.score(
            level = level(listOf(target, lamp, clock)),
            placements = listOf(
                PlacedObject("vase", 2, 90), // correct
                PlacedObject("clock", 5, 0), // correct -- streak of 2 with vase
                PlacedObject("lamp", 7, 0), // wrong slot -- breaks the streak
            ),
            placementOrder = listOf("vase", "clock", "lamp"),
            remainingReconstructMs = null,
        )

        assertEquals(2, result.comboCount)
    }

    @Test
    fun `combo count is zero when nothing was placed correctly`() {
        val result = engine.score(
            level = level(listOf(target)),
            placements = listOf(PlacedObject("vase", slotIndex = target.slotIndex + 1, rotationDegrees = 0)),
            placementOrder = listOf("vase"),
            remainingReconstructMs = null,
        )

        assertEquals(0, result.comboCount)
    }

    @Test
    fun `a streak at or above the minimum awards combo bonus points folded into final score`() {
        val lamp = target.copy(objectId = "lamp", slotIndex = 0, rotationDegrees = 0)
        val clock = target.copy(objectId = "clock", slotIndex = 5, rotationDegrees = 0)

        val result = engine.score(
            level = level(listOf(target, lamp, clock)),
            placements = listOf(PlacedObject("vase", 2, 90), PlacedObject("lamp", 0, 0), PlacedObject("clock", 5, 0)),
            placementOrder = listOf("vase", "lamp", "clock"),
            remainingReconstructMs = null,
        )

        assertEquals(3, result.comboCount)
        assertEquals(result.comboCount * ScoringRules.Default.comboBonusPerStreakObject, result.comboBonus)
        assertEquals(result.placementScore + result.timeBonus + result.comboBonus, result.finalScore)
    }

    @Test
    fun `a streak below the minimum awards no combo bonus`() {
        val result = engine.score(
            level = level(listOf(target)),
            placements = listOf(PlacedObject("vase", 2, 90)),
            placementOrder = listOf("vase"),
            remainingReconstructMs = null,
        )

        assertEquals(1, result.comboCount)
        assertEquals(0, result.comboBonus)
        assertEquals(result.placementScore, result.finalScore)
    }
}
