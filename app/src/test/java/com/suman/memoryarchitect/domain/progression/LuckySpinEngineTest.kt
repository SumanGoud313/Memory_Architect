package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LuckySpinEngineTest {

    @Test
    fun `rollRarity distribution converges to configured odds over many trials`() {
        val engine = LuckySpinEngine(random = Random(seed = 42))
        val trials = 50_000
        val counts = mutableMapOf<CosmeticRarity, Int>()
        repeat(trials) {
            val rarity = engine.rollRarity()
            counts[rarity] = (counts[rarity] ?: 0) + 1
        }

        SpinRules.Default.oddsByRarity.forEach { (rarity, expectedOdds) ->
            val actualFraction = (counts[rarity] ?: 0).toDouble() / trials
            assertTrue(
                "$rarity expected ~$expectedOdds, got $actualFraction",
                kotlin.math.abs(actualFraction - expectedOdds) < 0.02,
            )
        }
    }

    @Test
    fun `pickItem only returns spin-eligible items of the requested rarity`() {
        val engine = LuckySpinEngine(random = Random(seed = 7))
        CosmeticRarity.entries.forEach { rarity ->
            repeat(50) {
                val id = engine.pickItem(rarity)
                val definition = ShopCatalog.requireDefinition(id)
                assertEquals(rarity, definition.rarity)
                assertTrue(definition.spinEligible)
            }
        }
    }

    @Test
    fun `spin returns a roll whose id matches its own rarity`() {
        val engine = LuckySpinEngine(random = Random(seed = 99))
        repeat(100) {
            val roll = engine.spin()
            assertEquals(roll.rarity, ShopCatalog.requireDefinition(roll.id).rarity)
        }
    }

    @Test
    fun `duplicate refund math is exact`() {
        val price = 1000L
        val expectedRefund = (price * SpinRules.Default.duplicateRefundFraction).toLong()

        assertEquals(500L, expectedRefund)
    }

    @Test
    fun `odds sum to one`() {
        val total = SpinRules.Default.oddsByRarity.values.sum()

        assertTrue(kotlin.math.abs(total - 1.0) < 0.0001)
    }
}
