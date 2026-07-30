package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.MysteryChestAdClaimResult
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ShopRepository
import java.util.UUID
import javax.inject.Inject

class ClaimAdMysteryChestUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    suspend operator fun invoke(): Outcome<MysteryChestAdClaimResult> =
        repository.claimAdMysteryChest(UUID.randomUUID().toString())
}
