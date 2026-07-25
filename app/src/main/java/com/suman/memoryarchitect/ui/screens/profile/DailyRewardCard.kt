package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
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
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.progression.DailyRewardCatalog
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

private enum class DayCellStatus { CLAIMED, CLAIMABLE_TODAY, LOCKED }

private fun dayCellStatus(day: Int, cycleDay: Int, canClaimToday: Boolean): DayCellStatus = when {
    day < cycleDay -> DayCellStatus.CLAIMED
    day == cycleDay && canClaimToday -> DayCellStatus.CLAIMABLE_TODAY
    day == cycleDay -> DayCellStatus.CLAIMED
    else -> DayCellStatus.LOCKED
}

/**
 * A 7-day login-reward calendar + claim button. Every day is shown up front with its exact reward
 * *except* the one Mystery Chest day (see [DailyRewardEntry.isMysteryChest]) - its amount is fixed,
 * never randomized, just hidden until actually claimed for a little anticipation. A missed day
 * never loses anything already banked — it just quietly restarts the row at day 1 next time (see
 * [DailyRewardCatalog.nextCycleDay]) — and there's no countdown urgency copy anywhere: the claim
 * button is simply enabled or not.
 */
@Composable
fun DailyRewardCard(
    status: DailyRewardStatus,
    isClaiming: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), tint = MemoryArchitectColors.accentGold.copy(alpha = 0.08f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = MemoryArchitectColors.accentGold)
                Text(
                    text = stringResource(R.string.profile_daily_reward_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MemoryArchitectColors.textPrimary,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DailyRewardCatalog.entries.forEach { entry ->
                    DayCell(
                        day = entry.day,
                        coins = entry.coins,
                        isMysteryChest = entry.isMysteryChest,
                        bonusShield = entry.bonusShield,
                        cellStatus = dayCellStatus(entry.day, status.cycleDay, status.canClaimToday),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            PrimaryButton(
                text = if (status.canClaimToday) {
                    stringResource(R.string.profile_daily_reward_claim)
                } else {
                    stringResource(R.string.profile_daily_reward_claimed)
                },
                onClick = onClaim,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .graphicsLayer { alpha = if (status.canClaimToday && !isClaiming) 1f else 0.5f }
                    .then(if (status.canClaimToday && !isClaiming) Modifier.idlePulse(minScale = 0.99f, maxScale = 1.01f, periodMs = 2200) else Modifier),
            )
            if (isClaiming) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentGold, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    coins: Long,
    isMysteryChest: Boolean,
    bonusShield: Boolean,
    cellStatus: DayCellStatus,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(MemoryArchitectRadii.chip)
    // The Mystery Chest day never reveals its amount ahead of a real claim - only once it's
    // actually CLAIMED (a past day, already resolved via the claim celebration) does the number
    // mean anything to show here.
    val hideAmount = isMysteryChest && cellStatus != DayCellStatus.CLAIMED
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .then(if (cellStatus == DayCellStatus.CLAIMABLE_TODAY) Modifier.idlePulse(minScale = 0.96f, maxScale = 1.05f, periodMs = 1600) else Modifier)
                .background(
                    when (cellStatus) {
                        DayCellStatus.CLAIMED -> MemoryArchitectColors.accentGold.copy(alpha = 0.22f)
                        DayCellStatus.CLAIMABLE_TODAY -> MemoryArchitectColors.accentGold
                        DayCellStatus.LOCKED -> MemoryArchitectColors.glassFill
                    },
                    shape,
                )
                .graphicsLayer { alpha = if (cellStatus == DayCellStatus.LOCKED) 0.55f else 1f },
            contentAlignment = Alignment.Center,
        ) {
            when {
                cellStatus == DayCellStatus.CLAIMED -> Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MemoryArchitectColors.accentGold,
                    modifier = Modifier.size(16.dp),
                )
                cellStatus == DayCellStatus.CLAIMABLE_TODAY && hideAmount -> Icon(
                    Icons.Filled.CardGiftcard,
                    contentDescription = null,
                    tint = MemoryArchitectColors.bgBase,
                    modifier = Modifier.size(18.dp),
                )
                cellStatus == DayCellStatus.CLAIMABLE_TODAY -> Icon(
                    Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = MemoryArchitectColors.bgBase,
                    modifier = Modifier.size(18.dp),
                )
                hideAmount -> Icon(
                    Icons.Filled.QuestionMark,
                    contentDescription = null,
                    tint = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                else -> Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (bonusShield) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = stringResource(R.string.profile_daily_reward_bonus_shield_description),
                    tint = MemoryArchitectColors.accentSage,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(11.dp),
                )
            }
        }
        Text(
            text = if (hideAmount) "?" else "$coins",
            style = MaterialTheme.typography.labelMedium,
            color = if (cellStatus == DayCellStatus.LOCKED) MemoryArchitectColors.textTertiary else MemoryArchitectColors.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
