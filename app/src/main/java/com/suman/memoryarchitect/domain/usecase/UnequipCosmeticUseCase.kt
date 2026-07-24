package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class UnequipCosmeticUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(category: CosmeticCategory): Outcome<Unit> = repository.unequip(category)
}
