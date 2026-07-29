package com.suman.memoryarchitect.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.feature.home.DailyRewardBadgeViewModel
import com.suman.memoryarchitect.feature.home.ReturningPlayerViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.IconGlassButton
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Compose replacement for HomeFragment — the landing screen. The center circle *is* the Play
 * button (see [HeroPlayButton]) — the signature interaction of the whole game — plus branding:
 * coins/streak/level/stats/achievements/challenges all live under Profile now, where they belong
 * as *your* details rather than the first thing you see on launch. The one exception is a small
 * badge dot on the profile icon when a Daily Reward is waiting — a quiet nudge, not a second call
 * to action, so the retention hook stays discoverable without turning Home back into a dashboard. */
@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    dailyRewardBadgeViewModel: DailyRewardBadgeViewModel = hiltViewModel(),
    returningPlayerViewModel: ReturningPlayerViewModel = hiltViewModel(),
) {
    val particles = rememberParticleFieldState()
    val hasClaimableReward by dailyRewardBadgeViewModel.hasClaimableReward.collectAsStateWithLifecycle()
    val currentStreak by dailyRewardBadgeViewModel.currentStreak.collectAsStateWithLifecycle()
    val returningPlayerWelcome by returningPlayerViewModel.welcome.collectAsStateWithLifecycle()
    val isClaimingReturningGift by returningPlayerViewModel.isClaiming.collectAsStateWithLifecycle()
    // Fades the surrounding chrome the instant a tap is confirmed, concurrent with the circle's
    // own press/ripple/burst animation, so the whole screen reads as dissolving into the button
    // rather than the button animating alone while everything else sits static.
    var isExiting by remember { mutableStateOf(false) }

    AmbientBackground(nearParticles = particles) {
        AnimatedVisibility(visible = !isExiting, exit = fadeOut(tween(280))) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                IconGlassButton(
                    icon = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.home_profile),
                    onClick = onProfile,
                    showBadge = hasClaimableReward,
                    category = CosmeticCategory.AVATAR_FRAME,
                    modifier = Modifier.align(Alignment.CenterStart).staggeredReveal(0),
                )
                IconGlassButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    onClick = onSettings,
                    modifier = Modifier.align(Alignment.CenterEnd).staggeredReveal(0),
                )
                HomeStreakChip(
                    currentStreak = currentStreak,
                    modifier = Modifier.align(Alignment.TopCenter).staggeredReveal(0),
                )
            }
        }

        HeroPlayButton(
            onPlay = onPlay,
            onPressStart = { isExiting = true },
            modifier = Modifier.align(Alignment.Center).staggeredReveal(1),
        )

        AnimatedVisibility(
            visible = !isExiting,
            exit = fadeOut(tween(280)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                returningPlayerWelcome?.let { welcome ->
                    ReturningPlayerBanner(
                        welcome = welcome,
                        isClaiming = isClaimingReturningGift,
                        onClaimGift = returningPlayerViewModel::claimGift,
                        onDismiss = returningPlayerViewModel::dismiss,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MemoryArchitectColors.accentGold,
                    modifier = Modifier.staggeredReveal(2),
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MemoryArchitectColors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp).staggeredReveal(3),
                )
            }
        }
    }
}
