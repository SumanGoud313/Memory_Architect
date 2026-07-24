package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** A small round glass-chip icon button - originally Home's profile/settings corner buttons, now
 * shared wherever a screen needs the same "small round icon in a corner" affordance (e.g. Mode
 * Select's Remove Ads shortcut) rather than a second copy of the same Box/clickable/Icon shape.
 * [label], when non-null, renders a short caption below the circle (e.g. "Remove Ads") - `null` by
 * default so Home's existing profile/settings icons render exactly as before. [category] picks
 * which equipped cosmetic ring shows here - defaults to [CosmeticCategory.PROFILE_BORDER] so every
 * existing call site is unchanged; Home's profile icon overrides it to [CosmeticCategory.AVATAR_FRAME]
 * so the equipped Avatar Frame is visible outside the Profile screen too. */
@Composable
fun IconGlassButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    label: String? = null,
    category: CosmeticCategory = CosmeticCategory.PROFILE_BORDER,
) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .pressableScale(interactionSource)
                    .background(MemoryArchitectColors.glassFill, CircleShape)
                    // No-op unless something's equipped for `category` - always static/thin here,
                    // see premiumBorderRing's doc (a fast shimmer on a small corner icon is noise).
                    .premiumBorderRing(category = category)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        tick()
                        onClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MemoryArchitectColors.textPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .idlePulse(minScale = 0.9f, maxScale = 1.15f, periodMs = 1400)
                        .background(MemoryArchitectColors.accentGold, CircleShape),
                )
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
