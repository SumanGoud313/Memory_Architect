package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.InventoryRepository
import javax.inject.Inject

class ConsumeInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(kind: InventoryItemKind, quantity: Int = 1): Outcome<Inventory> =
        repository.consumeItem(kind, quantity)
}
