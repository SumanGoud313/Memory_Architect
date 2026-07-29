package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

/** [period] is [com.suman.memoryarchitect.domain.model.MissionPeriod.name] ("DAILY"/"WEEKLY"/"MONTHLY").
 * [coinsAwarded]/[xpAwarded] are the client-side [com.suman.memoryarchitect.domain.progression.MissionCategoryBonusCatalog.roll]
 * result - re-verified for plausibility server-side ([coinsAwarded] falls within that same
 * catalog's configured range for [period], [xpAwarded] must equal its configured fixed xp), never
 * trusted outright. */
@JsonClass(generateAdapter = true)
data class ClaimCategoryBonusRequestDto(
    val period: String,
    val periodKey: Long,
    val coinsAwarded: Long,
    val xpAwarded: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ClaimCategoryBonusResponseDto(
    val coinsAwarded: Long,
    val xpAwarded: Long,
    val inventoryGrants: Map<String, Int> = emptyMap(),
    val profile: PlayerProfileDto,
    val inventory: InventoryDto,
)

/** [dailyPeriodKey]/[weeklyPeriodKey]/[monthlyPeriodKey] are each period's current *effective*
 * periodKey (see [com.suman.memoryarchitect.domain.progression.MissionCatalog.effectivePeriodKey]) -
 * what the server re-derives eligibility and the "not the same as last" reroll against. */
@JsonClass(generateAdapter = true)
data class UnlockAllMissionsEarlyRequestDto(
    val dailyPeriodKey: Long,
    val weeklyPeriodKey: Long,
    val monthlyPeriodKey: Long,
)

@JsonClass(generateAdapter = true)
data class MissionRefreshStateDto(
    val dailyForcedPeriodKey: Long? = null,
    val weeklyForcedPeriodKey: Long? = null,
    val monthlyForcedPeriodKey: Long? = null,
)

@JsonClass(generateAdapter = true)
data class UnlockAllMissionsEarlyResponseDto(
    val profile: PlayerProfileDto,
    val refreshState: MissionRefreshStateDto,
)
