package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/** Mock-backend mirror of [com.suman.memoryarchitect.domain.model.LuckySpinState] - see that
 * class's doc for why this state is kept separate from [PlayerProfileDto]. */
@JsonClass(generateAdapter = true)
data class LuckySpinStateDto(
    val lastFreeSpinEpochDay: Long? = null,
    val lastAdSpinEpochDay: Long? = null,
    val adSpinsUsedToday: Int = 0,
    val hasEverSpun: Boolean = false,
)

/** [rewardKind] ("COINS"/"COSMETIC") picks which of [coinsAmount]/([chosenSku], [rarity]) is
 * populated - the client-computed [com.suman.memoryarchitect.domain.progression.LuckySpinEngine]
 * roll, already resolved to one of its two shapes before this request is ever built (see
 * [com.suman.memoryarchitect.data.repository.SpinRequest]'s doc). [source] ("FREE"/"AD"/"TICKET")
 * is [com.suman.memoryarchitect.domain.repository.SpinSource] - which of Lucky Spin's three
 * allowances this spin spends. */
@JsonClass(generateAdapter = true)
data class SpinLuckySpinRequestDto(
    val rewardKind: String,
    val coinsAmount: Long? = null,
    val chosenSku: String? = null,
    val rarity: String? = null,
    val spinNonce: String,
    val source: String,
    /** Caller-computed "today" - see [com.suman.memoryarchitect.data.repository.ShopRemoteSource.spin]'s
     * doc for why the mock-backend dev server needs this passed in rather than deriving it itself. */
    val todayEpochDay: Long,
)

@JsonClass(generateAdapter = true)
data class SpinLuckySpinResponseDto(
    val rewardKind: String,
    val coinsAwarded: Long? = null,
    val awardedSku: String? = null,
    val rarity: String? = null,
    val wasDuplicate: Boolean,
    val coinsRefunded: Long,
    val profile: PlayerProfileDto,
    val cosmetics: CosmeticsStateDto,
    val inventory: InventoryDto = InventoryDto(),
    val luckySpinState: LuckySpinStateDto = LuckySpinStateDto(),
)
