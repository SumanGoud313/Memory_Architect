package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.MemoryJourneyTier
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** One row in [MemoryJourneyScreen]'s full tier showcase - same [GlassCard] + icon-circle shape
 * [com.suman.memoryarchitect.ui.screens.profile.AchievementRow] already uses. */
@Composable
fun MemoryJourneyTierRow(tier: MemoryJourneyTier, isUnlocked: Boolean, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = if (isUnlocked) MemoryArchitectColors.accentGold.copy(alpha = 0.1f) else null,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isUnlocked) MemoryArchitectColors.accentGold else MemoryArchitectColors.glassFill, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Filled.AutoAwesome else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) MemoryArchitectColors.bgBase else MemoryArchitectColors.textTertiary,
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = tier.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUnlocked) MemoryArchitectColors.textPrimary else MemoryArchitectColors.textTertiary,
                )
                Text(
                    text = stringResource(R.string.memory_journey_points_format, tier.thresholdPoints),
                    style = MaterialTheme.typography.labelSmall,
                    color = MemoryArchitectColors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
