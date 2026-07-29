package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.SpinResult
import com.suman.memoryarchitect.domain.repository.ShopRepository
import com.suman.memoryarchitect.domain.repository.SpinSource
import java.util.UUID
import javax.inject.Inject

class SpinLuckySpinUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(source: SpinSource): Outcome<SpinResult> =
        repository.spin(UUID.randomUUID().toString(), source)
}
