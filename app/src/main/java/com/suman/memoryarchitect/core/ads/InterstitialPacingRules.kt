package com.suman.memoryarchitect.core.ads

/**
 * Data-driven tuning for [InterstitialPacingGate] - same "balancing is a data change, not a code
 * change" convention [com.suman.memoryarchitect.domain.scoring.ScoringRules]/
 * [com.suman.memoryarchitect.domain.progression.SpinRules] already use, resolved live from Remote
 * Config every time via [RemoteConfig.interstitialPacingRules] rather than a compile-time constant -
 * every field here is retunable from the console with no app update, per the "never require an app
 * update for tuning ad behavior" requirement.
 */
data class InterstitialPacingRules(
    /** Already folds in the `emergency_ads_disabled` master kill switch - see
     * [RemoteConfig.interstitialAdsEnabled]'s doc. */
    val enabled: Boolean = true,
    /** Minimum real time between two interstitials, regardless of any other condition. */
    val cooldownSeconds: Long = 180L,
    /** [InterstitialPacingGate] resets its own completions-since-last-interstitial counter to 0
     * every time one shows, then requires it to reach this value again before the next one is
     * even considered - a "frequency cap" applied continuously, not just once. This is also what
     * gives brand-new installs "first-few-level protection" for free: the counter starts at 0, so
     * the very first interstitial an install could ever see already has to clear this same bar. */
    val minLevelCompletionsBeforeFirst: Int = 3,
    /** "First-session protection" - [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore.sessionCount]
     * must already be at least this value; a player's very first app session never sees one. */
    val minSessionCountBeforeAny: Int = 2,
    /** Hard ceiling on interstitials shown in one app session, regardless of how many natural
     * transition points occur. */
    val sessionCap: Int = 4,
    /** Hard ceiling on interstitials shown in one *calendar day*, independent of [sessionCap] -
     * several short sessions in one day would otherwise dodge a session-only limit entirely. This
     * is the "daily limits" tunable the monetization brief calls out by name as its own Remote
     * Config category, distinct from the per-session cap above. */
    val dailyCap: Int = 6,
    /** Never show one this soon after any rewarded ad, successful or not - back-to-back full-screen
     * ads of any kind is exactly the "two ads back-to-back" pattern this whole system exists to
     * avoid. */
    val cooldownAfterRewardedSeconds: Long = 60L,
) {
    companion object {
        val Default = InterstitialPacingRules()
    }
}
