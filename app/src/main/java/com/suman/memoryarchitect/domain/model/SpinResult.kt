package com.suman.memoryarchitect.domain.model

/** What one Lucky Spin actually paid out - see [com.suman.memoryarchitect.domain.progression.SpinRules]
 * for the odds table deciding which branch a spin resolves to. Coins is the majority outcome by
 * design (a spin costs nothing now - see [SpinResult]'s own doc); Cosmetic is the minority "big
 * win" outcome, and the only one requiring server-side ownership re-verification. */
sealed interface SpinRewardKind {
    data class Coins(val amount: Long) : SpinRewardKind
    data class Cosmetic(val id: CosmeticId, val rarity: CosmeticRarity) : SpinRewardKind
}

/** [wasDuplicate]/[coinsRefunded] only ever apply to a [SpinRewardKind.Cosmetic] reward - see
 * [com.suman.memoryarchitect.domain.progression.SpinRules]'s "always feels like a win" fairness
 * mechanic. Both stay at their zero/false defaults for a [SpinRewardKind.Coins] reward, which has
 * no notion of "already owned." */
data class SpinResult(
    val reward: SpinRewardKind,
    val wasDuplicate: Boolean = false,
    val coinsRefunded: Long = 0L,
    val updatedProfile: PlayerProfile,
    val updatedSpinState: LuckySpinState,
)
