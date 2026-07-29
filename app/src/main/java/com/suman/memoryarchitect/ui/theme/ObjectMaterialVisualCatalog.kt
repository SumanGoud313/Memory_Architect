package com.suman.memoryarchitect.ui.theme

import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.domain.model.CosmeticId

/** A premium [com.suman.memoryarchitect.domain.model.CosmeticCategory.OBJECT_MATERIAL]'s gameplay
 * recolor - a paint-transform applied over every object's existing Canvas art (see
 * `GameplayScenePanel.kt`'s `objectMaterialTint`, the same `saveLayer`+`ColorFilter` technique the
 * pre-existing `distractorDesaturation` already establishes), applied uniformly regardless of
 * which of the 8 rooms/103 objects a level's generator picked that round - never a redraw of any
 * object's geometry. Which pickup/rotate/place sound family plays while this material is equipped
 * is a separate, audio-only lookup - see `domain/progression/ObjectMaterialSfxCatalog.kt` (kept
 * out of this Color-bearing UI-layer spec so `core/feedback` never needs to import `ui.theme`). */
data class ObjectMaterialVisualSpec(
    val tintColor: Color,
    val blendStrength: Float = 0.35f,
    val highlightColor: Color,
)

object ObjectMaterialVisualCatalog {
    private val specs: Map<CosmeticId, ObjectMaterialVisualSpec> = mapOf(
        CosmeticId.MATERIAL_FOUNDER_BRASS to ObjectMaterialVisualSpec(
            tintColor = MemoryArchitectColors.accentGold,
            highlightColor = Color(0xFFFFF3D6),
        ),
        CosmeticId.MATERIAL_STARTER_CANVAS to ObjectMaterialVisualSpec(
            tintColor = MemoryArchitectColors.accentAmber,
            highlightColor = Color(0xFFF4C77A),
        ),
        CosmeticId.MATERIAL_ROYAL_GILDED_MARBLE to ObjectMaterialVisualSpec(
            tintColor = Color(0xFFD4AF37),
            highlightColor = Color(0xFFFFF3D6),
        ),
        CosmeticId.MATERIAL_CYBER_CHROME_CIRCUIT to ObjectMaterialVisualSpec(
            tintColor = Color(0xFFC7CBD1),
            highlightColor = Color(0xFF00F0FF),
        ),
        CosmeticId.MATERIAL_SPACE_GUNMETAL to ObjectMaterialVisualSpec(
            tintColor = Color(0xFF4A5568),
            highlightColor = Color(0xFFAEE9E8),
        ),
        CosmeticId.MATERIAL_NATURE_MOSS_WOOD to ObjectMaterialVisualSpec(
            tintColor = Color(0xFF6B4A2E),
            highlightColor = Color(0xFF8FBF6F),
        ),
        // tintColor brightened from the original near-black pick to sit inside
        // ContrastValidation.SAFE_TINT_LUMINANCE_RANGE (see CosmeticContrastTest) - the original
        // value crushed objects toward pure black via BlendMode.Overlay's 0.35 blendStrength,
        // making them hard to identify - exactly the "some materials appear too dark" report.
        CosmeticId.MATERIAL_LUXURY_SMOKED_GLASS to ObjectMaterialVisualSpec(
            tintColor = Color(0xFF4D4335),
            highlightColor = Color(0xFFE8F7FF),
        ),
    )

    fun get(id: CosmeticId): ObjectMaterialVisualSpec = specs.getValue(id)
}
