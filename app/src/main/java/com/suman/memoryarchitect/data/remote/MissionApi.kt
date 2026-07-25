package com.suman.memoryarchitect.data.remote

import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardRequestDto
import com.suman.memoryarchitect.data.remote.dto.ClaimMissionRewardResponseDto
import com.suman.memoryarchitect.data.remote.dto.ConsumeInventoryItemRequestDto
import com.suman.memoryarchitect.data.remote.dto.InventoryDto
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
}
