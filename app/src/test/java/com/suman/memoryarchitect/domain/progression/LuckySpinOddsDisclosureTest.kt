package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LuckySpinOddsDisclosureTest {

    @Test
    fun `every entry's probability sums to 1_0`() {
        val total = LuckySpinOddsDisclosure.compute().sumOf { it.probability }
        assertTrue("total was $total", abs(total - 1.0) < 0.0001)
    }

    @Test
    fun `blended odds match the default rules' hand-computed values`() {
        val entries = LuckySpinOddsDisclosure.compute(SpinRules.Default)
        val coinsByAmount = entries.filter { it.coinsAwarded != null }.associate { it.coinsAwarded to it.probability }
        val cosmeticByRarity = entries.filter { it.rarity != null }.associate { it.rarity to it.probability }

        // 1 lucky day in 3: lucky-day odds are used as-is (0.35/0.30/0.10 coins, 0.25 cosmetic);
        // the other 2 days in 3 re-normalize coinOutcomes (which sum to 0.75) to fill 1.0.
        assertEquals(1.0 / 3 * 0.35 + 2.0 / 3 * (0.35 / 0.75), coinsByAmount.getValue(150L), 0.0001)
        assertEquals(1.0 / 3 * 0.30 + 2.0 / 3 * (0.30 / 0.75), coinsByAmount.getValue(250L), 0.0001)
        assertEquals(1.0 / 3 * 0.10 + 2.0 / 3 * (0.10 / 0.75), coinsByAmount.getValue(500L), 0.0001)
        assertEquals(1.0 / 3 * 0.25 * 0.60, cosmeticByRarity.getValue(CosmeticRarity.COMMON), 0.0001)
        assertEquals(1.0 / 3 * 0.25 * 0.28, cosmeticByRarity.getValue(CosmeticRarity.RARE), 0.0001)
        assertEquals(1.0 / 3 * 0.25 * 0.10, cosmeticByRarity.getValue(CosmeticRarity.EPIC), 0.0001)
        assertEquals(1.0 / 3 * 0.25 * 0.02, cosmeticByRarity.getValue(CosmeticRarity.LEGENDARY), 0.0001)
    }
}
