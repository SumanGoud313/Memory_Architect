package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.repository.ShopRepository
import javax.inject.Inject

class GetLuckySpinStateUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(): LuckySpinState = repository.getLuckySpinState()
}
