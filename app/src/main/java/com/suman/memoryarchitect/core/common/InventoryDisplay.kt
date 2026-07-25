package com.suman.memoryarchitect.core.common

import android.content.Context
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
