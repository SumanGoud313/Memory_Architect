package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class GetRecentlyUsedCosmeticsUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(limit: Int = 10): List<CosmeticId> = repository.getRecentlyUsedCosmeticIds(limit)
}
