package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.MemoryJourneyStanding
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * The one deliberately-oversized, gold-washed row button on Profile - unlike the plain
 * [com.suman.memoryarchitect.ui.components.OutlineButton] rows around it (Statistics/Leaderboards,
 * Achievements), Memory Journey is a lifetime, never-resetting standing (see
 * [com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog]'s doc), so its entry point
 * gets its own full-width, higher-ceremony treatment rather than sharing a half-width slot with
 * ordinary navigation buttons.
 */
@Composable
fun MemoryJourneyButton(standing: MemoryJourneyStanding, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val tick = rememberHapticsTick()
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .pressableScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {
                tick()
                onClick()
            },
        // Twice MemoryJourneyBar's own 0.08 tint - the deliberately stronger gold wash is the
        // "different style" this button needs to read as special rather than one more OutlineButton.
        tint = MemoryArchitectColors.accentGold.copy(alpha = 0.18f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(MemoryArchitectColors.accentGold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MemoryArchitectColors.bgBase, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.memory_journey_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MemoryArchitectColors.textPrimary,
                )
                Text(
                    text = standing.current?.title ?: stringResource(R.string.memory_journey_unranked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.accentGold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.memory_journey_points_format, standing.totalPoints),
                    style = MaterialTheme.typography.labelLarge,
                    color = MemoryArchitectColors.textSecondary,
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MemoryArchitectColors.textTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
