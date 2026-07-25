package com.suman.memoryarchitect.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Home's one deliberate exception to "just a Play button" (see [HomeScreen]'s doc) - a small,
 * quiet flame + count, shown only once a streak is actually worth flaunting (2+ days; a single
 * day isn't a streak yet). Full detail (longest streak, shields, the milestone timeline) stays on
 * Profile - this is a glance, not a second dashboard. */
@Composable
fun HomeStreakChip(currentStreak: Int, modifier: Modifier = Modifier) {
    if (currentStreak < 2) return
    Row(
        modifier = modifier
            .background(MemoryArchitectColors.glassFill, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = stringResource(R.string.home_streak_chip_description, currentStreak),
            tint = MemoryArchitectColors.accentTerracotta,
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(
            text = "$currentStreak",
            style = MaterialTheme.typography.labelLarge,
            color = MemoryArchitectColors.textPrimary,
        )
    }
}
