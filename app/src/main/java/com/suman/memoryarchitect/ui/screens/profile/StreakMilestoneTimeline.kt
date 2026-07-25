package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.StreakRules
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * The Streak System's full celebration timeline (3/7/14/30/60/90/180/365 days) - Profile's premium
 * counterpart to Home's quiet [HomeStreakChip]. A milestone marks itself reached once
 * [StreakRules.milestoneDays] is at or below the player's [currentStreak] - once the current run
 * resets, so do these markers, since they describe *this* run's progress toward the next
 * milestone, not a lifetime record (that permanent recognition already lives in
 * [com.suman.memoryarchitect.domain.model.PlayerProfile.longestStreak]'s own display). The one
 * still-ahead milestone whose shield icon is filled is the next Streak Shield on offer - see
 * [StreakRules.shieldMilestoneDays].
 */
@Composable
fun StreakMilestoneTimeline(
    currentStreak: Int,
    modifier: Modifier = Modifier,
    rules: StreakRules = StreakRules.Default,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.profile_streak_milestones_header),
            style = MaterialTheme.typography.titleMedium,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            rules.milestoneDays.forEach { day ->
                MilestoneNode(
                    day = day,
                    reached = currentStreak >= day,
                    grantsShield = day in rules.shieldMilestoneDays,
                )
            }
        }
    }
}

@Composable
private fun MilestoneNode(day: Int, reached: Boolean, grantsShield: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (reached) MemoryArchitectColors.accentTerracotta.copy(alpha = 0.9f) else MemoryArchitectColors.glassFill,
                    CircleShape,
                )
                .graphicsLayer { alpha = if (reached) 1f else 0.6f },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (reached) MemoryArchitectColors.bgBase else MemoryArchitectColors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
            if (grantsShield) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = stringResource(R.string.profile_streak_milestone_shield_description),
                    tint = if (reached) MemoryArchitectColors.bgBase else MemoryArchitectColors.accentSage,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.profile_streak_milestone_day, day),
            style = MaterialTheme.typography.labelSmall,
            color = if (reached) MemoryArchitectColors.textSecondary else MemoryArchitectColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
