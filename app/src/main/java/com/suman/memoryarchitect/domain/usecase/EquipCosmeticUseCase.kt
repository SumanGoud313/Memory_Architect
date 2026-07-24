package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class EquipCosmeticUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(category: CosmeticCategory, id: CosmeticId): Outcome<Unit> =
        repository.equip(category, id)
}
