package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCampaignEngineTest {

    private val engine = LevelCampaignEngine()
    private val rules = LevelCampaignRules.Default

    @Test
    fun `memorize duration gets a flat bonus the instant rotation unlocks`() {
        val justBelow = engine.constraintsFor(rules.rotationUnlockLevel - 1)
        val atUnlock = engine.constraintsFor(rules.rotationUnlockLevel)

        assertFalse(justBelow.rotationEnabled)
        assertTrue(atUnlock.rotationEnabled)
        // The base curve alone is monotonically decreasing, so without the bonus atUnlock's
        // duration would be lower than justBelow's - the rotation bonus must more than cover that
        // natural decrease, landing strictly above the pre-rotation level's own duration.
        assertTrue(
            "expected a real bump at the rotation debut level, was ${justBelow.memorizeDurationMs} -> ${atUnlock.memorizeDurationMs}",
            atUnlock.memorizeDurationMs > justBelow.memorizeDurationMs,
        )
    }

    @Test
    fun `memorize duration gets a second flat bonus the instant order mode unlocks`() {
        val justBelow = engine.constraintsFor(rules.orderUnlockLevel - 1)
        val atUnlock = engine.constraintsFor(rules.orderUnlockLevel)

        assertFalse(justBelow.orderModeEnabled)
        assertTrue(atUnlock.orderModeEnabled)
        assertTrue(
            "expected a real bump at the order-mode debut level, was ${justBelow.memorizeDurationMs} -> ${atUnlock.memorizeDurationMs}",
            atUnlock.memorizeDurationMs > justBelow.memorizeDurationMs,
        )
    }

    @Test
    fun `both bonuses stack once past both unlock levels`() {
        val level = maxOf(rules.rotationUnlockLevel, rules.orderUnlockLevel) + 1
        val constraints = engine.constraintsFor(level)
        val progress = (level - 1).toFloat() / (rules.maxLevel - 1)
        val baseDuration = (rules.maxMemorizeDurationMs - progress * (rules.maxMemorizeDurationMs - rules.minMemorizeDurationMs)).let(Math::round)

        assertTrue(constraints.rotationEnabled)
        assertTrue(constraints.orderModeEnabled)
        val expected = baseDuration + rules.rotationMemorizeBonusMs + rules.orderModeMemorizeBonusMs
        assertEquals(expected, constraints.memorizeDurationMs)
    }

    @Test
    fun `object count and time limit are unaffected by the memorize-time bonus`() {
        val level = rules.orderUnlockLevel
        val withoutRotationOrOrder = LevelCampaignEngine(rules.copy(rotationMemorizeBonusMs = 0L, orderModeMemorizeBonusMs = 0L)).constraintsFor(level)
        val withBonuses = engine.constraintsFor(level)

        assertEquals(withoutRotationOrOrder.objectCount, withBonuses.objectCount)
        assertEquals(withoutRotationOrOrder.timeLimitMs, withBonuses.timeLimitMs)
        assertEquals(withoutRotationOrOrder.distractorRatio, withBonuses.distractorRatio)
    }

    @Test
    fun `isRotationDebutLevel is true only on the exact unlock level`() {
        assertFalse(engine.isRotationDebutLevel(rules.rotationUnlockLevel - 1))
        assertTrue(engine.isRotationDebutLevel(rules.rotationUnlockLevel))
        assertFalse(engine.isRotationDebutLevel(rules.rotationUnlockLevel + 1))
    }

    @Test
    fun `isOrderModeDebutLevel is true only on the exact unlock level`() {
        assertFalse(engine.isOrderModeDebutLevel(rules.orderUnlockLevel - 1))
        assertTrue(engine.isOrderModeDebutLevel(rules.orderUnlockLevel))
        assertFalse(engine.isOrderModeDebutLevel(rules.orderUnlockLevel + 1))
    }
}
