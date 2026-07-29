package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClaimReturningPlayerGiftRequestDto(val claimedOnEpochDay: Long)

@JsonClass(generateAdapter = true)
data class ClaimReturningPlayerGiftResponseDto(
    val inventory: InventoryDto = InventoryDto(),
)
