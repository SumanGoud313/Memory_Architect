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
 * while [remaining] &gt; 0 the badge shows [remaining] plus [inventoryTokenCount] combined
 * (a player with 1 free hint and 3 owned Hint Tokens sees "4" - their true total available, not
 * just whichever pool a tap would draw from first), the owned token count alone (in a distinct
 * sage tint) once the free budget is spent and [hasInventoryToken] is still true - signalling the
 * next tap redeems one instantly, no ad - "Ad" once both are exhausted and a rewarded ad can
 * still grant more, a small spinner while either that ad or a token redemption is actually in
 * flight, or - once [isExhausted] - a muted dash, since there is nothing left tapping could do.
 * This is the one deliberate design choice behind the whole merged-button approach - a player
 * should never have to relearn what a control looks like just because they ran out of free uses;
 * [isExhausted] is the one state where the control genuinely does go inert, and looks it.
 */
@Composable
fun AssistIconBadge(
    icon: ImageVector,
    iconTint: Color,
    remaining: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isExhausted: Boolean = false,
    hasInventoryToken: Boolean = false,
    inventoryTokenCount: Int = 0,
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
                    color = when {
                        remaining > 0 -> MemoryArchitectColors.bgBase
                        hasInventoryToken -> MemoryArchitectColors.accentSage
                        isExhausted -> MemoryArchitectColors.textTertiary.copy(alpha = 0.35f)
                        else -> MemoryArchitectColors.accentGold
                    },
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
                    text = when {
                        remaining > 0 -> (remaining + inventoryTokenCount).toString()
                        hasInventoryToken -> inventoryTokenCount.toString()
                        isExhausted -> stringResource(R.string.gameplay_assist_exhausted_badge)
                        else -> stringResource(R.string.gameplay_assist_ad_badge)
                    },
                    color = if (remaining > 0) MemoryArchitectColors.textPrimary else MemoryArchitectColors.bgBase,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
