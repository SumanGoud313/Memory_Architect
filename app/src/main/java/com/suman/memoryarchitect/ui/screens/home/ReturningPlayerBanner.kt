package com.suman.memoryarchitect.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.ReturningPlayerTier
import com.suman.memoryarchitect.domain.model.ReturningPlayerWelcome
import com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** [ReturningPlayerTier.SHORT] is tone-only (no [onClaimGift] affordance shown at all) - "a reward
 * for a few days away would read as a bribe, not a welcome," per the retention plan. Never claws
 * back anything regardless of tier - this banner only ever adds a warm note and, for
 * [ReturningPlayerTier.MEDIUM]/[ReturningPlayerTier.LONG], a small gift on top of whatever the
 * player already has. */
@Composable
fun ReturningPlayerBanner(
    welcome: ReturningPlayerWelcome,
    isClaiming: Boolean,
    onClaimGift: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), tint = MemoryArchitectColors.accentGold.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.returning_player_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.textPrimary,
                    )
                    Text(
                        text = welcome.tier.messageRes(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (welcome.tier == ReturningPlayerTier.LONG && welcome.journeyTierId != null) {
                        Text(
                            text = stringResource(
                                R.string.returning_player_journey_reminder,
                                MemoryJourneyCatalog.definitionFor(welcome.journeyTierId).title,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MemoryArchitectColors.accentGold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = MemoryArchitectColors.textTertiary)
                }
            }
            if (welcome.canClaimGift) {
                Row(modifier = Modifier.padding(top = 10.dp)) {
                    if (isClaiming) {
                        CircularProgressIndicator(color = MemoryArchitectColors.accentGold, modifier = Modifier.padding(8.dp))
                    } else {
                        PrimaryButton(text = stringResource(R.string.returning_player_claim_gift), onClick = onClaimGift)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturningPlayerTier.messageRes(): String = when (this) {
    ReturningPlayerTier.NONE -> "" // unreachable - GetReturningPlayerWelcomeUseCase never returns NONE
    ReturningPlayerTier.SHORT -> stringResource(R.string.returning_player_message_short)
    ReturningPlayerTier.MEDIUM -> stringResource(R.string.returning_player_message_medium)
    ReturningPlayerTier.LONG -> stringResource(R.string.returning_player_message_long)
}
