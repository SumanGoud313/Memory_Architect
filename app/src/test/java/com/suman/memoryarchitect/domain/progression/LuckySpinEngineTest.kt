package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.SpinRewardKind
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
    fun `rollCosmetic returns a roll whose id matches its own rarity`() {
        val engine = LuckySpinEngine(random = Random(seed = 99))
        repeat(100) {
            val roll = engine.rollCosmetic()
            assertEquals(roll.rarity, ShopCatalog.requireDefinition(roll.id).rarity)
        }
    }

    @Test
    fun `spin with guaranteeCosmetic always resolves to Cosmetic regardless of the day`() {
        val engine = LuckySpinEngine(random = Random(seed = 3))
        val luckyDay = findDay(engine, lucky = true)
        val nonLuckyDay = findDay(engine, lucky = false)
        repeat(100) {
            assertTrue(engine.spin(guaranteeCosmetic = true, todayEpochDay = luckyDay) is SpinRewardKind.Cosmetic)
            assertTrue(engine.spin(guaranteeCosmetic = true, todayEpochDay = nonLuckyDay) is SpinRewardKind.Cosmetic)
        }
    }

    @Test
    fun `spin without guaranteeCosmetic always resolves to a table entry`() {
        val engine = LuckySpinEngine(random = Random(seed = 5))
        val coinAmounts = SpinRules.Default.coinOutcomes.map { it.first }.toSet()
        repeat(1_000) {
            when (val reward = engine.spin(todayEpochDay = it.toLong())) {
                is SpinRewardKind.Coins -> assertTrue("${reward.amount} not a configured coin outcome", reward.amount in coinAmounts)
                is SpinRewardKind.Cosmetic -> assertEquals(reward.rarity, ShopCatalog.requireDefinition(reward.id).rarity)
            }
        }
    }

    @Test
    fun `isLuckyCosmeticDay averages out to roughly one day in luckyDayIntervalDays over many days`() {
        val engine = LuckySpinEngine()
        val days = 30_000L
        val luckyCount = (0 until days).count { engine.isLuckyCosmeticDay(it) }
        val expectedFraction = 1.0 / SpinRules.Default.luckyDayIntervalDays
        val actualFraction = luckyCount.toDouble() / days
        assertTrue(
            "expected ~$expectedFraction of days lucky, got $actualFraction",
            kotlin.math.abs(actualFraction - expectedFraction) < 0.02,
        )
    }

    @Test
    fun `isLuckyCosmeticDay is deterministic and doesn't fall into an obvious fixed-interval pattern`() {
        val engine = LuckySpinEngine()
        // Same day always agrees with itself (stateless, pure function of the calendar day).
        assertEquals(engine.isLuckyCosmeticDay(500L), engine.isLuckyCosmeticDay(500L))
        // A naive `day % 3 == 0` gate would make every multiple-of-3 day lucky and nothing else -
        // the whole reason fmix32 avalanches todayEpochDay first is so consecutive lucky days
        // don't line up on a rigid, guessable 3-day calendar cadence.
        val luckyMultiplesOf3 = (0 until 300L step 3).count { engine.isLuckyCosmeticDay(it) }
        assertTrue("expected some non-multiple-of-3 days to be lucky too", luckyMultiplesOf3 < 100)
    }

    @Test
    fun `spin on a lucky day converges to configured coin vs cosmetic odds over many trials`() {
        val engine = LuckySpinEngine(random = Random(seed = 42))
        val luckyDay = findDay(engine, lucky = true)
        val trials = 50_000
        var coinsCount = 0
        var cosmeticCount = 0
        val perAmountCount = mutableMapOf<Long, Int>()
        repeat(trials) {
            when (val reward = engine.spin(todayEpochDay = luckyDay)) {
                is SpinRewardKind.Coins -> {
                    coinsCount++
                    perAmountCount[reward.amount] = (perAmountCount[reward.amount] ?: 0) + 1
                }
                is SpinRewardKind.Cosmetic -> cosmeticCount++
            }
        }

        val expectedCosmeticOdds = SpinRules.Default.cosmeticOdds
        val actualCosmeticFraction = cosmeticCount.toDouble() / trials
        assertTrue(
            "expected cosmetic odds ~$expectedCosmeticOdds, got $actualCosmeticFraction",
            kotlin.math.abs(actualCosmeticFraction - expectedCosmeticOdds) < 0.02,
        )
        SpinRules.Default.coinOutcomes.forEach { (amount, expectedOdds) ->
            val actualFraction = (perAmountCount[amount] ?: 0).toDouble() / trials
            assertTrue(
                "$amount expected ~$expectedOdds, got $actualFraction",
                kotlin.math.abs(actualFraction - expectedOdds) < 0.02,
            )
        }
    }

    @Test
    fun `spin on a non-lucky day never resolves to Cosmetic and matches coinOutcomes' own re-normalized weights`() {
        val engine = LuckySpinEngine(random = Random(seed = 42))
        val nonLuckyDay = findDay(engine, lucky = false)
        val trials = 50_000
        val perAmountCount = mutableMapOf<Long, Int>()
        repeat(trials) {
            when (val reward = engine.spin(todayEpochDay = nonLuckyDay)) {
                is SpinRewardKind.Coins -> perAmountCount[reward.amount] = (perAmountCount[reward.amount] ?: 0) + 1
                is SpinRewardKind.Cosmetic -> throw AssertionError("non-lucky day resolved to a Cosmetic")
            }
        }

        val totalOdds = SpinRules.Default.coinOutcomes.sumOf { it.second }
        SpinRules.Default.coinOutcomes.forEach { (amount, odds) ->
            val expectedFraction = odds / totalOdds
            val actualFraction = (perAmountCount[amount] ?: 0).toDouble() / trials
            assertTrue(
                "$amount expected ~$expectedFraction, got $actualFraction",
                kotlin.math.abs(actualFraction - expectedFraction) < 0.02,
            )
        }
    }

    private fun findDay(engine: LuckySpinEngine, lucky: Boolean): Long =
        (0 until 1000L).first { engine.isLuckyCosmeticDay(it) == lucky }

    @Test
    fun `duplicate refund math is exact`() {
        val price = 1000L
        val expectedRefund = (price * SpinRules.Default.duplicateRefundFraction).toLong()

        assertEquals(500L, expectedRefund)
    }

    @Test
    fun `oddsByRarity sums to one`() {
        val total = SpinRules.Default.oddsByRarity.values.sum()

        assertTrue(kotlin.math.abs(total - 1.0) < 0.0001)
    }

    @Test
    fun `coinOutcomes odds plus cosmeticOdds sum to one`() {
        val total = SpinRules.Default.coinOutcomes.sumOf { it.second } + SpinRules.Default.cosmeticOdds

        assertTrue(kotlin.math.abs(total - 1.0) < 0.0001)
    }

    @Test
    fun `every coin outcome stays under the server's per-write coin-gain cap`() {
        // Mirrors functions/src/index.ts's MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE (2,000) - see
        // MysteryChestOdds's doc for why a client-rolled coins-only outcome needs no dedicated
        // Cloud Function re-derivation as long as every entry stays comfortably under that bound.
        SpinRules.Default.coinOutcomes.forEach { (amount, _) ->
            assertTrue("$amount exceeds the server's per-write coin-gain cap", amount < 2_000L)
        }
    }
}
