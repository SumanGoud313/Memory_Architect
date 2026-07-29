package com.suman.memoryarchitect.domain.progression

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCampaignEngineTest {

    private val engine = LevelCampaignEngine()
    private val rules = LevelCampaignRules.Default

    @Test
    fun `memorize duration gets a per-object bonus the instant rotation unlocks`() {
        val justBelow = engine.constraintsFor(rules.rotationUnlockLevel - 1)
        val atUnlock = engine.constraintsFor(rules.rotationUnlockLevel)

        assertFalse(justBelow.rotationEnabled)
        assertTrue(atUnlock.rotationEnabled)
        // The base curve alone (memorizeSetupMs + objectCount * memorizePerObjectMs) never
        // decreases either, so this alone already guarantees atUnlock >= justBelow even before
        // the rotation bonus - the real assertion is that the bonus adds something on top.
        assertTrue(
            "expected a real bump at the rotation debut level, was ${justBelow.memorizeDurationMs} -> ${atUnlock.memorizeDurationMs}",
            atUnlock.memorizeDurationMs > justBelow.memorizeDurationMs,
        )
    }

    @Test
    fun `memorize duration gets a second per-object bonus the instant order mode unlocks`() {
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
    fun `both per-object bonuses stack once past both unlock levels`() {
        val level = maxOf(rules.rotationUnlockLevel, rules.orderUnlockLevel) + 1
        val constraints = engine.constraintsFor(level)
        val progress = (level - 1).toFloat() / (rules.maxLevel - 1)
        val objectCount = (rules.minObjectCount + progress * (rules.maxObjectCount - rules.minObjectCount))
            .roundToInt()
            .coerceIn(rules.minObjectCount, rules.maxObjectCount)
        val baseDuration = rules.memorizeSetupMs + objectCount * rules.memorizePerObjectMs

        assertTrue(constraints.rotationEnabled)
        assertTrue(constraints.orderModeEnabled)
        val expected = baseDuration + objectCount * rules.rotationMemorizePerObjectMs + objectCount * rules.orderModeMemorizePerObjectMs
        assertEquals(expected, constraints.memorizeDurationMs)
    }

    @Test
    fun `object count and time limit are unaffected by the memorize-time bonus`() {
        val level = rules.orderUnlockLevel
        val withoutRotationOrOrder = LevelCampaignEngine(rules.copy(rotationMemorizePerObjectMs = 0L, orderModeMemorizePerObjectMs = 0L)).constraintsFor(level)
        val withBonuses = engine.constraintsFor(level)

        assertEquals(withoutRotationOrOrder.objectCount, withBonuses.objectCount)
        assertEquals(withoutRotationOrOrder.timeLimitMs, withBonuses.timeLimitMs)
        assertEquals(withoutRotationOrOrder.distractorRatio, withBonuses.distractorRatio)
    }

    @Test
    fun `memorize duration never decreases as levels get harder`() {
        var previous = engine.constraintsFor(1).memorizeDurationMs
        for (level in 2..rules.maxLevel) {
            val current = engine.constraintsFor(level).memorizeDurationMs
            assertTrue(
                "memorize duration regressed at level $level: $previous -> $current",
                current >= previous,
            )
            previous = current
        }
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
