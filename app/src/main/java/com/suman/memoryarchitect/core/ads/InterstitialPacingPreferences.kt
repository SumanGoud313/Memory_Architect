package com.suman.memoryarchitect.core.ads

import kotlinx.coroutines.flow.Flow

/**
 * The exact slice of [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore] that
 * [InterstitialPacingGate] needs - extracted purely so that class is unit-testable with a plain
 * fake instead of a real Android DataStore/Context (this app has no Robolectric setup, and adding
 * one just for this would be a much bigger change than the gate itself). Bound to the real
 * [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore] in [com.suman.memoryarchitect.di.AdsModule] -
 * production code never sees a difference, this interface exists for tests only.
 */
interface InterstitialPacingPreferences {
    val sessionCount: Flow<Int>
    val lastInterstitialShownAtEpochMs: Flow<Long?>
    suspend fun setLastInterstitialShownAtEpochMs(epochMs: Long)
    val lastRewardedAdShownAtEpochMs: Flow<Long?>
    suspend fun setLastRewardedAdShownAtEpochMs(epochMs: Long)
    val gamesPlayedAtLastInterstitial: Flow<Int>
    suspend fun setGamesPlayedAtLastInterstitial(gamesPlayed: Int)
    /** How many interstitials have shown on [interstitialsShownTodayEpochDay] - a *calendar-day*
     * cap, deliberately separate from [InterstitialPacingGate]'s in-memory per-session cap, since
     * several short sessions in one day would otherwise dodge a session-only limit entirely. A
     * stale [interstitialsShownTodayEpochDay] (any day other than today) means this count is from a
     * previous day and reads as 0 - see [InterstitialPacingGate]'s own handling, there is no
     * separate "reset" call. */
    val interstitialsShownToday: Flow<Int>
    val interstitialsShownTodayEpochDay: Flow<Long?>
    suspend fun setInterstitialsShownToday(count: Int, epochDay: Long)
}
