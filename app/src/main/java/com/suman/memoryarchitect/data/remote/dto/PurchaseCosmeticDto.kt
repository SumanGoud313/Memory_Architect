package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PurchaseCosmeticRequestDto(
    val sku: String,
    val purchaseNonce: String,
)

@JsonClass(generateAdapter = true)
data class PurchaseCosmeticResponseDto(
    val purchasedSku: String,
    val profile: PlayerProfileDto,
    val cosmetics: CosmeticsStateDto,
)
