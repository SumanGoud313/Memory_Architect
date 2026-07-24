package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
 * Toggles hint-selection mode while [remaining] &gt; 0 (tapping while armed disarms it instead);
 * once the free budget is exhausted the exact same button switches its tap action to
 * [onWatchAd] instead - see [AssistIconBadge]'s doc for why the icon/glyph never changes just
 * because the count hit zero, only the small corner badge does (a number, or "Ad"). This single
 * control replaces the old pattern of swapping to an entirely different `RewardedHintButton`
 * composable in that state.
 *
 * [compact] narrows the button for tight layouts - the icon, badge, and name label all stay, only
 * the horizontal padding shrinks.
 */
@Composable
fun HintButton(
    remaining: Int,
    isArmed: Boolean,
    onClick: () -> Unit,
    adState: RewardedAdUiState,
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tick = rememberHapticsTick()
    val hasFreeUses = remaining > 0
    val isLoading = !hasFreeUses && adState is RewardedAdUiState.Loading
    val interactionSource = remember { MutableInteractionSource() }

    val infiniteTransition = rememberInfiniteTransition(label = "hintButtonArmed")
    val armedGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintArmedGlow",
    )
    val armedGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.96f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintArmedGlowScale",
    )

    val description = when {
        hasFreeUses && isArmed -> stringResource(R.string.gameplay_hint_armed_description)
        hasFreeUses -> stringResource(R.string.gameplay_hint_idle_description, remaining)
        adState is RewardedAdUiState.Loading -> stringResource(R.string.gameplay_rewarded_hint_loading_description)
        adState is RewardedAdUiState.Failed -> stringResource(
            when (adState.reason) {
                RewardedAdFailureReason.NO_INTERNET -> R.string.gameplay_rewarded_hint_no_internet
                RewardedAdFailureReason.AD_UNAVAILABLE -> R.string.gameplay_rewarded_hint_unavailable
                RewardedAdFailureReason.SHOW_FAILED -> R.string.gameplay_rewarded_hint_show_failed
            },
        )
        else -> stringResource(R.string.gameplay_rewarded_hint_watch_ad_description)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (hasFreeUses && isArmed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = armedGlowAlpha
                        scaleX = armedGlowScale
                        scaleY = armedGlowScale
                    }
                    .blur(18.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentGold.copy(alpha = 0f))),
                        shape = CircleShape,
                    ),
            )
        }

        GlassCard(
            modifier = Modifier
                .pressableScale(interactionSource)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = description
                }
                .clickable(interactionSource = interactionSource, indication = null, enabled = !isLoading) {
                    tick()
                    if (hasFreeUses) onClick() else onWatchAd()
                },
            tint = when {
                hasFreeUses && isArmed -> MemoryArchitectColors.accentGold.copy(alpha = 0.55f)
                else -> null
            },
            shape = RoundedCornerShape(MemoryArchitectRadii.chip),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = if (compact) 56.dp else 68.dp)
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AssistIconBadge(
                    icon = Icons.Filled.Lightbulb,
                    iconTint = MemoryArchitectColors.accentGold,
                    remaining = remaining,
                    isLoading = isLoading,
                )
                Text(
                    text = stringResource(R.string.gameplay_hint_name),
                    color = MemoryArchitectColors.textSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
