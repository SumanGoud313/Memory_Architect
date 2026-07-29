package com.suman.memoryarchitect.ui.screens.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.feature.missions.MissionsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Today's/this week's/this month's active missions - reached via a corner button on
 * [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen], same relationship
 * [com.suman.memoryarchitect.ui.screens.profile.AchievementsScreen] has to Profile. The active set
 * is fully deterministic (see [com.suman.memoryarchitect.domain.progression.MissionCatalog]) so
 * every device shows the same three Daily/three Weekly/one Monthly mission on a given day. */
@Composable
fun MissionsScreen(onBack: () -> Unit, viewModel: MissionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()
    val context = LocalContext.current

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.missions_header),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            if (uiState.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var rowIndex = 1
                    uiState.activeEvent?.let { event ->
                        LiveEventBanner(
                            event = event,
                            nowEpochSecond = System.currentTimeMillis() / 1000L,
                            modifier = Modifier.staggeredReveal(0),
                        )
                        rowIndex++
                    }
                    MissionPeriod.entries.forEach { period ->
                        val missionsForPeriod = uiState.missions.filter { it.definition.period == period }
                        if (missionsForPeriod.isEmpty()) return@forEach
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp).staggeredReveal(rowIndex),
                        ) {
                            Text(
                                text = stringResource(period.toSectionTitleRes()),
                                style = MaterialTheme.typography.labelLarge,
                                color = MemoryArchitectColors.textSecondary,
                            )
                            uiState.nextRotationEpochSecondByPeriod[period]?.let { targetEpochSecond ->
                                MissionCountdown(targetEpochSecond = targetEpochSecond)
                            }
                        }
                        rowIndex++
                        missionsForPeriod.forEach { mission ->
                            MissionRow(
                                mission = mission,
                                isClaiming = uiState.claimingMissionId == mission.definition.id,
                                onClaim = { viewModel.claim(mission.definition.id) },
                                modifier = Modifier.staggeredReveal(rowIndex),
                            )
                            rowIndex++
                        }
                        if (period != MissionPeriod.EVENT) {
                            CategoryBonusBanner(
                                period = period,
                                isComplete = uiState.isCategoryComplete(period),
                                modifier = Modifier.padding(top = 4.dp).staggeredReveal(rowIndex),
                            )
                            rowIndex++
                        }
                    }
                    if (uiState.claimError != null) {
                        Text(
                            text = uiState.claimError!!.toDisplayMessage(context),
                            color = MemoryArchitectColors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp).staggeredReveal(rowIndex),
                        )
                        rowIndex++
                    }
                    if (uiState.canUnlockAllNow) {
                        UnlockAllMissionsCard(
                            isUnlocking = uiState.isUnlockingAll,
                            error = uiState.unlockAllError?.toDisplayMessage(context),
                            onUnlockAll = viewModel::unlockAllMissionsNow,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).staggeredReveal(rowIndex),
                        )
                    }
                }
            }
            // Below the scrollable mission list, not overlaying it - the list above now takes
            // `weight(1f)` (was fillMaxSize()) specifically to leave this room. See
            // AdaptiveBannerAd's own doc for why this renders nothing at all for a Remove Ads
            // purchaser.
            AdaptiveBannerAd(placement = "missions", modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun MissionPeriod.toSectionTitleRes(): Int = when (this) {
    MissionPeriod.DAILY -> R.string.missions_section_daily
    MissionPeriod.WEEKLY -> R.string.missions_section_weekly
    MissionPeriod.MONTHLY -> R.string.missions_section_monthly
    MissionPeriod.EVENT -> R.string.missions_section_event
}

private fun MissionPeriod.toBonusPendingRes(): Int = when (this) {
    MissionPeriod.DAILY -> R.string.missions_category_bonus_daily_pending
    MissionPeriod.WEEKLY -> R.string.missions_category_bonus_weekly_pending
    MissionPeriod.MONTHLY -> R.string.missions_category_bonus_monthly_pending
    MissionPeriod.EVENT -> R.string.missions_category_bonus_daily_pending // unreachable - never called for EVENT
}

/** Communicates the per-period completion bonus - see [MissionCategoryBonusCatalog]'s doc for
 * what each period actually pays. Pending copy while incomplete ("Finish all 3 for..."),
 * replaced by a plain confirmation the instant [isComplete] flips true - the bonus itself is
 * granted automatically by [MissionsViewModel], never a separate tap here. */
@Composable
private fun CategoryBonusBanner(period: MissionPeriod, isComplete: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (isComplete) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MemoryArchitectColors.accentGold,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = if (isComplete) stringResource(R.string.missions_category_bonus_claimed) else stringResource(period.toBonusPendingRes()),
            style = MaterialTheme.typography.labelSmall,
            color = if (isComplete) MemoryArchitectColors.accentGold else MemoryArchitectColors.textTertiary,
            modifier = Modifier.padding(start = if (isComplete) 4.dp else 0.dp),
        )
    }
}

/** The pay-1000-coins-to-reroll-early affordance - only ever composed while
 * [MissionsUiState.canUnlockAllNow] is true (every mission in all three periods already claimed),
 * per the request this was built for. */
@Composable
private fun UnlockAllMissionsCard(
    isUnlocking: Boolean,
    error: String?,
    onUnlockAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, tint = MemoryArchitectColors.accentGold.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isUnlocking) {
                CircularProgressIndicator(color = MemoryArchitectColors.accentGold, modifier = Modifier.size(24.dp))
            } else {
                PrimaryButton(text = stringResource(R.string.missions_unlock_all_action), onClick = onUnlockAll)
            }
            if (error != null) {
                Text(
                    text = error,
                    color = MemoryArchitectColors.danger,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
