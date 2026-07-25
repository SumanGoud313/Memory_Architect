package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/** [missionId] is [com.suman.memoryarchitect.domain.model.MissionId.name]. [progressCount] is the
 * client's own locally-tracked count at claim time - see [com.suman.memoryarchitect.data.repository.MissionRemoteSource]'s
 * doc for why the server trusts but independently bounds this rather than rejecting it outright. */
@JsonClass(generateAdapter = true)
data class ClaimMissionRewardRequestDto(
    val missionId: String,
    val periodKey: Long,
    val progressCount: Int,
)

@JsonClass(generateAdapter = true)
data class ClaimMissionRewardResponseDto(
    val missionId: String,
    val coinsAwarded: Long,
    val xpAwarded: Long,
    val inventoryGrants: Map<String, Int> = emptyMap(),
    val profile: PlayerProfileDto,
    val inventory: InventoryDto,
)

@JsonClass(generateAdapter = true)
data class ConsumeInventoryItemRequestDto(
    val kind: String,
    val quantity: Int,
)
