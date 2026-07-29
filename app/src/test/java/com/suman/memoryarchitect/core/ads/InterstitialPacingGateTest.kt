package com.suman.memoryarchitect.core.ads

import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.RemoteConfig
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private class FakePreferences(
    sessionCount: Int = 5,
    lastInterstitialShownAtEpochMs: Long? = null,
    lastRewardedAdShownAtEpochMs: Long? = null,
    gamesPlayedAtLastInterstitial: Int = 0,
    interstitialsShownToday: Int = 0,
    interstitialsShownTodayEpochDay: Long? = null,
) : InterstitialPacingPreferences {
    private val sessionCountFlow = MutableStateFlow(sessionCount)
    private val lastInterstitialFlow = MutableStateFlow(lastInterstitialShownAtEpochMs)
    private val lastRewardedFlow = MutableStateFlow(lastRewardedAdShownAtEpochMs)
    private val gamesPlayedFlow = MutableStateFlow(gamesPlayedAtLastInterstitial)
    private val interstitialsShownTodayFlow = MutableStateFlow(interstitialsShownToday)
    private val interstitialsShownTodayEpochDayFlow = MutableStateFlow(interstitialsShownTodayEpochDay)

    override val sessionCount: Flow<Int> = sessionCountFlow
    override val lastInterstitialShownAtEpochMs: Flow<Long?> = lastInterstitialFlow
    override val lastRewardedAdShownAtEpochMs: Flow<Long?> = lastRewardedFlow
    override val gamesPlayedAtLastInterstitial: Flow<Int> = gamesPlayedFlow
    override val interstitialsShownToday: Flow<Int> = interstitialsShownTodayFlow
    override val interstitialsShownTodayEpochDay: Flow<Long?> = interstitialsShownTodayEpochDayFlow

    override suspend fun setLastInterstitialShownAtEpochMs(epochMs: Long) {
        lastInterstitialFlow.value = epochMs
    }

    override suspend fun setLastRewardedAdShownAtEpochMs(epochMs: Long) {
        lastRewardedFlow.value = epochMs
    }

    override suspend fun setGamesPlayedAtLastInterstitial(gamesPlayed: Int) {
        gamesPlayedFlow.value = gamesPlayed
    }

    override suspend fun setInterstitialsShownToday(count: Int, epochDay: Long) {
        interstitialsShownTodayFlow.value = count
        interstitialsShownTodayEpochDayFlow.value = epochDay
    }
}

private class FakeRemoteConfigRepository(private val rules: InterstitialPacingRules = InterstitialPacingRules.Default) : RemoteConfigRepository {
    var shouldFail = false

    override suspend fun getRemoteConfig(): Outcome<RemoteConfig> {
        if (shouldFail) return Outcome.Error(AppError.FeatureUnavailable)
        val values = mutableMapOf(
            "interstitial_ads_enabled" to rules.enabled.toString(),
            "interstitial_cooldown_seconds" to rules.cooldownSeconds.toString(),
            "interstitial_min_level_completions_before_first" to rules.minLevelCompletionsBeforeFirst.toString(),
            "interstitial_min_session_count" to rules.minSessionCountBeforeAny.toString(),
            "interstitial_session_cap" to rules.sessionCap.toString(),
            "interstitial_daily_cap" to rules.dailyCap.toString(),
            "interstitial_cooldown_after_rewarded_seconds" to rules.cooldownAfterRewardedSeconds.toString(),
        )
        return Outcome.Success(RemoteConfig(values = values, fetchedAt = 0L))
    }
}

/**
 * [InterstitialPacingGate.skipReasonOrNull] is the single decision point every interstitial pacing
 * requirement (frequency cap, cooldown, session-aware limit, first-session/first-few-level
 * protection, never-right-after-a-rewarded-ad) funnels through - these tests exercise each
 * condition independently, with every other condition held at its most permissive value, so a
 * failure in one never masks or gets masked by another.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterstitialPacingGateTest {

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC)

    private fun gate(
        preferences: InterstitialPacingPreferences = FakePreferences(),
        remoteConfigRepository: RemoteConfigRepository = FakeRemoteConfigRepository(),
        clock: Clock = fixedClock,
    ) = InterstitialPacingGate(preferences, remoteConfigRepository, clock)

    @Test
    fun `eligible when every condition is at its most permissive`() = runTest {
        val result = gate().skipReasonOrNull(currentGamesPlayed = 10)
        assertNull(result)
    }

    @Test
    fun `disabled via Remote Config skips with DISABLED`() = runTest {
        val remoteConfigRepository = FakeRemoteConfigRepository(InterstitialPacingRules.Default.copy(enabled = false))
        val result = gate(remoteConfigRepository = remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 10)
        assertEquals(InterstitialSkipReason.DISABLED, result)
    }

    @Test
    fun `a Remote Config failure falls back to the Default rules rather than crashing`() = runTest {
        val remoteConfigRepository = FakeRemoteConfigRepository().apply { shouldFail = true }
        // Default rules require minSessionCountBeforeAny=2 and minLevelCompletionsBeforeFirst=3 -
        // both satisfied here, so a successful fallback resolves to eligible, not an exception.
        val result = gate(remoteConfigRepository = remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 10)
        assertNull(result)
    }

    @Test
    fun `session cap reached skips with SESSION_CAP`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(sessionCap = 2)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)
        val gate = gate(remoteConfigRepository = remoteConfigRepository)

        gate.recordInterstitialShown(currentGamesPlayed = 10)
        gate.recordInterstitialShown(currentGamesPlayed = 20)

        assertEquals(InterstitialSkipReason.SESSION_CAP, gate.skipReasonOrNull(currentGamesPlayed = 30))
    }

    @Test
    fun `daily cap reached skips with DAILY_CAP even with the session cap and cooldowns wide open`() = runTest {
        val today = LocalDate.now(fixedClock).toEpochDay()
        val rules = InterstitialPacingRules.Default.copy(dailyCap = 3, sessionCap = 100)
        val preferences = FakePreferences(interstitialsShownToday = 3, interstitialsShownTodayEpochDay = today)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 100)

        assertEquals(InterstitialSkipReason.DAILY_CAP, result)
    }

    @Test
    fun `a stored count from a previous calendar day doesn't count against today's daily cap`() = runTest {
        val yesterday = LocalDate.now(fixedClock).toEpochDay() - 1
        val rules = InterstitialPacingRules.Default.copy(dailyCap = 3, sessionCap = 100)
        val preferences = FakePreferences(interstitialsShownToday = 10, interstitialsShownTodayEpochDay = yesterday)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 100)

        assertNull(result)
    }

    @Test
    fun `recordInterstitialShown increments today's count and stamps today's epoch day`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(
            dailyCap = 2,
            sessionCap = 100,
            cooldownSeconds = 0L,
            minLevelCompletionsBeforeFirst = 0,
        )
        val preferences = FakePreferences()
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)
        val gate = gate(preferences, remoteConfigRepository)

        gate.recordInterstitialShown(currentGamesPlayed = 10)
        assertNull(gate.skipReasonOrNull(currentGamesPlayed = 10)) // 1 of 2 used

        gate.recordInterstitialShown(currentGamesPlayed = 20)
        assertEquals(InterstitialSkipReason.DAILY_CAP, gate.skipReasonOrNull(currentGamesPlayed = 30)) // 2 of 2 used
    }

    @Test
    fun `under the required level completions since the last interstitial skips with LEVEL_COMPLETIONS`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(minLevelCompletionsBeforeFirst = 5)
        val preferences = FakePreferences(gamesPlayedAtLastInterstitial = 10)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 12)

        assertEquals(InterstitialSkipReason.LEVEL_COMPLETIONS, result)
    }

    @Test
    fun `reaching exactly the required level completions is eligible, not one short`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(minLevelCompletionsBeforeFirst = 5)
        val preferences = FakePreferences(gamesPlayedAtLastInterstitial = 10)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 15)

        assertNull(result)
    }

    @Test
    fun `a brand new install with zero games played is protected until the level-completion bar is cleared`() = runTest {
        // gamesPlayedAtLastInterstitial defaults to 0 (never shown one yet) - this is exactly
        // "first-few-level protection" for a fresh install, with no separate mechanism needed.
        val rules = InterstitialPacingRules.Default.copy(minLevelCompletionsBeforeFirst = 3)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        assertEquals(InterstitialSkipReason.LEVEL_COMPLETIONS, gate(remoteConfigRepository = remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 2))
        assertNull(gate(remoteConfigRepository = remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 3))
    }

    @Test
    fun `below the minimum session count skips with FIRST_SESSION`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(minSessionCountBeforeAny = 2)
        val preferences = FakePreferences(sessionCount = 1)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 100)

        assertEquals(InterstitialSkipReason.FIRST_SESSION, result)
    }

    @Test
    fun `a player's very first session never sees an interstitial regardless of levels played`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(minSessionCountBeforeAny = 2, minLevelCompletionsBeforeFirst = 0)
        val preferences = FakePreferences(sessionCount = 1)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 9_999)

        assertEquals(InterstitialSkipReason.FIRST_SESSION, result)
    }

    @Test
    fun `still within the cooldown window since the last interstitial skips with COOLDOWN`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(cooldownSeconds = 180L)
        val lastShown = fixedClock.millis() - 60_000L // 60s ago, cooldown is 180s
        val preferences = FakePreferences(lastInterstitialShownAtEpochMs = lastShown)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 100)

        assertEquals(InterstitialSkipReason.COOLDOWN, result)
    }

    @Test
    fun `past the cooldown window since the last interstitial is eligible again`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(cooldownSeconds = 180L)
        val lastShown = fixedClock.millis() - 200_000L // 200s ago, cooldown is 180s
        val preferences = FakePreferences(lastInterstitialShownAtEpochMs = lastShown)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 100)

        assertNull(result)
    }

    @Test
    fun `still within the post-rewarded-ad cooldown skips with COOLDOWN_AFTER_REWARDED`() = runTest {
        val rules = InterstitialPacingRules.Default.copy(cooldownAfterRewardedSeconds = 60L)
        val lastRewarded = fixedClock.millis() - 10_000L // 10s ago, cooldown is 60s
        val preferences = FakePreferences(lastRewardedAdShownAtEpochMs = lastRewarded)
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)

        val result = gate(preferences, remoteConfigRepository).skipReasonOrNull(currentGamesPlayed = 100)

        assertEquals(InterstitialSkipReason.COOLDOWN_AFTER_REWARDED, result)
    }

    @Test
    fun `recordInterstitialShown resets the level-completion baseline to the value passed in`() = runTest {
        // cooldownSeconds=0 isolates this test to the baseline-reset behavior alone - otherwise the
        // timestamp recordInterstitialShown also stamps would itself put every following check on
        // cooldown against this same fixed clock (see the dedicated cooldown-stamping test below).
        val rules = InterstitialPacingRules.Default.copy(minLevelCompletionsBeforeFirst = 3, cooldownSeconds = 0L)
        val preferences = FakePreferences()
        val remoteConfigRepository = FakeRemoteConfigRepository(rules)
        val gate = gate(preferences, remoteConfigRepository)

        gate.recordInterstitialShown(currentGamesPlayed = 50)

        // Immediately after, 2 more completions isn't enough yet...
        assertEquals(InterstitialSkipReason.LEVEL_COMPLETIONS, gate.skipReasonOrNull(currentGamesPlayed = 52))
        // ...but 3 more clears the bar again, measured from the new baseline (50), not the old one.
        assertNull(gate.skipReasonOrNull(currentGamesPlayed = 53))
    }

    @Test
    fun `recordInterstitialShown stamps the cooldown timestamp so an immediate re-check is on cooldown`() = runTest {
        val preferences = FakePreferences()
        val remoteConfigRepository = FakeRemoteConfigRepository()
        val gate = gate(preferences, remoteConfigRepository)

        gate.recordInterstitialShown(currentGamesPlayed = 100)

        assertEquals(InterstitialSkipReason.COOLDOWN, gate.skipReasonOrNull(currentGamesPlayed = 200))
    }
}
