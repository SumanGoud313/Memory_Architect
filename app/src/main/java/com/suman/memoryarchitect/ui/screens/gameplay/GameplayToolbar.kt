package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.feature.gameplay.HintUiState
import com.suman.memoryarchitect.feature.gameplay.RedoUiState
import com.suman.memoryarchitect.feature.gameplay.RewatchUiState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Below this available width, three full-label pills can't comfortably sit side by side on most
 * phones without squeezing or relying entirely on scroll to be discovered - every control drops
 * to its compact (icon + number, no word) form together at that point. */
private val CompactBreakpoint = 400.dp

/**
 * The Reconstruct-phase assist toolbar - Redo, Rewatch, and Hint, grouped as one cohesive,
 * responsive control cluster rather than three buttons that happen to share a row. Each control
 * is a single button for its whole lifetime now (see [HintButton]/[RedoButton]/[RewatchButton]) -
 * once a free budget hits zero the same button switches its own tap action to the rewarded-ad
 * flow rather than [GameplayToolbar] swapping in a different composable, so nothing about a
 * control's identity (icon, position, size) changes when that happens.
 *
 * [BoxWithConstraints] measures the space actually available and switches every control to its
 * compact form together below [CompactBreakpoint], rather than letting them compete individually
 * for space; a horizontal scroll remains underneath as a safety net for unusually narrow layouts
 * even in compact mode. Each control also staggers in on entrance.
 */
@Composable
fun GameplayToolbar(
    hintState: HintUiState,
    onToggleHint: () -> Unit,
    rewardedHintAdState: RewardedAdUiState,
    onWatchHintAd: () -> Unit,
    onUseHintToken: () -> Unit,
    redoState: RedoUiState,
    canUndo: Boolean,
    onRedo: () -> Unit,
    rewardedRedoAdState: RewardedAdUiState,
    onWatchRedoAd: () -> Unit,
    onUseRedoToken: () -> Unit,
    rewatchState: RewatchUiState,
    onRewatchFree: () -> Unit,
    rewatchAdState: RewardedAdUiState,
    onWatchRewatchAd: () -> Unit,
    onUseRewatchToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAssistInfo by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < CompactBreakpoint
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        ) {
            RedoButton(
                remaining = redoState.remaining,
                canUndo = canUndo,
                onClick = onRedo,
                adState = rewardedRedoAdState,
                onWatchAd = onWatchRedoAd,
                compact = compact,
                maxRewardedRedos = redoState.maxRewardedRedos,
                rewardedRemaining = redoState.rewardedRemaining,
                hasInventoryToken = redoState.canUseInventoryToken,
                inventoryTokenCount = redoState.inventoryTokenCount,
                isRedeemingToken = redoState.isRedeemingToken,
                onUseToken = onUseRedoToken,
                modifier = Modifier.staggeredReveal(0),
            )
            RewatchButton(
                remaining = rewatchState.remaining,
                onClick = onRewatchFree,
                adState = rewatchAdState,
                onWatchAd = onWatchRewatchAd,
                compact = compact,
                maxRewardedRewatches = rewatchState.maxRewardedRewatches,
                rewardedRemaining = rewatchState.rewardedRemaining,
                hasInventoryToken = rewatchState.canUseInventoryToken,
                inventoryTokenCount = rewatchState.inventoryTokenCount,
                isRedeemingToken = rewatchState.isRedeemingToken,
                onUseToken = onUseRewatchToken,
                modifier = Modifier.staggeredReveal(1),
            )
            HintButton(
                remaining = hintState.remaining,
                isArmed = hintState.isArmed,
                onClick = onToggleHint,
                adState = rewardedHintAdState,
                onWatchAd = onWatchHintAd,
                compact = compact,
                maxRewardedHints = hintState.maxRewardedHints,
                rewardedRemaining = hintState.rewardedRemaining,
                hasInventoryToken = hintState.canUseInventoryToken,
                inventoryTokenCount = hintState.inventoryTokenCount,
                isRedeemingToken = hintState.isRedeemingToken,
                onUseToken = onUseHintToken,
                modifier = Modifier.staggeredReveal(2),
            )
            IconButton(onClick = { showAssistInfo = true }, modifier = Modifier.staggeredReveal(3)) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.gameplay_assist_info_button_description),
                    tint = MemoryArchitectColors.textSecondary,
                )
            }
        }
    }

    if (showAssistInfo) {
        AssistResourcesDialog(
            hintState = hintState,
            redoState = redoState,
            rewatchState = rewatchState,
            onDismiss = { showAssistInfo = false },
        )
    }
}
