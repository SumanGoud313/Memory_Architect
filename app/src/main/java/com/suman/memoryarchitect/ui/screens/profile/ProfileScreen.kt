package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.feature.profile.AccountViewModel
import com.suman.memoryarchitect.feature.profile.ProfileUiState
import com.suman.memoryarchitect.feature.profile.ProfileViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.AnimatedCounter
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.StatTile
import com.suman.memoryarchitect.ui.components.UnlockBurst
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.AccentPaletteCatalog
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay

private const val CLAIM_CELEBRATION_DURATION_MS = 1800L

/** Compose replacement for ProfileFragment — level ring, coins/streak, daily reward calendar,
 * daily/weekly challenge entry points, stats grid, and achievements. This is where all of the
 * player's "details" live; Home stays a bare Play button. */
@Composable
fun ProfileScreen(
    onOpenStatistics: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenRewards: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val particles = rememberParticleFieldState()
    var celebration by remember { mutableStateOf<DailyRewardClaimResult?>(null) }

    LaunchedEffect(Unit) {
        viewModel.claimEvents.collect { result ->
            particles.confettiBurst(Offset(400f, 500f))
            celebration = result
            delay(CLAIM_CELEBRATION_DURATION_MS)
            celebration = null
        }
    }

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
            }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.error.toDisplayMessage(context), color = MemoryArchitectColors.textSecondary)
                    OutlineButton(
                        text = stringResource(R.string.action_retry),
                        onClick = viewModel::retry,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            is ProfileUiState.Content -> ProfileContent(
                state = state,
                accountViewModel = accountViewModel,
                onClaimDailyReward = viewModel::claimDailyReward,
                onOpenStatistics = onOpenStatistics,
                onOpenLeaderboard = onOpenLeaderboard,
                onOpenAchievements = onOpenAchievements,
                onOpenRewards = onOpenRewards,
            )
        }

        if (celebration != null) {
            DailyRewardCelebration(result = celebration, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Content,
    accountViewModel: AccountViewModel,
    onClaimDailyReward: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenRewards: () -> Unit,
) {
    val context = LocalContext.current
    // The Achievements/Rewards row is the new bottom-most interactive content on this screen -
    // without accounting for the system gesture-nav inset here, it can end up partially underneath
    // that reserved touch zone on edge-to-edge devices, making it unreliable to tap. Every other
    // scrollable screen in this app that reaches near the bottom edge (e.g. ModeSelectScreen)
    // already adds this; Profile just never needed to before. An extra fixed margin on top of the
    // raw inset gives the last row real clearance from the very edge of the gesture-nav zone, not
    // just flush against its boundary.
    val systemBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp + systemBarBottomPadding + 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val progressFraction = if (state.xpForNextLevel > 0) state.xpIntoLevel.toFloat() / state.xpForNextLevel.toFloat() else 0f
        val equippedSweep = AccentPaletteCatalog.get(state.equippedPaletteId)?.sweep
        val equippedFrameId = state.equippedCosmetics[CosmeticCategory.AVATAR_FRAME]
        Box(modifier = Modifier.padding(top = 8.dp).staggeredReveal(0), contentAlignment = Alignment.Center) {
            if (equippedFrameId != null) {
                AvatarFrameRing(frameId = equippedFrameId)
            }
            LevelProgressRing(
                level = state.level,
                progressFraction = progressFraction,
                accentSweep = equippedSweep ?: listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold, MemoryArchitectColors.accentTerracotta),
            )
        }
        if (state.equippedTitleId != null) {
            val nameColorId = state.equippedCosmetics[CosmeticCategory.NAME_COLOR]
            val nameColorBrush = nameColorId?.let { Brush.linearGradient(CosmeticVisualCatalog.get(it).gradientColors) }
            Text(
                text = state.equippedTitleId.toDisplayTitle(context),
                color = if (nameColorBrush != null) Color.Unspecified else MemoryArchitectColors.accentGold,
                style = if (nameColorBrush != null) {
                    MaterialTheme.typography.titleMedium.copy(brush = nameColorBrush)
                } else {
                    MaterialTheme.typography.titleMedium
                },
                modifier = Modifier.padding(top = 4.dp).staggeredReveal(0),
            )
        }
        if (state.memoryRank.isNotEmpty()) {
            Text(
                text = state.memoryRank,
                color = MemoryArchitectColors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp).staggeredReveal(0),
            )
        }
        Text(
            text = context.getString(R.string.profile_xp_progress, state.xpIntoLevel, state.xpForNextLevel),
            color = MemoryArchitectColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp).staggeredReveal(0),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp).staggeredReveal(1),
        ) {
            PillBadge(
                text = context.getString(R.string.profile_coins, state.profile.coins),
                icon = Icons.Filled.MonetizationOn,
                contentColor = MemoryArchitectColors.accentGold,
            )
            PillBadge(
                text = context.getString(R.string.profile_streak, state.profile.currentStreak, state.profile.longestStreak),
                icon = Icons.Filled.LocalFireDepartment,
                contentColor = MemoryArchitectColors.accentTerracotta,
            )
        }

        Text(
            text = stringResource(R.string.settings_account),
            style = MaterialTheme.typography.titleLarge,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp).staggeredReveal(1),
        )
        // Avatar Frame decorates the actual avatar glyph (AvatarRow's small circle); the Premium
        // Border on the identity/verification card (AccountStatusCard) is now automatic - every
        // GlassCard-based surface inherits the app-wide equipped border for free (see
        // GlassCard.kt/PremiumBorder.kt), no explicit wiring needed here anymore.
        AccountSection(
            viewModel = accountViewModel,
            equippedFrameId = state.equippedCosmetics[CosmeticCategory.AVATAR_FRAME],
            equippedNameColorId = state.equippedCosmetics[CosmeticCategory.NAME_COLOR],
            modifier = Modifier.staggeredReveal(1),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).staggeredReveal(1),
        ) {
            OutlineButton(
                text = stringResource(R.string.profile_view_statistics),
                onClick = onOpenStatistics,
                modifier = Modifier.weight(1f),
                horizontalPadding = 12.dp,
            )
            OutlineButton(
                text = stringResource(R.string.profile_view_leaderboards),
                onClick = onOpenLeaderboard,
                modifier = Modifier.weight(1f),
                horizontalPadding = 12.dp,
            )
        }

        if (state.dailyRewardStatus != null) {
            DailyRewardCard(
                status = state.dailyRewardStatus,
                isClaiming = state.isClaimingDailyReward,
                onClaim = onClaimDailyReward,
                modifier = Modifier.padding(top = 20.dp).staggeredReveal(1),
            )
        }

        Text(
            text = stringResource(R.string.profile_stats_header),
            style = MaterialTheme.typography.titleLarge,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp).staggeredReveal(2),
        )
        Column(modifier = Modifier.fillMaxWidth().staggeredReveal(2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Filled.VideogameAsset,
                    label = stringResource(R.string.profile_stat_games_played),
                    value = "${state.statistics.gamesPlayed}",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    icon = Icons.Filled.SportsScore,
                    label = stringResource(R.string.profile_stat_best_score),
                    value = "${state.statistics.bestScore}",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = stringResource(R.string.profile_stat_best_accuracy),
                    value = "${(state.statistics.bestAccuracy * 100).toInt()}%",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    icon = Icons.Filled.Timeline,
                    label = stringResource(R.string.profile_stat_total_score),
                    value = "${state.statistics.totalScore}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp).staggeredReveal(3),
        ) {
            OutlineButton(
                text = stringResource(R.string.profile_achievements_header),
                onClick = onOpenAchievements,
                modifier = Modifier.weight(1f),
                horizontalPadding = 12.dp,
            )
            OutlineButton(
                text = stringResource(R.string.profile_rewards_header),
                onClick = onOpenRewards,
                modifier = Modifier.weight(1f),
                horizontalPadding = 12.dp,
            )
        }
    }
}

/** Decorative ring drawn just outside [LevelProgressRing] when an Avatar Frame is equipped - a
 * thin sweep-gradient stroke, same Canvas-arc technique [LevelProgressRing] itself already uses,
 * sized 20dp larger so it frames the ring rather than overlapping its own stroke. */
@Composable
private fun AvatarFrameRing(frameId: CosmeticId, modifier: Modifier = Modifier) {
    val spec = CosmeticVisualCatalog.get(frameId)
    Canvas(modifier = modifier.size(148.dp)) {
        val strokeWidth = size.minDimension * 0.045f
        val inset = strokeWidth / 2f
        drawArc(
            brush = Brush.sweepGradient(spec.gradientColors),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun DailyRewardCelebration(result: DailyRewardClaimResult?, modifier: Modifier = Modifier) {
    if (result == null) return
    val context = LocalContext.current
    UnlockBurst(modifier = modifier) {
        GlassCard(tint = MemoryArchitectColors.accentGold.copy(alpha = 0.16f)) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.profile_daily_reward_claimed_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MemoryArchitectColors.accentGold,
                )
                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    AnimatedCounter(
                        target = result.coinsAwarded.toInt(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MemoryArchitectColors.textPrimary,
                        formatter = { context.getString(R.string.profile_daily_reward_coins_awarded, it) },
                    )
                }
                if (result.xpAwarded > 0) {
                    AnimatedCounter(
                        target = result.xpAwarded.toInt(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MemoryArchitectColors.textSecondary,
                        formatter = { context.getString(R.string.profile_daily_reward_xp_awarded, it) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
