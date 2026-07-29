package com.suman.memoryarchitect.ui.theme

import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.domain.model.CosmeticId

/** A premium [com.suman.memoryarchitect.domain.model.CosmeticCategory.ROOM_SKIN]'s gameplay
 * recolor - a translucent wash over whichever of the 8 existing rooms a level's generator picked
 * (see `RoomSkinOverlay.kt`), never a redraw of the room itself. [wallTint]/[floorTint] drive the
 * wash's gradient and the object grounding-shadow tint; [accentGlow] replaces the hardcoded gold
 * used by every existing gameplay interaction cue (snap-target ring, placement confirmation
 * pulse, hint-reveal glow, drag-ghost glow, tray pickup glow); [particleColors] replaces the
 * default confetti palette for a successful drop/submit while this skin is equipped. */
data class RoomSkinVisualSpec(
    val wallTint: Color,
    val floorTint: Color,
    val accentGlow: Color,
    val particleColors: List<Color>,
)

object RoomSkinVisualCatalog {
    private val specs: Map<CosmeticId, RoomSkinVisualSpec> = mapOf(
        CosmeticId.ROOM_FOUNDER_HERITAGE to RoomSkinVisualSpec(
            wallTint = MemoryArchitectColors.accentTerracottaDark,
            floorTint = MemoryArchitectColors.accentAmber,
            accentGlow = MemoryArchitectColors.accentGold,
            particleColors = listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentTerracotta, Color(0xFFFFF3D6)),
        ),
        CosmeticId.ROOM_STARTER_DAWN to RoomSkinVisualSpec(
            wallTint = Color(0xFFF4C77A),
            floorTint = MemoryArchitectColors.accentSage,
            accentGlow = MemoryArchitectColors.accentAmber,
            particleColors = listOf(MemoryArchitectColors.accentAmber, Color(0xFFF4C77A), MemoryArchitectColors.accentSage),
        ),
        // wallTint/floorTint below are brightened just enough from their original near-black/
        // over-bright picks to sit inside ContrastValidation.SAFE_TINT_LUMINANCE_RANGE (see
        // CosmeticContrastTest) - BlendMode.Overlay pulls hard toward whatever's outside that band,
        // crushing gameplay toward black or blowing it toward white regardless of what's under it.
        CosmeticId.ROOM_ROYAL_PALACE to RoomSkinVisualSpec(
            wallTint = Color(0xFF6B3F94),
            floorTint = Color(0xFF6A3FA0),
            accentGlow = Color(0xFFD4AF37),
            particleColors = listOf(Color(0xFFD4AF37), Color(0xFF6A3FA0), Color(0xFFE07FA0)),
        ),
        CosmeticId.ROOM_CYBER_GRIDWORKS to RoomSkinVisualSpec(
            wallTint = Color(0xFF394476),
            floorTint = Color(0xFF00B8C4),
            accentGlow = Color(0xFFFF2DD1),
            particleColors = listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1), Color(0xFFAEE9E8)),
        ),
        CosmeticId.ROOM_SPACE_NEBULA_DOCK to RoomSkinVisualSpec(
            wallTint = Color(0xFF3A4989),
            floorTint = Color(0xFF6A3FA0),
            accentGlow = Color(0xFFAEE9E8),
            particleColors = listOf(Color(0xFFAEE9E8), Color(0xFF6A3FA0), Color(0xFFE07FA0)),
        ),
        CosmeticId.ROOM_NATURE_GROVE to RoomSkinVisualSpec(
            wallTint = Color(0xFF2E5339),
            floorTint = Color(0xFF8FBF6F),
            accentGlow = MemoryArchitectColors.accentSage,
            particleColors = listOf(Color(0xFF8FBF6F), MemoryArchitectColors.accentSage, Color(0xFFE07FA0)),
        ),
        CosmeticId.ROOM_LUXURY_SUITE to RoomSkinVisualSpec(
            wallTint = Color(0xFF4D4335),
            floorTint = MemoryArchitectColors.accentGold,
            accentGlow = MemoryArchitectColors.accentGold,
            particleColors = listOf(MemoryArchitectColors.accentGold, Color(0xFFE8F7FF), MemoryArchitectColors.accentGoldDark),
        ),
    )

    fun get(id: CosmeticId): RoomSkinVisualSpec = specs.getValue(id)
}
