package com.suman.memoryarchitect.domain.model

/** A player's current consumable balances - see [InventoryItemKind]'s doc. Server-authoritative,
 * same trust model as [PlayerProfile]. */
data class Inventory(val quantities: Map<InventoryItemKind, Int> = emptyMap()) {
    fun quantityOf(kind: InventoryItemKind): Int = quantities[kind] ?: 0

    companion object {
        val EMPTY = Inventory()
    }
}
