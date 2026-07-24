package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * 0-3 star rating row for level-complete results, each earned star popping in with a
 * per-index [staggeredReveal] delay so they light up left-to-right rather than all at once.
 */
@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, total: Int = 3) {
    Row(modifier = modifier) {
        repeat(total) { index ->
            val earned = index < stars
            Icon(
                imageVector = if (earned) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (earned) MemoryArchitectColors.accentGold else MemoryArchitectColors.textTertiary,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(36.dp)
                    .staggeredReveal(index = index, baseDelayMs = 300L, staggerMs = 150L),
            )
        }
    }
}
