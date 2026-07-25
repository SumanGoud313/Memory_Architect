package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.data.remote.MissionApi
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ConsumeInventoryItemRequestDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.PlayerProfile
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
        val profile = PlayerProfile(
            xp = response.profile.xp,
            coins = response.profile.coins,
            currentStreak = response.profile.currentStreak,
            longestStreak = response.profile.longestStreak,
            lastPlayedEpochDay = response.profile.lastPlayedEpochDay,
            streakShields = response.profile.streakShields,
            dailyChallengeWonAtEpochSecond = response.profile.dailyChallengeWonAtEpochSecond,
            weeklyChallengeWonAtEpochSecond = response.profile.weeklyChallengeWonAtEpochSecond,
            journeyPoints = response.profile.journeyPoints,
        )
        return MissionClaimResult(
            missionId = missionId,
            reward = MissionReward(
                coins = response.coinsAwarded,
                xp = response.xpAwarded,
                inventoryGrants = response.inventoryGrants.toInventoryGrants(),
            ),
            profile = profile,
            inventory = response.inventory.toDomain(),
        )
    }

    override suspend fun consumeInventoryItem(kind: InventoryItemKind, quantity: Int): Inventory =
        api.consumeInventoryItem(ConsumeInventoryItemRequestDto(kind.name, quantity)).toDomain()

    private fun InventoryDto.toDomain(): Inventory = Inventory(quantities.toInventoryGrants())

    private fun Map<String, Int>.toInventoryGrants(): Map<InventoryItemKind, Int> =
        mapNotNull { (key, value) -> runCatching { InventoryItemKind.valueOf(key) }.getOrNull()?.let { it to value } }.toMap()
}
