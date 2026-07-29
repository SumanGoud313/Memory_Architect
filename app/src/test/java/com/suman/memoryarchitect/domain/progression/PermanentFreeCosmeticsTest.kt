package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PermanentFreeCosmeticsTest {

    @Test
    fun `ids is exactly Twilight Haze background and Luxury Onyx border`() {
        assertEquals(setOf(CosmeticId.BACKGROUND_TWILIGHT_HAZE, CosmeticId.BORDER_LUXURY_ONYX), PermanentFreeCosmetics.ids)
    }

    @Test
    fun `defaultEquippedByCategory maps BACKGROUND_THEME and PROFILE_BORDER to the permanent free ids`() {
        assertEquals(CosmeticId.BACKGROUND_TWILIGHT_HAZE, PermanentFreeCosmetics.defaultEquippedByCategory[CosmeticCategory.BACKGROUND_THEME])
        assertEquals(CosmeticId.BORDER_LUXURY_ONYX, PermanentFreeCosmetics.defaultEquippedByCategory[CosmeticCategory.PROFILE_BORDER])
        assertEquals(2, PermanentFreeCosmetics.defaultEquippedByCategory.size)
    }

    @Test
    fun `neither permanent free cosmetic is spin-eligible - both are already free, never a meaningful spin prize`() {
        PermanentFreeCosmetics.ids.forEach { id ->
            assertFalse("$id should not be spinEligible", AllCosmeticsCatalog.requireDefinition(id).spinEligible)
        }
    }

    @Test
    fun `every permanent free id resolves to the category defaultEquippedByCategory claims for it`() {
        PermanentFreeCosmetics.defaultEquippedByCategory.forEach { (category, id) ->
            assertEquals(category, AllCosmeticsCatalog.requireDefinition(id).category)
        }
    }
}
