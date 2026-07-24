package com.suman.memoryarchitect.domain.model

/** [wasDuplicate]/[coinsRefunded] together describe the Lucky Spin "always feels like a win"
 * fairness mechanic - see [com.suman.memoryarchitect.domain.progression.SpinRules]. */
data class SpinResult(
    val awardedId: CosmeticId,
    val rarity: CosmeticRarity,
    val wasDuplicate: Boolean,
    val coinsRefunded: Long,
    val updatedProfile: PlayerProfile,
)
