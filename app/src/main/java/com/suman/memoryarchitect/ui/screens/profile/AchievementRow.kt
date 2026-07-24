package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private fun AchievementId.toIcon(): ImageVector = when (this) {
    AchievementId.FIRST_STEPS -> Icons.Filled.Flag
    AchievementId.DEDICATED -> Icons.Filled.Whatshot
    AchievementId.CENTURY -> Icons.Filled.WorkspacePremium
    AchievementId.SHARP_EYE -> Icons.Filled.Visibility
    AchievementId.PERFECTIONIST -> Icons.Filled.AutoAwesome
    AchievementId.WEEK_STREAK -> Icons.Filled.LocalFireDepartment
    AchievementId.MONTH_STREAK -> Icons.Filled.CalendarMonth
    AchievementId.RISING_STAR -> Icons.AutoMirrored.Filled.TrendingUp
    AchievementId.ARCHITECT -> Icons.Filled.Villa
    AchievementId.FLAWLESS -> Icons.Filled.Diamond
    AchievementId.GRAND_ARCHITECT -> Icons.Filled.EmojiEvents
    AchievementId.FIRST_PERFECT_ROOM -> Icons.Filled.StarBorder
    AchievementId.PERFECT_ROOMS_10 -> Icons.Filled.Grade
    AchievementId.PERFECT_ROOMS_100 -> Icons.Filled.WorkspacePremium
    AchievementId.LEVEL_25 -> Icons.Filled.Terrain
    AchievementId.LEVEL_50 -> Icons.Filled.Explore
    AchievementId.LEVEL_100 -> Icons.Filled.Public
    AchievementId.TOP_100_DAILY -> Icons.Filled.Leaderboard
    AchievementId.TOP_10_WEEKLY -> Icons.Filled.MilitaryTech
    AchievementId.MEMORY_MASTER -> Icons.Filled.Psychology
    AchievementId.SPEED_RUNNER -> Icons.Filled.Bolt
    AchievementId.PRECISION_EXPERT -> Icons.Filled.GpsFixed
}

@Composable
fun AchievementRow(id: AchievementId, isUnlocked: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val title = id.toDisplayTitle(context)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = if (isUnlocked) MemoryArchitectColors.accentGold.copy(alpha = 0.1f) else null,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isUnlocked) MemoryArchitectColors.accentGold else MemoryArchitectColors.glassFill,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isUnlocked) id.toIcon() else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) MemoryArchitectColors.bgBase else MemoryArchitectColors.textTertiary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isUnlocked) MemoryArchitectColors.textPrimary else MemoryArchitectColors.textTertiary,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}
