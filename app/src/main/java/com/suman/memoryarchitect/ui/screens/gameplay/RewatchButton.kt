package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.RewardedAdFailureReason
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/**
 * Replays the Memorize phase while [remaining] &gt; 0, no ad involved (unlike Hint/Redo there's
 * no "armed" step first - replaying is a single instant action); once the free budget hits zero
 * the same button switches its tap action to [onWatchAd] instead - see [AssistIconBadge]'s doc
 * for why the icon never changes, only the corner badge does.
 */
@Composable
fun RewatchButton(
    remaining: Int,
    onClick: () -> Unit,
    adState: RewardedAdUiState,
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    val hasFreeUses = remaining > 0
    val isLoading = !hasFreeUses && adState is RewardedAdUiState.Loading

    val description = when {
        hasFreeUses -> stringResource(R.string.gameplay_rewatch_free_description, remaining)
        adState is RewardedAdUiState.Loading -> stringResource(R.string.gameplay_rewatch_loading_description)
        adState is RewardedAdUiState.Failed -> stringResource(
            when (adState.reason) {
                RewardedAdFailureReason.NO_INTERNET -> R.string.gameplay_rewatch_no_internet
                RewardedAdFailureReason.AD_UNAVAILABLE -> R.string.gameplay_rewatch_unavailable
                RewardedAdFailureReason.SHOW_FAILED -> R.string.gameplay_rewatch_show_failed
            },
        )
        else -> stringResource(R.string.gameplay_rewatch_idle_description)
    }

    GlassCard(
        modifier = modifier
            .pressableScale(interactionSource)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(interactionSource = interactionSource, indication = null, enabled = !isLoading) {
                tick()
                if (hasFreeUses) onClick() else onWatchAd()
            },
        tint = MemoryArchitectColors.accentSage.copy(alpha = 0.12f),
        shape = RoundedCornerShape(MemoryArchitectRadii.chip),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = if (compact) 56.dp else 68.dp)
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AssistIconBadge(
                icon = Icons.Filled.Visibility,
                iconTint = MemoryArchitectColors.accentSage,
                remaining = remaining,
                isLoading = isLoading,
            )
            Text(
                text = stringResource(R.string.gameplay_rewatch_name),
                color = MemoryArchitectColors.textSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
