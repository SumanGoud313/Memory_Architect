package com.suman.memoryarchitect.ui.screens.modeselect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.feature.modeselect.ModeSelectProgressUiState
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LevelProgressBar
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * The screen's dedicated bottom content area — a compact echo of Profile's hero (level, XP,
 * coins, streak), not a duplicate of it, so Mode Select never ends in dead space below the last
 * card. Shares [LevelProgressBar] and [PillBadge] with Level Select/Profile so the "progress"
 * visual language reads as one system across the app.
 */
@Composable
fun ModeSelectProgressCard(progress: ModeSelectProgressUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val progressFraction = if (progress.xpForNextLevel > 0) {
        progress.xpIntoLevel.toFloat() / progress.xpForNextLevel.toFloat()
    } else {
        0f
    }

    GlassCard(modifier = modifier.fillMaxWidth(), tint = MemoryArchitectColors.accentGold.copy(alpha = 0.08f)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(MemoryArchitectColors.accentGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${progress.level}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MemoryArchitectColors.bgBase,
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(
                        text = context.getString(R.string.mode_select_progress_level, progress.level),
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.textPrimary,
                    )
                    LevelProgressBar(fraction = progressFraction, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = context.getString(R.string.mode_select_progress_xp, progress.xpIntoLevel, progress.xpForNextLevel),
                        style = MaterialTheme.typography.labelMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PillBadge(
                    text = context.getString(R.string.profile_coins, progress.coins),
                    icon = Icons.Filled.MonetizationOn,
                    contentColor = MemoryArchitectColors.accentGold,
                )
                PillBadge(
                    text = context.getString(R.string.mode_select_progress_streak, progress.currentStreak),
                    icon = Icons.Filled.LocalFireDepartment,
                    contentColor = MemoryArchitectColors.accentTerracotta,
                )
            }
        }
    }
}
