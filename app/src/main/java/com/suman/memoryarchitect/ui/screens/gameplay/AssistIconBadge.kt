package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Shared icon + corner badge for the Hint/Redo/Rewatch assist buttons (see [HintButton]/
 * [RedoButton]/[RewatchButton]). [icon]/[iconTint] never change based on [remaining] - the
 * button's identity stays the same at all times. Only the badge in the icon's corner changes:
 * the remaining count while [remaining] &gt; 0, "Ad" once it hits zero (signalling that tapping
 * now offers a rewarded ad instead), or a small spinner while that ad is actually loading. This
 * is the one deliberate design choice behind the whole merged-button approach - a player should
 * never have to relearn what a control looks like just because they ran out of free uses.
 */
@Composable
fun AssistIconBadge(
    icon: ImageVector,
    iconTint: Color,
    remaining: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                .background(
                    color = if (remaining > 0) MemoryArchitectColors.bgBase else MemoryArchitectColors.accentGold,
                    shape = CircleShape,
                )
                .padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(9.dp),
                    strokeWidth = 1.5.dp,
                    color = iconTint,
                )
            } else {
                Text(
                    text = if (remaining > 0) remaining.toString() else stringResource(R.string.gameplay_assist_ad_badge),
                    color = if (remaining > 0) MemoryArchitectColors.textPrimary else MemoryArchitectColors.bgBase,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
