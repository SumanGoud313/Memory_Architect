package com.suman.memoryarchitect.ui.screens.modeselect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.remainingHms
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.core.common.toSubtitleRes
import com.suman.memoryarchitect.core.common.winRewardCoins
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay

fun GameMode.toComposeIcon(): ImageVector = when (this) {
    GameMode.CLASSIC -> Icons.Filled.EmojiEvents
    GameMode.DAILY_CHALLENGE -> Icons.Filled.Today
    GameMode.PRACTICE -> Icons.Filled.SelfImprovement
    GameMode.WEEKLY_CHALLENGE -> Icons.Filled.CalendarMonth
}

fun GameMode.toAccentColor(): Color = when (this) {
    GameMode.CLASSIC -> MemoryArchitectColors.accentTerracotta
    GameMode.DAILY_CHALLENGE -> MemoryArchitectColors.accentAmber
    GameMode.PRACTICE -> MemoryArchitectColors.accentSage
    GameMode.WEEKLY_CHALLENGE -> MemoryArchitectColors.accentGold
}

/** [unlockAtEpochSecond] non-null and still in the future means this mode was just won (Daily
 * or Weekly Challenge only) and is serving its post-win cooldown - see
 * [com.suman.memoryarchitect.core.common.challengeLockDurationSeconds]. The card renders dimmed
 * and unclickable with a live HH:MM:SS countdown in place of the usual subtitle/reward line. */
@Composable
fun ModeCard(
    mode: GameMode,
    onSelected: (GameMode, Color) -> Unit,
    modifier: Modifier = Modifier,
    unlockAtEpochSecond: Long? = null,
) {
    val context = LocalContext.current
    val tick = rememberHapticsTick()
    val accent = mode.toAccentColor()
    val interactionSource = remember { MutableInteractionSource() }

    var nowEpochSecond by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(unlockAtEpochSecond) {
        while (unlockAtEpochSecond != null && nowEpochSecond < unlockAtEpochSecond) {
            nowEpochSecond = System.currentTimeMillis() / 1000
            delay(1_000)
        }
    }
    val locked = unlockAtEpochSecond != null && nowEpochSecond < unlockAtEpochSecond

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (locked) 0.55f else 1f)
            .pressableScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, enabled = !locked) {
                tick()
                onSelected(mode, accent)
            },
        tint = accent.copy(alpha = 0.12f),
    ) {
        // fillMaxSize (rather than wrap-content) so the row's content stays vertically centered
        // now that the card is taller than its intrinsic content — GlassCard's Box defaults to
        // top-start alignment, which would otherwise pin everything to the top and leave dead
        // space below once heightIn(min = 128.dp) makes the card taller than the row needs.
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                // Soft glow wash behind the badge, echoing the same technique used for the
                // premium drag ghost's glow ring in Gameplay.
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .blur(14.dp)
                        .background(accent.copy(alpha = 0.45f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .then(if (locked) Modifier else Modifier.idlePulse(minScale = 0.98f, maxScale = 1.03f, periodMs = 2200))
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (locked) Icons.Filled.Lock else mode.toComposeIcon(),
                        contentDescription = null,
                        tint = MemoryArchitectColors.bgBase,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = mode.toDisplayTitle(context),
                    style = MaterialTheme.typography.titleMedium,
                    color = MemoryArchitectColors.textPrimary,
                )
                if (locked) {
                    Text(
                        text = stringResource(R.string.mode_locked_unlocks_in, remainingHms(unlockAtEpochSecond, nowEpochSecond)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    Text(
                        text = stringResource(mode.toSubtitleRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MemoryArchitectColors.textSecondary,
                    )
                    mode.winRewardCoins()?.let { coins ->
                        Text(
                            text = stringResource(R.string.mode_win_reward, coins),
                            style = MaterialTheme.typography.labelMedium,
                            color = MemoryArchitectColors.accentGold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(26.dp)
                    .background(MemoryArchitectColors.glassFill, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
