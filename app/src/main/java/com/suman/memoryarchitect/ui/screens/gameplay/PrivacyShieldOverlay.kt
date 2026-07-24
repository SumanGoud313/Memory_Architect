package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.feature.gameplay.PrivacyShieldPhase
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.warmGlowOverlay
import com.suman.memoryarchitect.ui.components.warmGradientBackground
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.glowTitleStyle

private sealed interface ShieldContent {
    data object HiddenAway : ShieldContent
    data object ReadyToContinue : ShieldContent
    data class Countdown(val value: Int) : ShieldContent
}

/**
 * Full-screen, fully opaque cover for the Anti-Cheat Privacy Shield - shown whenever [phase] is
 * anything but [PrivacyShieldPhase.NONE] (see [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.onPaused]/
 * [onPrivacyShieldContinue]). Deliberately not a plain black screen: this is what a player
 * actually sees the moment they return to the app, so it carries the same premium warm identity
 * as the rest of the game rather than reading as an error or a crash.
 *
 * This is the in-app half of the fix - it's what a player sees while *inside* the app during a
 * focus-loss transition and, most importantly, the very first thing shown on return. It does not
 * by itself keep the scene out of Recent Apps' task thumbnail/a screenshot/screen recording; that
 * guarantee comes from [android.view.WindowManager.LayoutParams.FLAG_SECURE], set at the window
 * level for the whole time [GameplayScreen] is on screen (a Compose overlay can never race that
 * reliably, since the system's task-snapshot is captured by the window compositor, outside
 * Compose's control).
 *
 * Consumes every touch itself (see the root `clickable`) so no gesture can reach the gameplay
 * content underneath while this is showing - combined with the caller freezing the timer/music/
 * game state before this ever appears, nothing behind it can change while it's up.
 */
@Composable
fun PrivacyShieldOverlay(
    phase: PrivacyShieldPhase,
    countdownValue: Int?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .warmGradientBackground()
            .background(MemoryArchitectColors.bgBase.copy(alpha = 0.86f))
            .warmGlowOverlay()
            // Swallows every tap/drag that lands on the shield rather than letting it reach
            // whatever's composed underneath - the empty lambda is intentional, this exists purely
            // to claim the gesture.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .blur(22.dp)
                        .background(MemoryArchitectColors.accentGold.copy(alpha = 0.22f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .idlePulse()
                        .background(MemoryArchitectColors.glassFillPressed, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MemoryArchitectColors.accentGold,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = glowTitleStyle(MemoryArchitectColors.accentGold).copy(fontSize = 20.sp),
                color = MemoryArchitectColors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            val content = when {
                countdownValue != null -> ShieldContent.Countdown(countdownValue)
                phase == PrivacyShieldPhase.HIDDEN_AWAY -> ShieldContent.HiddenAway
                else -> ShieldContent.ReadyToContinue
            }
            AnimatedContent(
                targetState = content,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
                label = "privacyShieldContent",
            ) { state ->
                when (state) {
                    is ShieldContent.Countdown -> Text(
                        text = state.value.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MemoryArchitectColors.accentGold,
                    )
                    ShieldContent.HiddenAway -> ShieldMessage(
                        title = stringResource(R.string.gameplay_privacy_shield_hidden_title),
                        subtitle = stringResource(R.string.gameplay_privacy_shield_hidden_subtitle),
                    )
                    ShieldContent.ReadyToContinue -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ShieldMessage(
                            title = stringResource(R.string.gameplay_privacy_shield_ready_title),
                            subtitle = stringResource(R.string.gameplay_privacy_shield_ready_subtitle),
                        )
                        Spacer(Modifier.height(28.dp))
                        PrimaryButton(
                            text = stringResource(R.string.gameplay_privacy_shield_continue),
                            onClick = onContinue,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldMessage(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MemoryArchitectColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MemoryArchitectColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
