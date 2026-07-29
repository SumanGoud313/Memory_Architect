package com.suman.memoryarchitect.core.ads

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place every "should an interstitial show right now" decision gets made -
 * [InterstitialAdController] only knows how to load/show one when told to, never whether it's a
 * good moment. Every condition in [skipReasonOrNull] must pass; failing any one is a routine,
 * expected "not now," never an error (see
 * [com.suman.memoryarchitect.core.analytics.logInterstitialSkipped] for the distinction that
 * matters for the ad funnel).
 *
 * Deliberately has no dependency on [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]/
 * [RewardedAdFlow] to track "levels completed"/"a rewarded ad just showed" - both of this app's
 * *other* frequency signals already exist elsewhere and are read fresh at decision time instead:
 * - "N levels completed since the last interstitial" is derived from
 *   [com.suman.memoryarchitect.domain.model.PlayerStatistics.gamesPlayed] (already tracked,
 *   persistent) minus [UserPreferencesDataStore.gamesPlayedAtLastInterstitial] (a baseline snapshot
 *   this class itself stamps) - no new counter needs wiring through every scored-round call site.
 * - "just watched a rewarded ad" is stamped by [RewardedAdControllerImpl] itself (the one shared
 *   implementation behind every Hint/Redo/Rewatch/Lucky-Spin/Bonus-Coins/etc. rewarded placement),
 *   not by each ViewModel that happens to use one.
 *
 * [interstitialsShownThisSession] is the one genuinely session-scoped (not persisted) piece of
 * state - a plain `@Singleton` instance field, correctly reset to 0 on every process start, which
 * is exactly what "session-aware" cap means. [InterstitialPacingRules.dailyCap] is the complementary
 * *calendar-day* cap - persisted (not in-memory), since several short sessions in one day would
 * otherwise dodge a session-only limit entirely; see [todaysShownCount]'s own doc for how a new
 * day's count resets without an explicit reset call.
 */
@Singleton
class InterstitialPacingGate @Inject constructor(
    private val preferences: InterstitialPacingPreferences,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val clock: Clock,
) {
    private var interstitialsShownThisSession = 0

    /** Called by [RewardedAdControllerImpl] itself - the one shared implementation behind every
     * rewarded placement (Hint/Redo/Rewatch/Lucky Spin/Bonus Coins/XP/Mystery Chest/Reward
     * Multiplier) - the moment any rewarded ad actually resolves as shown-and-dismissed (earned or
     * not; either way a full-screen ad experience just happened, which is what
     * [InterstitialPacingRules.cooldownAfterRewardedSeconds] guards against stacking another one
     * on top of). Never called for a load/show failure - no ad was actually shown, nothing to cool
     * down from. */
    suspend fun recordRewardedAdShown() {
        preferences.setLastRewardedAdShownAtEpochMs(clock.millis())
    }

    /** Called only after [InterstitialAdController.loadAndShow] actually returns
     * [InterstitialAdResult.Shown] - never speculatively before attempting to show, so a load/show
     * failure never consumes any budget. [currentGamesPlayed] becomes the new baseline
     * [UserPreferencesDataStore.gamesPlayedAtLastInterstitial] compares future attempts against. */
    suspend fun recordInterstitialShown(currentGamesPlayed: Int) {
        interstitialsShownThisSession += 1
        preferences.setLastInterstitialShownAtEpochMs(clock.millis())
        preferences.setGamesPlayedAtLastInterstitial(currentGamesPlayed)
        preferences.setInterstitialsShownToday(todaysShownCount() + 1, todayEpochDay())
    }

    private fun todayEpochDay(): Long = LocalDate.now(clock).toEpochDay()

    /** `0` whenever [InterstitialPacingPreferences.interstitialsShownTodayEpochDay] isn't today -
     * a new calendar day's count starts fresh with no explicit reset call needed, the same way
     * [interstitialsShownThisSession] starts fresh every process start with no explicit reset. */
    private suspend fun todaysShownCount(): Int {
        val storedDay = preferences.interstitialsShownTodayEpochDay.first()
        return if (storedDay == todayEpochDay()) preferences.interstitialsShownToday.first() else 0
    }

    /** `null` return means "eligible" - a non-null [InterstitialSkipReason] names exactly which
     * condition failed, both for the caller to log via
     * [com.suman.memoryarchitect.core.analytics.logInterstitialSkipped] and so this class's own
     * tests can assert on *why*, not just a bare boolean. [currentGamesPlayed] is the caller's
     * freshly-read [com.suman.memoryarchitect.domain.model.PlayerStatistics.gamesPlayed] - see this
     * class's own doc for why that lifetime counter (not a new one) is what "levels completed"
     * pacing is measured against. */
    suspend fun skipReasonOrNull(currentGamesPlayed: Int): InterstitialSkipReason? {
        val rules = (remoteConfigRepository.getRemoteConfig() as? Outcome.Success)?.data?.interstitialPacingRules()
            ?: InterstitialPacingRules.Default
        if (!rules.enabled) return InterstitialSkipReason.DISABLED

        if (interstitialsShownThisSession >= rules.sessionCap) return InterstitialSkipReason.SESSION_CAP
        if (todaysShownCount() >= rules.dailyCap) return InterstitialSkipReason.DAILY_CAP

        val gamesPlayedAtLast = preferences.gamesPlayedAtLastInterstitial.first()
        if (currentGamesPlayed - gamesPlayedAtLast < rules.minLevelCompletionsBeforeFirst) {
            return InterstitialSkipReason.LEVEL_COMPLETIONS
        }

        val sessionCount = preferences.sessionCount.first()
        if (sessionCount < rules.minSessionCountBeforeAny) return InterstitialSkipReason.FIRST_SESSION

        val now = clock.millis()
        val lastInterstitial = preferences.lastInterstitialShownAtEpochMs.first()
        if (lastInterstitial != null && now - lastInterstitial < rules.cooldownSeconds * 1_000L) {
            return InterstitialSkipReason.COOLDOWN
        }
        val lastRewarded = preferences.lastRewardedAdShownAtEpochMs.first()
        if (lastRewarded != null && now - lastRewarded < rules.cooldownAfterRewardedSeconds * 1_000L) {
            return InterstitialSkipReason.COOLDOWN_AFTER_REWARDED
        }
        return null
    }
}

/** Mirrors [com.suman.memoryarchitect.core.analytics.logInterstitialSkipped]'s own `reason` string
 * values exactly - [analyticsName] is what actually gets logged. */
enum class InterstitialSkipReason(val analyticsName: String) {
    DISABLED("disabled"),
    SESSION_CAP("session_cap"),
    DAILY_CAP("daily_cap"),
    LEVEL_COMPLETIONS("first_levels_protected"),
    FIRST_SESSION("first_session_protected"),
    COOLDOWN("cooldown"),
    COOLDOWN_AFTER_REWARDED("cooldown_after_rewarded"),
}
