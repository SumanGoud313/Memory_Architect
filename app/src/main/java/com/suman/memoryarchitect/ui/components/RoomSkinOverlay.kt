package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import com.suman.memoryarchitect.ui.theme.RoomSkinVisualSpec

/**
 * A premium [com.suman.memoryarchitect.domain.model.CosmeticCategory.ROOM_SKIN]'s gameplay
 * recolor - the same "translucent wash drawn above the backdrop but below every placed object"
 * technique [LightingOverlay] already establishes, so a premium room skin composes cleanly with
 * whatever ambient lighting mood the level already rolled rather than fighting it. Renders
 * nothing when [spec] is `null` (no `ROOM_SKIN` equipped) - a true no-op, matching every other
 * "optional cosmetic param, default reproduces today's exact look" call site in this app.
 *
 * [BlendMode.Color], not [BlendMode.Overlay] - `Overlay`'s neutral (no-brightness-change) point is
 * raw value 0.5 on each R/G/B channel independently, unrelated to a color's overall luminance, so
 * it quietly darkened the room regardless of how "medium" a wallTint/floorTint looked by the
 * (WCAG-weighted) luminance this file's colors were originally checked against. `BlendMode.Color`
 * takes the wash color's hue/saturation while preserving the room's own existing luminance exactly
 * - see `objectMaterialTint`'s doc in `GameplayScenePanel.kt` for the same fix and the full
 * mechanism, and `CosmeticColorBlendTest.kt` for the computed proof.
 */
@Composable
fun RoomSkinOverlay(spec: RoomSkinVisualSpec?, modifier: Modifier = Modifier) {
    if (spec == null) return
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(listOf(spec.wallTint.copy(alpha = 0.16f), spec.floorTint.copy(alpha = 0.16f))),
            size = size,
            blendMode = BlendMode.Color,
        )
    }
}
