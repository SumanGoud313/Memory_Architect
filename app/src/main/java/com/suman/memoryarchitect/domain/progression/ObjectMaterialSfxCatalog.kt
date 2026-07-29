package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.SfxMaterialFamily

/**
 * Which pickup/rotate/place sound family plays for each premium `OBJECT_MATERIAL` cosmetic - the
 * audio-only counterpart to [com.suman.memoryarchitect.ui.theme.ObjectMaterialVisualCatalog]
 * (which owns the Color-bearing render spec). Kept as a separate, plain domain-layer object rather
 * than one field on that UI-layer spec so `core/feedback/FeedbackManagerImpl.kt` can resolve a
 * sound family without ever importing `ui.theme` - this app's `core/` package never imports from
 * `ui/` anywhere else, and this catalog preserves that boundary.
 */
object ObjectMaterialSfxCatalog {
    private val families: Map<CosmeticId, SfxMaterialFamily> = mapOf(
        CosmeticId.MATERIAL_FOUNDER_BRASS to SfxMaterialFamily.METALLIC,
        CosmeticId.MATERIAL_STARTER_CANVAS to SfxMaterialFamily.ORGANIC,
        CosmeticId.MATERIAL_ROYAL_GILDED_MARBLE to SfxMaterialFamily.METALLIC,
        CosmeticId.MATERIAL_CYBER_CHROME_CIRCUIT to SfxMaterialFamily.METALLIC,
        CosmeticId.MATERIAL_SPACE_GUNMETAL to SfxMaterialFamily.CRYSTALLINE,
        CosmeticId.MATERIAL_NATURE_MOSS_WOOD to SfxMaterialFamily.ORGANIC,
        CosmeticId.MATERIAL_LUXURY_SMOKED_GLASS to SfxMaterialFamily.METALLIC,
    )

    fun get(id: CosmeticId): SfxMaterialFamily? = families[id]
}
