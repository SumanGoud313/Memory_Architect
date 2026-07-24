package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PurchaseResult
import com.suman.memoryarchitect.domain.repository.ShopRepository
import java.util.UUID
import javax.inject.Inject

class PurchaseCosmeticUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(id: CosmeticId): Outcome<PurchaseResult> =
        repository.purchase(id, UUID.randomUUID().toString())
}
