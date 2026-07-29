package com.suman.memoryarchitect.core.ads

import com.suman.memoryarchitect.domain.model.RemoteConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun remoteConfigOf(vararg pairs: Pair<String, String>) = RemoteConfig(values = mapOf(*pairs), fetchedAt = 0L)

class AdRemoteConfigTest {

    @Test
    fun `every ad toggle defaults to enabled and the kill switch defaults to off when no keys are set`() {
        val remoteConfig = remoteConfigOf()

        assertFalse(remoteConfig.emergencyAdsDisabled())
        assertTrue(remoteConfig.bannerAdsEnabled())
        assertTrue(remoteConfig.interstitialAdsEnabled())
        assertTrue(remoteConfig.rewardedAdsEnabled())
    }

    @Test
    fun `an unparseable boolean value falls back to the default rather than throwing`() {
        val remoteConfig = remoteConfigOf("banner_ads_enabled" to "not-a-boolean")

        assertTrue(remoteConfig.bannerAdsEnabled())
    }

    @Test
    fun `emergency kill switch overrides every individual toggle even when each is explicitly true`() {
        val remoteConfig = remoteConfigOf(
            "emergency_ads_disabled" to "true",
            "banner_ads_enabled" to "true",
            "interstitial_ads_enabled" to "true",
            "rewarded_ads_enabled" to "true",
        )

        assertTrue(remoteConfig.emergencyAdsDisabled())
        assertFalse(remoteConfig.bannerAdsEnabled())
        assertFalse(remoteConfig.interstitialAdsEnabled())
        assertFalse(remoteConfig.rewardedAdsEnabled())
    }

    @Test
    fun `each ad toggle can be individually disabled without affecting the others`() {
        val remoteConfig = remoteConfigOf("interstitial_ads_enabled" to "false")

        assertFalse(remoteConfig.interstitialAdsEnabled())
        assertTrue(remoteConfig.bannerAdsEnabled())
        assertTrue(remoteConfig.rewardedAdsEnabled())
    }

    @Test
    fun `interstitialPacingRules falls back to every Default field when no keys are set`() {
        val rules = remoteConfigOf().interstitialPacingRules()

        assertEquals(InterstitialPacingRules.Default, rules)
    }

    @Test
    fun `interstitialPacingRules parses every field from its own key`() {
        val remoteConfig = remoteConfigOf(
            "interstitial_ads_enabled" to "false",
            "interstitial_cooldown_seconds" to "300",
            "interstitial_min_level_completions_before_first" to "7",
            "interstitial_min_session_count" to "5",
            "interstitial_session_cap" to "2",
            "interstitial_daily_cap" to "9",
            "interstitial_cooldown_after_rewarded_seconds" to "90",
        )

        val rules = remoteConfig.interstitialPacingRules()

        assertEquals(
            InterstitialPacingRules(
                enabled = false,
                cooldownSeconds = 300L,
                minLevelCompletionsBeforeFirst = 7,
                minSessionCountBeforeAny = 5,
                sessionCap = 2,
                dailyCap = 9,
                cooldownAfterRewardedSeconds = 90L,
            ),
            rules,
        )
    }

    @Test
    fun `a single malformed pacing key falls back to just that field's default, not the whole rule set`() {
        val remoteConfig = remoteConfigOf(
            "interstitial_cooldown_seconds" to "not-a-number",
            "interstitial_session_cap" to "2",
        )

        val rules = remoteConfig.interstitialPacingRules()

        assertEquals(InterstitialPacingRules.Default.cooldownSeconds, rules.cooldownSeconds)
        assertEquals(2, rules.sessionCap)
    }

    @Test
    fun `interstitialPacingRules enabled field mirrors interstitialAdsEnabled including the emergency kill switch`() {
        val remoteConfig = remoteConfigOf(
            "emergency_ads_disabled" to "true",
            "interstitial_ads_enabled" to "true",
        )

        assertFalse(remoteConfig.interstitialPacingRules().enabled)
    }

    @Test
    fun `reward multiplier defaults to disabled with a 2x value when no keys are set`() {
        val remoteConfig = remoteConfigOf()

        assertFalse(remoteConfig.rewardMultiplierEnabled())
        assertEquals(2.0, remoteConfig.rewardMultiplierValue(), 0.0)
    }

    @Test
    fun `reward multiplier can be enabled with a custom value`() {
        val remoteConfig = remoteConfigOf(
            "reward_multiplier_enabled" to "true",
            "reward_multiplier_value" to "3.5",
        )

        assertTrue(remoteConfig.rewardMultiplierEnabled())
        assertEquals(3.5, remoteConfig.rewardMultiplierValue(), 0.0)
    }
}
