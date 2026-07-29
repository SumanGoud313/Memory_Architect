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

/** [coinsAwarded] is client-rolled (see [com.suman.memoryarchitect.domain.progression.MysteryChestOdds]'s
 * doc for why the server doesn't need to independently re-derive it). */
@JsonClass(generateAdapter = true)
data class OpenMysteryChestRequestDto(
    val coinsAwarded: Long,
)

/** [xpAwarded] is always [com.suman.memoryarchitect.domain.progression.XpBoostRules.xpGrantedPerBoost] -
 * sent rather than hardcoded server-side purely so the mock-backend/Firestore write shapes match
 * [ClaimMissionRewardResponseDto]'s existing "client states the amount, server applies it" style. */
@JsonClass(generateAdapter = true)
data class ApplyXpBoostRequestDto(
    val xpAwarded: Long,
)

@JsonClass(generateAdapter = true)
data class InventoryEconomyResponseDto(
    val profile: PlayerProfileDto,
    val inventory: InventoryDto,
)
