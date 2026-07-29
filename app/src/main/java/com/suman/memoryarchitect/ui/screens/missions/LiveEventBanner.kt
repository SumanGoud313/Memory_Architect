package com.suman.memoryarchitect.ui.screens.missions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.model.LiveEvent
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlin.math.max

/** Lightweight seasonal-event callout at the top of [MissionsScreen] - reuses the existing
 * Missions engine/screen rather than a new one, per the Points Economy plan's Phase 4 scope (the
 * Event mission pool below this banner is the actual event content; this is just the "an event is
 * live, and it ends in X" signal). Never shown when [LiveEventCatalog.activeEvent][com.suman.memoryarchitect.domain.progression.LiveEventCatalog.activeEvent]
 * resolves to `null`, which is the common case. */
@Composable
fun LiveEventBanner(event: LiveEvent, nowEpochSecond: Long, modifier: Modifier = Modifier) {
    val remainingSeconds = max(0L, event.endEpochSecond - nowEpochSecond)
    val remainingDays = (remainingSeconds / 86_400L).toInt()

    GlassCard(modifier = modifier.fillMaxWidth(), tint = MemoryArchitectColors.accentTerracotta.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = event.toDisplayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MemoryArchitectColors.textPrimary,
            )
            Text(
                text = if (remainingDays > 0) {
                    pluralStringResource(R.plurals.live_event_ends_in_days, remainingDays, remainingDays)
                } else {
                    val remainingHours = max(1, (remainingSeconds / 3_600L).toInt())
                    pluralStringResource(R.plurals.live_event_ends_in_hours, remainingHours, remainingHours)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
