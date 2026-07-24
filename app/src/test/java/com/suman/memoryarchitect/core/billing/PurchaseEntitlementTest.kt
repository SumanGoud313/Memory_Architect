package com.suman.memoryarchitect.core.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseEntitlementTest {

    private val productId = "remove_ads_lifetime"

    @Test
    fun `purchased state for the target product grants entitlement`() {
        assertTrue(isGrantedEntitlement(listOf(productId), BILLING_PURCHASE_STATE_PURCHASED, productId))
    }

    @Test
    fun `pending state never grants entitlement`() {
        val pending = 2 // Purchase.PurchaseState.PENDING
        assertFalse(isGrantedEntitlement(listOf(productId), pending, productId))
    }

    @Test
    fun `unspecified state never grants entitlement`() {
        val unspecified = 0 // Purchase.PurchaseState.UNSPECIFIED_STATE
        assertFalse(isGrantedEntitlement(listOf(productId), unspecified, productId))
    }

    @Test
    fun `a purchased record for a different product does not grant this entitlement`() {
        assertFalse(isGrantedEntitlement(listOf("some_other_sku"), BILLING_PURCHASE_STATE_PURCHASED, productId))
    }

    @Test
    fun `multi-product purchase record grants entitlement if the target is among them`() {
        assertTrue(isGrantedEntitlement(listOf("some_other_sku", productId), BILLING_PURCHASE_STATE_PURCHASED, productId))
    }

    @Test
    fun `empty product list never grants entitlement`() {
        assertFalse(isGrantedEntitlement(emptyList(), BILLING_PURCHASE_STATE_PURCHASED, productId))
    }
}
