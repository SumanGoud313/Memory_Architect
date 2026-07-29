package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId

/**
 * Cosmetics every player owns unconditionally, free, forever - no purchase, no Premium unlock, no
 * per-player grant or Firestore migration needed for existing profiles either, since ownership is
 * computed by unioning this set in at read time (see
 * [com.suman.memoryarchitect.data.repository.ShopRepositoryImpl.getOwnedCosmeticIds]) rather than
 * ever being persisted per-player. [defaultEquippedByCategory] is what
 * [com.suman.memoryarchitect.data.repository.ShopRepositoryImpl.getEquippedCosmetics] falls back to
 * for a category with nothing explicitly equipped yet - a player can still equip something else
 * they own instead (this is a default, not a lock), but there's always *something* shown for these
 * two categories rather than a bare/unstyled look.
 *
 * [CosmeticId.BACKGROUND_TWILIGHT_HAZE] is marked `spinEligible = false` in [ShopCatalog] and
 * [CosmeticId.BORDER_LUXURY_ONYX] already was in [PremiumCatalog] (a Premium-only item) - neither
 * is a meaningful Lucky Spin/Shop-purchase prize any more now that every player already has it.
 */
object PermanentFreeCosmetics {
    val ids: Set<CosmeticId> = setOf(
        CosmeticId.BACKGROUND_TWILIGHT_HAZE,
        CosmeticId.BORDER_LUXURY_ONYX,
    )

    val defaultEquippedByCategory: Map<CosmeticCategory, CosmeticId> = mapOf(
        CosmeticCategory.BACKGROUND_THEME to CosmeticId.BACKGROUND_TWILIGHT_HAZE,
        CosmeticCategory.PROFILE_BORDER to CosmeticId.BORDER_LUXURY_ONYX,
    )
}
