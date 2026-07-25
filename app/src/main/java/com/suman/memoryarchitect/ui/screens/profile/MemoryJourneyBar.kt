package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.MemoryJourneyStanding
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LevelProgressBar
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * The premium animated bar - a permanent, never-resetting readout of [MemoryJourneyStanding],
 * shown on Profile alongside the streak/coins pills. Reads a side effect of things the player was
 * already doing for other reasons (see [com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog]'s
 * doc) - there is deliberately no claim/tap affordance here, only a bar that quietly fills.
 */
@Composable
fun MemoryJourneyBar(standing: MemoryJourneyStanding, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), tint = MemoryArchitectColors.accentGold.copy(alpha = 0.08f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MemoryArchitectColors.accentGold)
                    Text(
                        text = standing.current?.title ?: stringResource(R.string.memory_journey_unranked),
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.textPrimary,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.memory_journey_points_format, standing.totalPoints),
                    style = MaterialTheme.typography.labelLarge,
                    color = MemoryArchitectColors.textSecondary,
                )
            }
            LevelProgressBar(fraction = standing.progressToNext, modifier = Modifier.padding(top = 12.dp))
            Text(
                text = standing.next?.let { stringResource(R.string.memory_journey_next_tier, it.title) }
                    ?: stringResource(R.string.memory_journey_max_tier),
                style = MaterialTheme.typography.labelSmall,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
