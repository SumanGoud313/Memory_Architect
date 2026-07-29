package com.suman.memoryarchitect.ui.screens.missions

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** "New missions in Xh Ym" (or Xd Yh / Xm), ticking once a minute - [targetEpochSecond] is
 * [com.suman.memoryarchitect.domain.progression.MissionCatalog.nextPeriodStartEpochSecond]'s
 * result, purely a display refresh (the real rotation boundary is already a pure function of the
 * calendar; this composable never itself decides when missions actually rotate). One instance per
 * period section in [MissionsScreen] - Daily/Weekly/Monthly each get their own countdown to their
 * own next rotation. */
@Composable
fun MissionCountdown(targetEpochSecond: Long, modifier: Modifier = Modifier) {
    var remainingSeconds by remember(targetEpochSecond) {
        mutableLongStateOf((targetEpochSecond - System.currentTimeMillis() / 1000L).coerceAtLeast(0L))
    }

    LaunchedEffect(targetEpochSecond) {
        while (isActive) {
            remainingSeconds = (targetEpochSecond - System.currentTimeMillis() / 1000L).coerceAtLeast(0L)
            if (remainingSeconds <= 0L) break
            delay(60_000L)
        }
    }

    val context = LocalContext.current
    val days = remainingSeconds / 86_400L
    val hours = (remainingSeconds % 86_400L) / 3_600L
    val minutes = (remainingSeconds % 3_600L) / 60L
    val text = when {
        days > 0 -> context.getString(R.string.missions_countdown_days_hours, days, hours)
        hours > 0 -> context.getString(R.string.missions_countdown_hours_minutes, hours, minutes)
        else -> context.getString(R.string.missions_countdown_minutes, minutes.coerceAtLeast(1L))
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MemoryArchitectColors.textTertiary,
        modifier = modifier,
    )
}
