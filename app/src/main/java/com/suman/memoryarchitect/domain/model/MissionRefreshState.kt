package com.suman.memoryarchitect.domain.model

/**
 * Per-period "pay to skip the countdown" overrides - see
 * [com.suman.memoryarchitect.domain.repository.MissionRepository.unlockAllMissionsEarly]'s doc.
 * Deliberately its own small state, kept out of [PlayerProfile], the same "Missions keeps its own
 * per-concern documents" convention this session's `LuckySpinState` already established for Shop.
 *
 * A `null` field means "no override - use the natural, calendar-derived periodKey" (see
 * [com.suman.memoryarchitect.domain.progression.MissionCatalog.effectivePeriodKey]). A non-null
 * value only ever matters until the real calendar's own periodKey catches up past it, at which
 * point [MissionCatalog.effectivePeriodKey]'s `maxOf` starts tracking the natural key again with
 * no cleanup step needed - self-healing by construction.
 */
data class MissionRefreshState(
    val dailyForcedPeriodKey: Long? = null,
    val weeklyForcedPeriodKey: Long? = null,
    val monthlyForcedPeriodKey: Long? = null,
) {
    companion object {
        val EMPTY = MissionRefreshState()
    }
}
