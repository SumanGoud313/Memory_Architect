package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/** [chosenSku]/[rarity] are the client-computed [com.suman.memoryarchitect.domain.progression.LuckySpinEngine]
 * roll - see [com.suman.memoryarchitect.domain.repository.ShopRepository]'s doc for why the roll
 * itself is client-trusted while only the resulting ownership transition is re-verified
 * server-side. */
@JsonClass(generateAdapter = true)
data class SpinLuckySpinRequestDto(
    val chosenSku: String,
    val rarity: String,
    val spinNonce: String,
)

@JsonClass(generateAdapter = true)
data class SpinLuckySpinResponseDto(
    val awardedSku: String,
    val rarity: String,
    val wasDuplicate: Boolean,
    val coinsRefunded: Long,
    val profile: PlayerProfileDto,
    val cosmetics: CosmeticsStateDto,
)
