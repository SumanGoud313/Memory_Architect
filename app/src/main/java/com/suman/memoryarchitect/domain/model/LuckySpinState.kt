package com.suman.memoryarchitect.domain.model

/**
 * Lucky Spin's own daily-gate/first-spin state - deliberately its own small server-authoritative
 * document rather than fields on [PlayerProfile], the same "Shop keeps its own small per-concern
 * documents" convention `playerCosmetics/{uid}` and `inventory/{uid}` already establish (see
 * [com.suman.memoryarchitect.data.repository.FirestoreShopRemoteSource]'s doc) - Progression/
 * Mission never read or write this, so this state can never be silently overwritten back to
 * stale defaults by an unrelated `submitScore`/`claimDailyReward` write landing on the shared
 * `playerProfiles/{uid}` document the way it would if these lived there instead.
 */
data class LuckySpinState(
    /** Epoch-day of this player's last free Lucky Spin, or null if never spun for free - compared
     * against `todayEpochDay`, same "one claim per day" shape as
     * [com.suman.memoryarchitect.domain.progression.DailyRewardCatalog.canClaim]. */
    val lastFreeSpinEpochDay: Long? = null,
    /** Same shape as [lastFreeSpinEpochDay], for the one bonus spin a rewarded ad unlocks per day. */
    val lastAdSpinEpochDay: Long? = null,
    /** True once this player has ever completed a Lucky Spin, by any
     * [com.suman.memoryarchitect.domain.repository.SpinSource] - the very first one is guaranteed
     * to resolve to [SpinRewardKind.Cosmetic] regardless of the odds table (see
     * [com.suman.memoryarchitect.domain.progression.SpinRules]'s doc), so a first-time spinner
     * always walks away with a real Shop item to go equip, not "just coins." */
    val hasEverSpun: Boolean = false,
) {
    companion object {
        val EMPTY = LuckySpinState()
    }
}
