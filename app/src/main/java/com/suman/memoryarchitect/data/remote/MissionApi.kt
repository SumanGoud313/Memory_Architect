package com.suman.memoryarchitect.data.remote

import com.suman.memoryarchitect.data.remote.dto.ApplyXpBoostRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimCategoryBonusRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimCategoryBonusResponseDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardResponseDto
import com.suman.memoryarchitect.data.remote.dto.ConsumeInventoryItemRequestDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
import com.suman.memoryarchitect.data.remote.dto.InventoryEconomyResponseDto
import com.suman.memoryarchitect.data.remote.dto.OpenMysteryChestRequestDto
import com.suman.memoryarchitect.data.remote.dto.UnlockAllMissionsEarlyRequestDto
import com.suman.memoryarchitect.data.remote.dto.UnlockAllMissionsEarlyResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MissionApi {
    @GET("v1/inventory")
    suspend fun getInventory(): InventoryDto

    @POST("v1/missions/claim")
    suspend fun claimMissionReward(@Body body: ClaimMissionRewardRequestDto): ClaimMissionRewardResponseDto

    @POST("v1/inventory/consume")
    suspend fun consumeInventoryItem(@Body body: ConsumeInventoryItemRequestDto): InventoryDto

    @POST("v1/inventory/mystery-chest")
    suspend fun openMysteryChest(@Body body: OpenMysteryChestRequestDto): InventoryEconomyResponseDto

    @POST("v1/inventory/xp-boost")
    suspend fun applyXpBoost(@Body body: ApplyXpBoostRequestDto): InventoryEconomyResponseDto

    @POST("v1/missions/claimCategoryBonus")
    suspend fun claimCategoryBonus(@Body body: ClaimCategoryBonusRequestDto): ClaimCategoryBonusResponseDto

    @POST("v1/missions/unlockAllEarly")
    suspend fun unlockAllMissionsEarly(@Body body: UnlockAllMissionsEarlyRequestDto): UnlockAllMissionsEarlyResponseDto
}
