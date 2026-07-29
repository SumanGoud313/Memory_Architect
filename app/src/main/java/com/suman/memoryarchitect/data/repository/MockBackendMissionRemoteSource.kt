package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.data.remote.MissionApi
import com.suman.memoryarchitect.data.remote.dto.ApplyXpBoostRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimCategoryBonusRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ConsumeInventoryItemRequestDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
import com.suman.memoryarchitect.data.remote.dto.OpenMysteryChestRequestDto
import com.suman.memoryarchitect.data.remote.dto.UnlockAllMissionsEarlyRequestDto
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.MysteryChestReward
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps `mock-backend/`'s REST API exactly as [MockBackendProgressionRemoteSource] wraps
 * `ProgressionApi` - the rotation/eligibility validation this class's doc on
 * [MissionRemoteSource] describes lives server-side in `mock-backend/missions.js`, not here; this
 * class is purely request/response mapping. Same dev-only, single-shared-profile caveat as
 * [MockBackendProgressionRemoteSource] - no per-player identity at all.
 */
@Singleton
class MockBackendMissionRemoteSource @Inject constructor(
    private val api: MissionApi,
) : MissionRemoteSource {

    override suspend fun getInventory(): Inventory = api.getInventory().toDomain()

    override suspend fun claimMissionReward(missionId: MissionId, periodKey: Long, progressCount: Int): MissionClaimResult {
        val response = api.claimMissionReward(ClaimMissionRewardRequestDto(missionId.name, periodKey, progressCount))
        return MissionClaimResult(
            missionId = missionId,
            reward = MissionReward(
                coins = response.coinsAwarded,
                xp = response.xpAwarded,
                inventoryGrants = response.inventoryGrants.toInventoryGrants(),
            ),
            profile = response.profile.toDomain(),
            inventory = response.inventory.toDomain(),
        )
    }

    override suspend fun consumeInventoryItem(kind: InventoryItemKind, quantity: Int): Inventory =
        api.consumeInventoryItem(ConsumeInventoryItemRequestDto(kind.name, quantity)).toDomain()

    override suspend fun openMysteryChest(reward: MysteryChestReward): Pair<PlayerProfile, Inventory> {
        val response = api.openMysteryChest(OpenMysteryChestRequestDto(reward.coinsAwarded))
        return response.profile.toDomain() to response.inventory.toDomain()
    }

    override suspend fun applyXpBoost(xpGranted: Long): Pair<PlayerProfile, Inventory> {
        val response = api.applyXpBoost(ApplyXpBoostRequestDto(xpGranted))
        return response.profile.toDomain() to response.inventory.toDomain()
    }

    override suspend fun claimCategoryBonus(period: MissionPeriod, periodKey: Long, coinsAwarded: Long, xpAwarded: Long): MissionCategoryBonusOutcome {
        val response = api.claimCategoryBonus(ClaimCategoryBonusRequestDto(period.name, periodKey, coinsAwarded, xpAwarded))
        return MissionCategoryBonusOutcome(
            reward = MissionReward(
                coins = response.coinsAwarded,
                xp = response.xpAwarded,
                inventoryGrants = response.inventoryGrants.toInventoryGrants(),
            ),
            profile = response.profile.toDomain(),
            inventory = response.inventory.toDomain(),
        )
    }

    override suspend fun unlockAllMissionsEarly(dailyPeriodKey: Long, weeklyPeriodKey: Long, monthlyPeriodKey: Long): MissionRefreshOutcome {
        val response = api.unlockAllMissionsEarly(UnlockAllMissionsEarlyRequestDto(dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey))
        return MissionRefreshOutcome(
            profile = response.profile.toDomain(),
            refreshState = MissionRefreshState(
                dailyForcedPeriodKey = response.refreshState.dailyForcedPeriodKey,
                weeklyForcedPeriodKey = response.refreshState.weeklyForcedPeriodKey,
                monthlyForcedPeriodKey = response.refreshState.monthlyForcedPeriodKey,
            ),
        )
    }

    private fun com.suman.memoryarchitect.data.remote.dto.PlayerProfileDto.toDomain(): PlayerProfile = PlayerProfile(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        streakShields = streakShields,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        journeyPoints = journeyPoints,
    )

    private fun InventoryDto.toDomain(): Inventory = Inventory(quantities.toInventoryGrants())

    private fun Map<String, Int>.toInventoryGrants(): Map<InventoryItemKind, Int> =
        mapNotNull { (key, value) -> runCatching { InventoryItemKind.valueOf(key) }.getOrNull()?.let { it to value } }.toMap()
}
