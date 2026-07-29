package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.feature.gameplay.HintUiState
import com.suman.memoryarchitect.feature.gameplay.RedoUiState
import com.suman.memoryarchitect.feature.gameplay.RewatchUiState
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Read-only breakdown of the three assist resources - "clearly see what you own" for Hint/Redo/
 * Rewatch, the surface the compact [GameplayToolbar] buttons and their corner badges can't fit at
 * a glance. Purely informational: unlike watching a rewarded ad, opening this never pauses the
 * Reconstruct countdown - there's nothing here worth a timer grace period over, and pausing for it
 * would let a player stall the clock just by leaving it open.
 */
@Composable
fun AssistResourcesDialog(
    hintState: HintUiState,
    redoState: RedoUiState,
    rewatchState: RewatchUiState,
    onDismiss: () -> Unit,
) {
    val feedback = rememberFeedback()
    LaunchedEffect(Unit) { feedback.onDialogOpen() }
    AlertDialog(
        onDismissRequest = { feedback.onDialogClose(); onDismiss() },
        title = { Text(stringResource(R.string.gameplay_assist_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AssistResourceRow(
                    name = stringResource(R.string.gameplay_hint_name),
                    free = hintState.remaining,
                    inventory = hintState.inventoryTokenCount,
                    adsLeft = hintState.rewardedRemaining,
                )
                HorizontalDivider()
                AssistResourceRow(
                    name = stringResource(R.string.gameplay_redo_name),
                    free = redoState.remaining,
                    inventory = redoState.inventoryTokenCount,
                    adsLeft = redoState.rewardedRemaining,
                )
                HorizontalDivider()
                // No "Free" line for Rewatch - the free tier is currently zero in every level (see
                // RewatchUiState's own doc), so showing it here would only ever read "Free: 0",
                // noise rather than information.
                AssistResourceRow(
                    name = stringResource(R.string.gameplay_rewatch_name),
                    free = null,
                    inventory = rewatchState.inventoryTokenCount,
                    adsLeft = rewatchState.rewardedRemaining,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { feedback.onDialogClose(); onDismiss() }) {
                Text(stringResource(R.string.gameplay_assist_info_close))
            }
        },
    )
}

@Composable
private fun AssistResourceRow(name: String, free: Int?, inventory: Int, adsLeft: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleSmall, color = MemoryArchitectColors.textPrimary)
        if (free != null) {
            Text(
                text = stringResource(R.string.gameplay_assist_info_free, free),
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.gameplay_assist_info_inventory, inventory),
            style = MaterialTheme.typography.bodyMedium,
            color = MemoryArchitectColors.textSecondary,
        )
        Text(
            text = stringResource(R.string.gameplay_assist_info_ads_left, adsLeft),
            style = MaterialTheme.typography.bodyMedium,
            color = MemoryArchitectColors.textSecondary,
        )
    }
}
