package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MysteryChestAdState
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class GetMysteryChestAdStateUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(): MysteryChestAdState = repository.getMysteryChestAdState()
}
