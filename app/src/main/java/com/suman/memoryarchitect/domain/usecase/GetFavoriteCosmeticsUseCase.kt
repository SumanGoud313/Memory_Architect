package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class GetFavoriteCosmeticsUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(): Set<CosmeticId> = repository.getFavoriteCosmeticIds()
}
