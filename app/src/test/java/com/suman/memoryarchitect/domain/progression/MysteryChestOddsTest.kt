package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MysteryChestOddsTest {

    @Test
    fun `reward table odds sum to one`() {
        val total = MysteryChestOdds.rewardTable.sumOf { it.second }

        assertTrue(kotlin.math.abs(total - 1.0) < 0.0001)
    }

    @Test
    fun `every reward in the table stays under the server's per-write coin-gain cap`() {
        // Mirrors functions/src/index.ts's MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE (2,000) - see
        // MysteryChestOdds's own doc for why this table needs no dedicated Cloud Function
        // re-derivation as long as every entry stays comfortably under that bound.
        MysteryChestOdds.rewardTable.forEach { (reward, _) ->
            assertTrue("${reward.coinsAwarded} exceeds the server's per-write coin-gain cap", reward.coinsAwarded < 2_000L)
        }
    }

    @Test
    fun `roll always returns a table entry`() {
        val random = Random(seed = 11)
        val validRewards = MysteryChestOdds.rewardTable.map { it.first }.toSet()
        repeat(1_000) {
            assertTrue(MysteryChestOdds.roll(random) in validRewards)
        }
    }

    @Test
    fun `roll distribution converges to configured odds over many trials`() {
        val random = Random(seed = 42)
        val trials = 50_000
        val counts = mutableMapOf<MysteryChestReward, Int>()
        repeat(trials) {
            val reward = MysteryChestOdds.roll(random)
            counts[reward] = (counts[reward] ?: 0) + 1
        }

        MysteryChestOdds.rewardTable.forEach { (reward, expectedOdds) ->
            val actualFraction = (counts[reward] ?: 0).toDouble() / trials
            assertTrue(
                "$reward expected ~$expectedOdds, got $actualFraction",
                kotlin.math.abs(actualFraction - expectedOdds) < 0.02,
            )
        }
    }
}
