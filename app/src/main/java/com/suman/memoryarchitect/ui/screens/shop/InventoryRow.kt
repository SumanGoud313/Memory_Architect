package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private fun InventoryItemKind.toIcon(): ImageVector = when (this) {
    InventoryItemKind.HINT_TOKEN -> Icons.Filled.Lightbulb
    InventoryItemKind.REDO_TOKEN -> Icons.Filled.Replay
    InventoryItemKind.REWATCH_TICKET -> Icons.Filled.Visibility
    InventoryItemKind.LUCKY_SPIN_TICKET -> Icons.Filled.Casino
    InventoryItemKind.XP_BOOST -> Icons.Filled.Bolt
    InventoryItemKind.DISCOUNT_COUPON -> Icons.Filled.Sell
    InventoryItemKind.MYSTERY_CHEST -> Icons.Filled.Inventory2
}

/** One row in the Inventory tab - shown for every [InventoryItemKind], even at zero, so the
 * catalog of what's earnable is always visible (same "the full set is the point" reasoning
 * [com.suman.memoryarchitect.ui.screens.shop.CollectionsScreen] already uses for cosmetics). */
@Composable
fun InventoryRow(kind: InventoryItemKind, quantity: Int, modifier: Modifier = Modifier) {
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
            )
        }
    }
}
