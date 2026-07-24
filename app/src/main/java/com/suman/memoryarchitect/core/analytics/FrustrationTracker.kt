package com.suman.memoryarchitect.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory, process-lifetime tracker for the one frustration signal that genuinely needs to
 * survive across separate [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]
 * instances: consecutive fails on the *same* level across separate attempts (a fresh ViewModel is
 * created each time a level is re-entered, so an instance-scoped counter can't see this on its
 * own). Deliberately not persisted - this is a live-session signal ("is the player stuck on this
 * level right now"), not a permanent record, and resets naturally on app restart same as the rest
 * of a play session's momentum.
 */
@Singleton
class FrustrationTracker @Inject constructor() {
    private val consecutiveFailsByLevel = mutableMapOf<Int, Int>()
    private val startCountByLevel = mutableMapOf<Int, Int>()

    /** Returns the new consecutive-fail count for [levelNumber] after recording this attempt's
     * [passed] result - callers fire a frustration signal once this crosses their own threshold. */
    fun recordAttempt(levelNumber: Int, passed: Boolean): Int {
        if (passed) {
            consecutiveFailsByLevel.remove(levelNumber)
            return 0
        }
        val updated = (consecutiveFailsByLevel[levelNumber] ?: 0) + 1
        consecutiveFailsByLevel[levelNumber] = updated
        return updated
    }

    /** Returns how many times [levelNumber] has been started this session (this session only - a
     * fresh app process starts every count back at zero). A replayed/retried level always creates
     * a brand new [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel] instance, so
     * this has to live here rather than on the ViewModel itself for "retried the same level
     * repeatedly" to be visible across those separate instances. */
    fun recordLevelStart(levelNumber: Int): Int {
        val updated = (startCountByLevel[levelNumber] ?: 0) + 1
        startCountByLevel[levelNumber] = updated
        return updated
    }
}
