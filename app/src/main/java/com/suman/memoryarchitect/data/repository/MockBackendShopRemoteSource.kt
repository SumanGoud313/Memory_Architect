package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.data.remote.ShopApi
import com.suman.memoryarchitect.data.remote.dto.ClaimAdMysteryChestRequestDto
import com.suman.memoryarchitect.data.remote.dto.EquipCosmeticRequestDto
import com.suman.memoryarchitect.data.remote.dto.LuckySpinStateDto
import com.suman.memoryarchitect.data.remote.dto.MysteryChestAdStateDto
import com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto
import com.suman.memoryarchitect.data.remote.dto.PurchaseCosmeticRequestDto
import com.suman.memoryarchitect.data.remote.dto.SpinLuckySpinRequestDto
import com.suman.memoryarchitect.data.remote.dto.UnequipCosmeticRequestDto
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.model.MysteryChestAdState
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import com.suman.memoryarchitect.domain.repository.SpinSource
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

    override suspend fun getLuckySpinState(): LuckySpinState = api.getLuckySpinState().toDomain()

    override suspend fun purchase(id: CosmeticId, purchaseNonce: String, useDiscountCoupon: Boolean): Pair<PlayerProfile, Set<String>> {
        val response = api.purchase(PurchaseCosmeticRequestDto(id.name, purchaseNonce, useDiscountCoupon))
        return response.profile.toDomain() to response.cosmetics.ownedSkus.toSet()
    }

    override suspend fun spin(request: SpinRequest, spinNonce: String, source: SpinSource, todayEpochDay: Long): SpinOutcome {
        val requestDto = when (request) {
            is SpinRequest.Coins -> SpinLuckySpinRequestDto(
                rewardKind = "COINS", coinsAmount = request.amount, spinNonce = spinNonce, source = source.name, todayEpochDay = todayEpochDay,
            )
            is SpinRequest.Cosmetic -> SpinLuckySpinRequestDto(
                rewardKind = "COSMETIC", chosenSku = request.id.name, rarity = request.rarity.name, spinNonce = spinNonce, source = source.name, todayEpochDay = todayEpochDay,
            )
        }
        val response = api.spin(requestDto)
        val reward = if (response.rewardKind == "COINS") {
            SpinRewardKind.Coins(response.coinsAwarded ?: 0L)
        } else {
            SpinRewardKind.Cosmetic(CosmeticId.valueOf(requireNotNull(response.awardedSku)), CosmeticRarity.valueOf(requireNotNull(response.rarity)))
        }
        return SpinOutcome(
            reward = reward,
            wasDuplicate = response.wasDuplicate,
            coinsRefunded = response.coinsRefunded,
            profile = response.profile.toDomain(),
            ownedSkus = response.cosmetics.ownedSkus.toSet(),
            spinState = response.luckySpinState.toDomain(),
        )
    }

    override suspend fun equip(category: CosmeticCategory, id: CosmeticId): Map<String, String> =
        api.equip(EquipCosmeticRequestDto(category.name, id.name)).equipped

    override suspend fun unequip(category: CosmeticCategory): Map<String, String> =
        api.unequip(UnequipCosmeticRequestDto(category.name)).equipped

    override suspend fun getMysteryChestAdState(): MysteryChestAdState = api.getMysteryChestAdState().toDomain()

    override suspend fun claimAdMysteryChest(claimNonce: String, todayEpochDay: Long): MysteryChestAdClaimOutcome {
        val response = api.claimAdMysteryChest(ClaimAdMysteryChestRequestDto(claimNonce, todayEpochDay))
        val quantities = response.inventory.quantities.mapNotNull { (key, value) ->
            runCatching { InventoryItemKind.valueOf(key) }.getOrNull()?.let { it to value }
        }.toMap()
        return MysteryChestAdClaimOutcome(quantities, response.mysteryChestAdState.toDomain())
    }

    private fun LuckySpinStateDto.toDomain() = LuckySpinState(
        lastFreeSpinEpochDay = lastFreeSpinEpochDay,
        lastAdSpinEpochDay = lastAdSpinEpochDay,
        adSpinsUsedToday = adSpinsUsedToday,
        hasEverSpun = hasEverSpun,
    )

    private fun MysteryChestAdStateDto.toDomain() = MysteryChestAdState(
        lastClaimEpochDay = lastClaimEpochDay,
        claimsUsedToday = claimsUsedToday,
    )

    private fun PlayerProfileDto.toDomain() = PlayerProfile(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        streakShields = streakShields,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        // Previously omitted - see FirestoreShopRemoteSource.toProfile's identical fix and doc.
        journeyPoints = journeyPoints,
    )
}
