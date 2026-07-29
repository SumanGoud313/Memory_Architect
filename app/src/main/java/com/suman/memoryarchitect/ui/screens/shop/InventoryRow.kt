package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.core.common.toIcon
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** One row in the Inventory tab - shown for every [InventoryItemKind], even at zero, so the
 * catalog of what's earnable is always visible (same "the full set is the point" reasoning
 * [com.suman.memoryarchitect.ui.screens.shop.CollectionsScreen] already uses for cosmetics).
 * [onOpenMysteryChest]/[onApplyXpBoost] and their `isBusy` flags only ever render a button for
 * their own matching [InventoryItemKind] - every other kind's spend path (Hint/Redo/Rewatch
 * tokens, Discount Coupons, Lucky Spin Tickets) is consumed from where it's actually useful
 * (gameplay, a purchase, a spin), not from this list. */
@Composable
fun InventoryRow(
    kind: InventoryItemKind,
    quantity: Int,
    modifier: Modifier = Modifier,
    onOpenMysteryChest: () -> Unit = {},
    isOpeningChest: Boolean = false,
    onApplyXpBoost: () -> Unit = {},
    isApplyingXpBoost: Boolean = false,
) {
    val context = LocalContext.current
    val title = kind.toDisplayTitle(context)
    val isEmpty = quantity <= 0

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).graphicsLayer { alpha = if (isEmpty) 0.55f else 1f },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MemoryArchitectColors.glassFill, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = kind.toIcon(), contentDescription = null, tint = MemoryArchitectColors.textSecondary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(start = 14.dp).weight(1f),
            )
            Text(
                text = "×$quantity",
                style = MaterialTheme.typography.titleMedium,
                color = MemoryArchitectColors.accentGold,
                modifier = Modifier.padding(end = if (!isEmpty && kind == InventoryItemKind.MYSTERY_CHEST || !isEmpty && kind == InventoryItemKind.XP_BOOST) 12.dp else 0.dp),
            )
            if (!isEmpty && kind == InventoryItemKind.MYSTERY_CHEST) {
                if (isOpeningChest) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MemoryArchitectColors.accentGold)
                } else {
                    OutlineButton(text = stringResource(R.string.inventory_open_chest), onClick = onOpenMysteryChest, horizontalPadding = 14.dp)
                }
            }
            if (!isEmpty && kind == InventoryItemKind.XP_BOOST) {
                if (isApplyingXpBoost) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MemoryArchitectColors.accentGold)
                } else {
                    OutlineButton(text = stringResource(R.string.inventory_use_xp_boost), onClick = onApplyXpBoost, horizontalPadding = 14.dp)
                }
            }
        }
    }
}
