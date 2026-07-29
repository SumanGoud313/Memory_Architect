package com.suman.memoryarchitect.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscountCouponRulesTest {

    @Test
    fun `discountedPrice applies the configured fraction off`() {
        val rules = DiscountCouponRules(discountFraction = 0.25)

        assertEquals(750L, rules.discountedPrice(1000L))
    }

    @Test
    fun `discountedPrice never goes negative for any catalog price`() {
        val rules = DiscountCouponRules.Default

        assertEquals(0L, rules.discountedPrice(0L))
        assertTrue(rules.discountedPrice(1L) >= 0L)
    }

    @Test
    fun `discountedPrice is always less than or equal to the catalog price`() {
        val rules = DiscountCouponRules.Default

        listOf(0L, 1L, 150L, 1_000L, 3_500L).forEach { price ->
            assertTrue(rules.discountedPrice(price) <= price)
        }
    }
}
