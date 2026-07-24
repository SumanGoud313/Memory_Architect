package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class ToggleFavoriteCosmeticUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(id: CosmeticId) = repository.toggleFavorite(id)
}
