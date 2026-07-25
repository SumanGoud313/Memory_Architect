package com.suman.memoryarchitect.ui.screens.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LevelProgressBar
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private fun MissionRequirementType.toIcon(): ImageVector = when (this) {
    MissionRequirementType.COMPLETE_LEVELS -> Icons.Filled.Extension
    MissionRequirementType.COMPLETE_PRACTICE_ROUNDS -> Icons.Filled.FitnessCenter
    MissionRequirementType.COMPLETE_DAILY_CHALLENGE -> Icons.Filled.Today
    MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE -> Icons.Filled.CalendarViewWeek
    MissionRequirementType.EARN_COINS -> Icons.Filled.MonetizationOn
    MissionRequirementType.ZERO_HINT_LEVEL_CLEAR -> Icons.Filled.VisibilityOff
    MissionRequirementType.HIGH_ACCURACY_CLEAR -> Icons.Filled.GpsFixed
    MissionRequirementType.EARN_STARS -> Icons.Filled.Star
    MissionRequirementType.UNLOCK_COSMETIC -> Icons.Filled.CardGiftcard
    MissionRequirementType.EQUIP_COSMETIC -> Icons.Filled.Checkroom
    MissionRequirementType.WATCH_REWARDED_AD -> Icons.Filled.PlayCircle
}

/** One row in [MissionsScreen] - a claimed mission stays visible (checkmark, no button) rather
 * than disappearing, so a player can see what they've already finished this period. */
@Composable
fun MissionRow(
    mission: ActiveMission,
    isClaiming: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = mission.definition.id.toDisplayTitle(context)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = if (mission.claimed) MemoryArchitectColors.accentGold.copy(alpha = 0.08f) else null,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (mission.isComplete) MemoryArchitectColors.accentGold else MemoryArchitectColors.glassFill,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (mission.claimed) Icons.Filled.Check else mission.definition.requirement.toIcon(),
                    contentDescription = null,
                    tint = if (mission.isComplete) MemoryArchitectColors.bgBase else MemoryArchitectColors.textSecondary,
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MemoryArchitectColors.textPrimary,
                )
                if (!mission.claimed) {
                    LevelProgressBar(
                        fraction = mission.currentCount.toFloat() / mission.definition.targetCount.toFloat(),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.missions_progress_format,
                            mission.currentCount.coerceAtMost(mission.definition.targetCount),
                            mission.definition.targetCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            when {
                mission.claimed -> Unit
                isClaiming -> CircularProgressIndicator(
                    color = MemoryArchitectColors.accentGold,
                    modifier = Modifier.size(20.dp),
                )
                mission.canClaim -> PrimaryButton(
                    text = stringResource(R.string.missions_claim),
                    onClick = onClaim,
                    modifier = Modifier.padding(start = 12.dp),
                )
                else -> Unit
            }
        }
    }
}
