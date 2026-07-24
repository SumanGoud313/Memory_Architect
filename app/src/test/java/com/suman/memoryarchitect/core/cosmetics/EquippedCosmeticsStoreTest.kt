package com.suman.memoryarchitect.core.cosmetics

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import org.junit.Assert.assertEquals
import org.junit.Test

class EquippedCosmeticsStoreTest {

    @Test
    fun `setAll replaces the whole map`() {
        val store = EquippedCosmeticsStore()
        store.setAll(mapOf(CosmeticCategory.PROFILE_BORDER to CosmeticId.BORDER_ROYAL_GOLD))

        assertEquals(mapOf(CosmeticCategory.PROFILE_BORDER to CosmeticId.BORDER_ROYAL_GOLD), store.equipped.value)
    }

    @Test
    fun `setEquipped with a non-null id sets that category`() {
        val store = EquippedCosmeticsStore()
        store.setEquipped(CosmeticCategory.PROFILE_BORDER, CosmeticId.BORDER_GALAXY)

        assertEquals(CosmeticId.BORDER_GALAXY, store.equipped.value[CosmeticCategory.PROFILE_BORDER])
    }

    @Test
    fun `setEquipped with a null id removes that category`() {
        val store = EquippedCosmeticsStore()
        store.setEquipped(CosmeticCategory.PROFILE_BORDER, CosmeticId.BORDER_GALAXY)

        store.setEquipped(CosmeticCategory.PROFILE_BORDER, null)

        assertEquals(null, store.equipped.value[CosmeticCategory.PROFILE_BORDER])
        assertEquals(false, store.equipped.value.containsKey(CosmeticCategory.PROFILE_BORDER))
    }

    @Test
    fun `setEquipped leaves other categories untouched`() {
        val store = EquippedCosmeticsStore()
        store.setEquipped(CosmeticCategory.PROFILE_BORDER, CosmeticId.BORDER_EMERALD)
        store.setEquipped(CosmeticCategory.NAME_COLOR, CosmeticId.NAME_COLOR_SLATE)

        store.setEquipped(CosmeticCategory.PROFILE_BORDER, null)

        assertEquals(CosmeticId.NAME_COLOR_SLATE, store.equipped.value[CosmeticCategory.NAME_COLOR])
    }
}
