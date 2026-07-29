package com.suman.memoryarchitect.core.common

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.InventoryItemKind

fun InventoryItemKind.toDisplayTitle(context: Context): String = when (this) {
    InventoryItemKind.HINT_TOKEN -> context.getString(R.string.inventory_hint_token)
    InventoryItemKind.REDO_TOKEN -> context.getString(R.string.inventory_redo_token)
    InventoryItemKind.REWATCH_TICKET -> context.getString(R.string.inventory_rewatch_ticket)
    InventoryItemKind.LUCKY_SPIN_TICKET -> context.getString(R.string.inventory_lucky_spin_ticket)
    InventoryItemKind.XP_BOOST -> context.getString(R.string.inventory_xp_boost)
    InventoryItemKind.DISCOUNT_COUPON -> context.getString(R.string.inventory_discount_coupon)
    InventoryItemKind.MYSTERY_CHEST -> context.getString(R.string.inventory_mystery_chest)
}

/** Shared with [com.suman.memoryarchitect.ui.screens.shop.InventoryRow] so an item's icon looks
 * the same whether it's shown in the Inventory tab or as a mission/reward preview. */
fun InventoryItemKind.toIcon(): ImageVector = when (this) {
    InventoryItemKind.HINT_TOKEN -> Icons.Filled.Lightbulb
    InventoryItemKind.REDO_TOKEN -> Icons.Filled.Replay
    InventoryItemKind.REWATCH_TICKET -> Icons.Filled.Visibility
    InventoryItemKind.LUCKY_SPIN_TICKET -> Icons.Filled.Casino
    InventoryItemKind.XP_BOOST -> Icons.Filled.Bolt
    InventoryItemKind.DISCOUNT_COUPON -> Icons.Filled.Sell
    InventoryItemKind.MYSTERY_CHEST -> Icons.Filled.Inventory2
}
