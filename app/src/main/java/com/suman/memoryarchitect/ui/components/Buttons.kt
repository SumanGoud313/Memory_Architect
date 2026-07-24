package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/** Gradient pill button — replaces `bg_button_glow_gradient` (e.g. Home's Play button). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tick = rememberHapticsTick()
    val shape = RoundedCornerShape(MemoryArchitectRadii.button)
    Box(
        modifier = modifier
            .pressableScale(interactionSource)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold),
                ),
                shape = shape,
            )
            // No-op unless a Premium Border is equipped - see PremiumBorder.kt's doc.
            .premiumBorder(shape = shape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) {
                tick()
                onClick()
            }
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Text(text = text, color = MemoryArchitectColors.bgBase.copy(alpha = if (enabled) 1f else 0.6f))
    }
}

/** Outlined glass button — replaces `bg_button_glass_outline` (e.g. Retry buttons). [horizontalPadding]
 * defaults to the original fixed 32dp every existing call site already relies on; a narrower value
 * is for a button squeezed into a `weight(1f)` row alongside a sibling (e.g. Profile's Statistics/
 * Leaderboards pair) where the full padding would force longer labels onto two lines. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 32.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tick = rememberHapticsTick()
    val shape = RoundedCornerShape(MemoryArchitectRadii.button)
    // A Premium Border REPLACES (never stacks with) this button's own always-on 1dp glass
    // outline - two overlapping borders on a thin pill reads as a rendering bug, not a feature.
    val hasEquippedBorder = LocalEquippedCosmetics.current[CosmeticCategory.PROFILE_BORDER] != null
    Box(
        modifier = modifier
            .pressableScale(interactionSource)
            .then(
                if (hasEquippedBorder) {
                    Modifier.premiumBorder(shape = shape)
                } else {
                    Modifier.border(width = 1.dp, color = MemoryArchitectColors.glassStrokeBright, shape = shape)
                },
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                tick()
                onClick()
            }
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = MemoryArchitectColors.textPrimary, maxLines = 1)
    }
}
