package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/** Mock-backend mirror of [com.suman.memoryarchitect.domain.model.MysteryChestAdState] - see that
 * class's doc for why this state is kept separate from [LuckySpinStateDto]. */
@JsonClass(generateAdapter = true)
data class MysteryChestAdStateDto(
    val lastClaimEpochDay: Long? = null,
    val claimsUsedToday: Int = 0,
)

/** [todayEpochDay] is caller-computed, same convention [SpinLuckySpinRequestDto] already uses. */
@JsonClass(generateAdapter = true)
data class ClaimAdMysteryChestRequestDto(
    val claimNonce: String,
    val todayEpochDay: Long,
)

@JsonClass(generateAdapter = true)
data class ClaimAdMysteryChestResponseDto(
    val inventory: InventoryDto = InventoryDto(),
    val mysteryChestAdState: MysteryChestAdStateDto = MysteryChestAdStateDto(),
)
