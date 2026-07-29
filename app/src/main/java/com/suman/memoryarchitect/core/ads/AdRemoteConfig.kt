package com.suman.memoryarchitect.core.ads

import com.suman.memoryarchitect.domain.model.RemoteConfig

/**
 * Typed accessors over [RemoteConfig]'s raw string map for every ad-related tunable - same
 * `remoteConfig.values["key"] ?: default` convention
 * [com.suman.memoryarchitect.domain.progression.LiveEventCatalog.activeEvent] already uses, not a
 * new typed-config layer. Every default here is the sensible starting point if a value is missing
 * (a key not yet set in the console, or [RemoteConfig] itself falling back to a stale Room cache -
 * see `RemoteConfigRepositoryImpl`'s doc) - ads always degrade to a reasonable default, never to a
 * crash or an always-on/always-off surprise.
 *
 * [emergencyAdsDisabled] is the one master kill switch every other `*Enabled` accessor here already
 * folds in - flipping it in the Remote Config console turns off banner+interstitial+rewarded
 * instantly for every install, no release needed, exactly the "emergency disable switch" requested.
 */
fun RemoteConfig.emergencyAdsDisabled(): Boolean = values["emergency_ads_disabled"]?.toBooleanStrictOrNull() ?: false

fun RemoteConfig.bannerAdsEnabled(): Boolean =
    !emergencyAdsDisabled() && (values["banner_ads_enabled"]?.toBooleanStrictOrNull() ?: true)

fun RemoteConfig.interstitialAdsEnabled(): Boolean =
    !emergencyAdsDisabled() && (values["interstitial_ads_enabled"]?.toBooleanStrictOrNull() ?: true)

fun RemoteConfig.rewardedAdsEnabled(): Boolean =
    !emergencyAdsDisabled() && (values["rewarded_ads_enabled"]?.toBooleanStrictOrNull() ?: true)

/** Resolves a full [InterstitialPacingRules] from this [RemoteConfig] - every field individually
 * falls back to [InterstitialPacingRules.Default] if its key is missing/unparseable, so a single
 * malformed console value never breaks every other pacing rule at once. */
fun RemoteConfig.interstitialPacingRules(): InterstitialPacingRules {
    val default = InterstitialPacingRules.Default
    return InterstitialPacingRules(
        enabled = interstitialAdsEnabled(),
        cooldownSeconds = values["interstitial_cooldown_seconds"]?.toLongOrNull() ?: default.cooldownSeconds,
        minLevelCompletionsBeforeFirst = values["interstitial_min_level_completions_before_first"]?.toIntOrNull()
            ?: default.minLevelCompletionsBeforeFirst,
        minSessionCountBeforeAny = values["interstitial_min_session_count"]?.toIntOrNull() ?: default.minSessionCountBeforeAny,
        sessionCap = values["interstitial_session_cap"]?.toIntOrNull() ?: default.sessionCap,
        dailyCap = values["interstitial_daily_cap"]?.toIntOrNull() ?: default.dailyCap,
        cooldownAfterRewardedSeconds = values["interstitial_cooldown_after_rewarded_seconds"]?.toLongOrNull()
            ?: default.cooldownAfterRewardedSeconds,
    )
}

/** Gates the "Optional Reward Multiplier" rewarded placement - off by default until deliberately
 * turned on in the console, since it's the one rewarded placement that changes a real economic
 * outcome (doubling a round's coins/xp) rather than just granting a flat, pre-defined amount. */
fun RemoteConfig.rewardMultiplierEnabled(): Boolean = values["reward_multiplier_enabled"]?.toBooleanStrictOrNull() ?: false

fun RemoteConfig.rewardMultiplierValue(): Double = values["reward_multiplier_value"]?.toDoubleOrNull() ?: 2.0
