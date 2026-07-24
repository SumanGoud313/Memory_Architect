package com.suman.memoryarchitect.domain.hint

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.HintReveal
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintEngineTest {

    private val engine = HintEngine()

    private val level = LevelSpec(
        seed = 1L,
        mode = GameMode.PRACTICE,
        difficultyTier = DifficultyTier.EASY,
        sceneType = "kitchen",
        objects = listOf(
            SceneObjectSpec(objectId = "target-a", slotIndex = 2, rotationDegrees = 0, scale = 1f, isDistractor = false),
            SceneObjectSpec(objectId = "target-b", slotIndex = 5, rotationDegrees = 0, scale = 1f, isDistractor = false),
            SceneObjectSpec(objectId = "decoy", slotIndex = 7, rotationDegrees = 0, scale = 1f, isDistractor = true),
        ),
        timeLimitMs = null,
        memorizeDurationMs = 5000L,
        orderModeEnabled = false,
    )

    @Test
    fun `revealFor a target object returns its exact correct slot`() {
        assertEquals(HintReveal(objectId = "target-a", slotIndex = 2, rotationDegrees = 0), engine.revealFor("target-a", level))
    }

    @Test
    fun `revealFor a different target object returns that object's own slot`() {
        assertEquals(HintReveal(objectId = "target-b", slotIndex = 5, rotationDegrees = 0), engine.revealFor("target-b", level))
    }

    @Test
    fun `revealFor a distractor returns null - there is no correct slot to reveal`() {
        assertNull(engine.revealFor("decoy", level))
    }

    @Test
    fun `revealFor an unknown object id returns null`() {
        assertNull(engine.revealFor("not-in-this-level", level))
    }

    @Test
    fun `maxHintsForLevel delegates to the injected rules`() {
        assertEquals(1, engine.maxHintsForLevel(1))
        assertEquals(3, engine.maxHintsForLevel(100))
    }
}
