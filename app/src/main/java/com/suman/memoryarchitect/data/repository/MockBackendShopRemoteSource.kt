package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.data.remote.ShopApi
import com.suman.memoryarchitect.data.remote.dto.EquipCosmeticRequestDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.data.remote.dto.PurchaseCosmeticRequestDto
import com.suman.memoryarchitect.data.remote.dto.SpinLuckySpinRequestDto
import com.suman.memoryarchitect.data.remote.dto.UnequipCosmeticRequestDto
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.PlayerProfile
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps `mock-backend/`'s REST API - same dev-only, single-shared-state caveat as
 * [MockBackendProgressionRemoteSource]. */
@Singleton
class MockBackendShopRemoteSource @Inject constructor(
    private val api: ShopApi,
) : ShopRemoteSource {

    override suspend fun getOwnedSkus(): Set<String> = api.getCosmeticsState().ownedSkus.toSet()

    override suspend fun getEquipped(): Map<String, String> = api.getCosmeticsState().equipped

    override suspend fun purchase(id: CosmeticId, purchaseNonce: String): Pair<PlayerProfile, Set<String>> {
        val response = api.purchase(PurchaseCosmeticRequestDto(id.name, purchaseNonce))
        return response.profile.toDomain() to response.cosmetics.ownedSkus.toSet()
    }

    override suspend fun spin(chosenId: CosmeticId, rarity: CosmeticRarity, spinNonce: String): SpinOutcome {
        val response = api.spin(SpinLuckySpinRequestDto(chosenId.name, rarity.name, spinNonce))
        return SpinOutcome(
            awardedId = CosmeticId.valueOf(response.awardedSku),
            wasDuplicate = response.wasDuplicate,
            coinsRefunded = response.coinsRefunded,
            profile = response.profile.toDomain(),
            ownedSkus = response.cosmetics.ownedSkus.toSet(),
        )
    }

    override suspend fun equip(category: CosmeticCategory, id: CosmeticId): Map<String, String> =
        api.equip(EquipCosmeticRequestDto(category.name, id.name)).equipped

    override suspend fun unequip(category: CosmeticCategory): Map<String, String> =
        api.unequip(UnequipCosmeticRequestDto(category.name)).equipped

    private fun PlayerProfileDto.toDomain() = PlayerProfile(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
    )
}
