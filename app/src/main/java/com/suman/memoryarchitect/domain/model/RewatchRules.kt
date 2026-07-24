package com.suman.memoryarchitect.domain.model

/**
 * Rewatch has no free tier at all, at any level (1-24 or 25-100) - it's reachable only through a
 * rewarded ad (see `GameplayViewModel.watchRewatchAd`). [levelNumber] is accepted only to keep
 * this rule's shape consistent with [HintRules]/[RedoRules]'s call sites; it's never actually
 * consulted.
 */
data class RewatchRules(
    val freeRewatchesPerLevel: Int = 0,
) {
    fun rewatchesAllowedForLevel(levelNumber: Int): Int = freeRewatchesPerLevel

    companion object {
        val Default = RewatchRules()
    }
}
