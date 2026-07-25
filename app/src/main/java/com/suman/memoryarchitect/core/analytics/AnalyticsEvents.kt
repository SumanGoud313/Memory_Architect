package com.suman.memoryarchitect.core.analytics

import com.suman.memoryarchitect.domain.generation.GenerationRules
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * The full event catalog this app logs — named functions instead of call sites building
 * `logEvent("...", mapOf(...))` by hand, so the same event can never be logged with a typo'd name
 * or a different param shape from two different places. See ANALYTICS_EVENTS.md for the full
 * catalog with recommended Firebase reports/funnels/audiences built on top of these.
 *
 * A few things Steps 4/7/12 of the analytics brief ask for are *not* here because Firebase already
 * provides them automatically and re-logging them would be a banned duplicate event:
 * `first_open`, `session_start`, `app_update`, and the device/OS/locale/country dimensions are all
 * automatically collected the instant the SDK is configured (see FIREBASE_SETUP.md); DAU/WAU/MAU,
 * average session duration, sessions/user, and D1/D7/D30 retention are all *computed* by Firebase
 * from that automatic collection plus [logSessionEnded] below, not something an app "logs" as an
 * event — see the Recommended Reports section of ANALYTICS_EVENTS.md.
 */

// ===================================================================================
// Lifecycle
// ===================================================================================

fun AnalyticsLogger.logAppBackgrounded() = logEvent("app_backgrounded")

fun AnalyticsLogger.logAppForegrounded() = logEvent("app_foregrounded")

/** [durationMs] is measured client-side from when the owning ViewModel/process was created to
 * [android.app.Application.ActivityLifecycleCallbacks] detecting the app went to background -
 * Firebase's own session concept is inferred from event timestamps, not an explicit duration, so
 * this is the one lifecycle signal genuinely worth a custom event rather than relying on
 * automatic collection alone. */
fun AnalyticsLogger.logSessionEnded(mode: GameMode?, durationMs: Long) =
    logEvent("session_ended", mapOf("mode" to mode?.name, "duration_ms" to durationMs))

// ===================================================================================
// Screens (Compose Navigation has no Fragment/Activity-per-screen for Firebase's automatic
// screen_view tracking to hook into, since this whole app is one Activity - so unlike
// first_open/session_start, this one genuinely needs a manual call per screen).
// ===================================================================================

fun AnalyticsLogger.logScreenView(screenName: String) =
    logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName))

// ===================================================================================
// Gameplay flow
// ===================================================================================

fun AnalyticsLogger.logModeSelected(mode: GameMode) =
    logEvent("mode_selected", mapOf("mode" to mode.name))

fun AnalyticsLogger.logLevelStarted(mode: GameMode, levelNumber: Int, level: LevelSpec) =
    logEvent("level_started", levelContextParams(mode, levelNumber, level))

fun AnalyticsLogger.logLevelRestarted(mode: GameMode, levelNumber: Int) =
    logEvent("level_restarted", mapOf("mode" to mode.name, "level_number" to levelNumber))

fun AnalyticsLogger.logMemorizeStarted(mode: GameMode, levelNumber: Int, level: LevelSpec) =
    logEvent("memorize_started", levelContextParams(mode, levelNumber, level))

/** Only fires when Memorize runs to its natural end - a player who backs out mid-Memorize
 * produces [logLevelQuitMidway] instead, never this. */
fun AnalyticsLogger.logMemorizeFinished(mode: GameMode, levelNumber: Int) =
    logEvent("memorize_finished", mapOf("mode" to mode.name, "level_number" to levelNumber))

fun AnalyticsLogger.logRebuildStarted(mode: GameMode, levelNumber: Int) =
    logEvent("rebuild_started", mapOf("mode" to mode.name, "level_number" to levelNumber))

/** Only for a genuine player tap (see [GameplayViewModel.submitReconstruction]'s `isAutoSubmit`
 * param) - the countdown expiring instead logs [logTimeExpired], never this, so the two can never
 * double-count the same submission. */
fun AnalyticsLogger.logSubmitPressed(mode: GameMode, levelNumber: Int, objectsPlaced: Int) =
    logEvent("submit_pressed", mapOf("mode" to mode.name, "level_number" to levelNumber, "objects_placed" to objectsPlaced))

fun AnalyticsLogger.logTimeExpired(mode: GameMode, levelNumber: Int, objectsPlaced: Int) =
    logEvent("time_expired", mapOf("mode" to mode.name, "level_number" to levelNumber, "objects_placed" to objectsPlaced))

/** The one event nearly every business question in ANALYTICS_EVENTS.md's "recommended reports"
 * section is built from. [passed] is null outside Classic (no pass/fail gate elsewhere) - "level
 * failed" isn't a separate event (see the module doc) but a filtered view of this one where
 * `passed = false`, so failure-rate analysis and completion analysis share one event instead of
 * two that could ever disagree. */
fun AnalyticsLogger.logLevelCompleted(
    mode: GameMode,
    levelNumber: Int,
    level: LevelSpec,
    stars: Int,
    accuracy: Float,
    passed: Boolean?,
    timeTakenMs: Long,
    mistakesCount: Int,
    hintsUsed: Int,
    redosUsed: Int,
    rewatchesUsed: Int,
    xpAwarded: Long?,
    coinsAwarded: Long?,
) = logEvent(
    "level_completed",
    levelContextParams(mode, levelNumber, level) + mapOf(
        "stars" to stars,
        "accuracy" to accuracy,
        "passed" to passed,
        "time_taken_ms" to timeTakenMs,
        "mistakes_count" to mistakesCount,
        "hints_used" to hintsUsed,
        "redos_used" to redosUsed,
        "rewatches_used" to rewatchesUsed,
        "xp_awarded" to xpAwarded,
        "coins_awarded" to coinsAwarded,
    ),
)

/** [phase] is `"memorize"` or `"reconstruct"` - fires from [GameplayViewModel.onCleared] whenever
 * the round never reached Finished, covering both "backed out" and "process/activity torn down
 * mid-round" the same way. Also the source of the frustration signals "quit during memorize" /
 * "quit during reconstruct" / "level abandoned" - see [logFrustrationSignal] - rather than three
 * near-duplicate events for the same underlying moment. */
fun AnalyticsLogger.logLevelQuitMidway(mode: GameMode, levelNumber: Int, phase: String, elapsedMs: Long) =
    logEvent("level_quit_midway", mapOf("mode" to mode.name, "level_number" to levelNumber, "phase" to phase, "elapsed_ms" to elapsedMs))

private fun levelContextParams(mode: GameMode, levelNumber: Int, level: LevelSpec): Map<String, Any?> {
    val slotCount = GenerationRules.Default.roomSlotCountByScene[level.sceneType]
    return mapOf(
        "mode" to mode.name,
        "level_number" to levelNumber,
        "room_theme" to level.sceneType,
        "object_count" to level.objects.size,
        "object_density" to slotCount?.let { level.objects.size.toFloat() / it },
        "rotation_enabled" to level.objects.any { it.rotationDegrees != 0 },
        "order_mode_enabled" to level.orderModeEnabled,
        "memorize_timer_ms" to level.memorizeDurationMs,
    )
}

// ===================================================================================
// Assist features
// ===================================================================================

fun AnalyticsLogger.logHintUsed(mode: GameMode, levelNumber: Int) =
    logEvent("hint_used", mapOf("mode" to mode.name, "level_number" to levelNumber))

fun AnalyticsLogger.logRedoUsed(mode: GameMode, levelNumber: Int) =
    logEvent("redo_used", mapOf("mode" to mode.name, "level_number" to levelNumber))

fun AnalyticsLogger.logRewatchUsed(mode: GameMode, levelNumber: Int) =
    logEvent("rewatch_used", mapOf("mode" to mode.name, "level_number" to levelNumber))

// ===================================================================================
// Frustration detection - one consolidated event with a `signal_type` param rather than 8
// near-duplicate event names, so "which levels are difficult" is one filtered query
// (`frustration_signal` grouped by `signal_type` and `level_number`) instead of eight.
// ===================================================================================

/** [signalType] is one of: `repeated_retry`, `quit_during_memorize`, `quit_during_reconstruct`,
 * `excessive_hints`, `excessive_redo`, `excessive_rewatch`, `long_duration`, `repeated_failures`.
 * [detail] is a short free-form value specific to the signal (e.g. the retry count) - optional,
 * kept out of the param name itself so this stays one event shape for every signal type. */
fun AnalyticsLogger.logFrustrationSignal(signalType: String, mode: GameMode, levelNumber: Int, detail: String? = null) =
    logEvent(
        "frustration_signal",
        mapOf("signal_type" to signalType, "mode" to mode.name, "level_number" to levelNumber, "detail" to detail),
    )

// ===================================================================================
// Streak & Daily Check-In - see StreakCalculator/DailyRewardCatalog. [milestoneDay] is one of
// StreakRules.milestoneDays (3/7/14/30/60/90/180/365); [remainingShields] is the player's Streak
// Shield balance right after the event, so "did the shield economy actually work" (grants vs.
// consumes vs. players sitting at the cap) is answerable straight from these two events.
// ===================================================================================

fun AnalyticsLogger.logStreakMilestoneReached(milestoneDay: Int, remainingShields: Int) =
    logEvent("streak_milestone_reached", mapOf("milestone_day" to milestoneDay, "remaining_shields" to remainingShields))

fun AnalyticsLogger.logStreakShieldEarned(milestoneDay: Int, remainingShields: Int) =
    logEvent("streak_shield_earned", mapOf("milestone_day" to milestoneDay, "remaining_shields" to remainingShields))

fun AnalyticsLogger.logStreakShieldConsumed(remainingShields: Int) =
    logEvent("streak_shield_consumed", mapOf("remaining_shields" to remainingShields))

/** [rewardKind] is one of [com.suman.memoryarchitect.domain.model.DailyRewardKind]'s names
 * ("COINS"/"XP"/"MYSTERY_CHEST"). [shieldAwarded] tags the one cycle day (see
 * [com.suman.memoryarchitect.domain.progression.DailyRewardCatalog]) that also grants a Streak
 * Shield, separately from [logStreakShieldEarned]'s milestone-triggered path - the two shield
 * sources stay distinguishable in analytics even though they land on the same profile field. */
fun AnalyticsLogger.logDailyRewardClaimed(cycleDay: Int, rewardKind: String, coinsAwarded: Long, xpAwarded: Long, shieldAwarded: Boolean) =
    logEvent(
        "daily_reward_claimed",
        mapOf(
            "cycle_day" to cycleDay,
            "reward_kind" to rewardKind,
            "coins_awarded" to coinsAwarded,
            "xp_awarded" to xpAwarded,
            "shield_awarded" to shieldAwarded,
        ),
    )

// ===================================================================================
// Feature usage
// ===================================================================================

fun AnalyticsLogger.logAchievementUnlocked(achievementId: String) =
    logEvent("achievement_unlocked", mapOf("achievement_id" to achievementId))

fun AnalyticsLogger.logHapticsToggled(enabled: Boolean) =
    logEvent("haptics_toggled", mapOf("enabled" to enabled))

/** [level] is one of [android.content.ComponentCallbacks2]'s `TRIM_MEMORY_*` constant names -
 * logged from [com.suman.memoryarchitect.MemoryArchitectApp.onTrimMemory]. */
fun AnalyticsLogger.logMemoryWarning(level: String) = logEvent("memory_warning", mapOf("level" to level))

// ===================================================================================
// Monetization - granular ad-funnel events (from RewardedAdControllerImpl, the raw SDK
// callbacks) plus the existing feature-level rewarded_ad_result summary (from RewardedAdFlow) -
// different layers answering different questions, not duplicates of each other. See
// ANALYTICS_EVENTS.md for the funnel these are meant to build.
// ===================================================================================

fun AnalyticsLogger.logAdRequested(feature: String) = logEvent("ad_requested", mapOf("feature" to feature))

fun AnalyticsLogger.logAdLoaded(feature: String) = logEvent("ad_loaded", mapOf("feature" to feature))

fun AnalyticsLogger.logAdLoadFailed(feature: String) = logEvent("ad_load_failed", mapOf("feature" to feature))

fun AnalyticsLogger.logAdShown(feature: String) = logEvent("ad_shown", mapOf("feature" to feature))

fun AnalyticsLogger.logAdShowFailed(feature: String) = logEvent("ad_show_failed", mapOf("feature" to feature))

fun AnalyticsLogger.logAdClosed(feature: String, earnedReward: Boolean) =
    logEvent("ad_closed", mapOf("feature" to feature, "earned_reward" to earnedReward))

/** Logged once per resolved rewarded-ad attempt, for every feature that flows through
 * [com.suman.memoryarchitect.core.ads.RewardedAdFlow] - a single call site covers Hint/Redo/
 * Rewatch/any future feature rather than each one logging its own copy. [result] is one of
 * "rewarded", "cancelled", or "failed:<reason>". */
fun AnalyticsLogger.logRewardedAdResult(feature: String, result: String) =
    logEvent("rewarded_ad_result", mapOf("feature" to feature, "result" to result))

/** Logged from [com.google.android.gms.ads.OnPaidEventListener] - the one Firebase+AdMob event
 * name/param set that's *not* ours to invent (`ad_impression` with these exact param keys is
 * Firebase's own recommended schema for AdMob revenue linking; using anything else forfeits the
 * automatic LTV/revenue reporting Firebase builds on top of it). [valueMicros] is AdMob's raw
 * micros value (1,000,000 micros = 1 unit of [currencyCode]). */
fun AnalyticsLogger.logAdRevenue(feature: String, valueMicros: Long, currencyCode: String) =
    logEvent(
        FirebaseAnalytics.Event.AD_IMPRESSION,
        mapOf(
            FirebaseAnalytics.Param.AD_PLATFORM to "admob",
            FirebaseAnalytics.Param.AD_SOURCE to "AdMob",
            FirebaseAnalytics.Param.AD_FORMAT to "rewarded",
            FirebaseAnalytics.Param.AD_UNIT_NAME to feature,
            FirebaseAnalytics.Param.CURRENCY to currencyCode,
            FirebaseAnalytics.Param.VALUE to valueMicros / 1_000_000.0,
        ),
    )

// ===================================================================================
// User properties (Step 12) - persistent player attributes, not point-in-time events. Firebase
// caps property names at 24 characters and values at 36 - every name/value below stays well
// under both. "Highest Daily/Weekly Challenge" from the brief isn't included: unlike Classic,
// Daily/Weekly have no persistent numbered-level concept to hold a "highest" of - the closest
// real equivalent (best score in those modes specifically) isn't tracked anywhere in the
// existing data model (PlayerStatistics.bestScore is cross-mode), so it's left out rather than
// approximated with a number that doesn't mean what the property name would claim.
// ===================================================================================

fun AnalyticsLogger.setHighestCampaignLevelProperty(level: Int) = setUserProperty("highest_level", level.toString())

fun AnalyticsLogger.setLifetimeXpProperty(xp: Long) = setUserProperty("lifetime_xp", xp.toString())

fun AnalyticsLogger.setLifetimeCoinsProperty(coins: Long) = setUserProperty("lifetime_coins", coins.toString())

fun AnalyticsLogger.setLifetimeStarsProperty(stars: Int) = setUserProperty("lifetime_stars", stars.toString())

fun AnalyticsLogger.setLifetimeLevelsCompletedProperty(count: Int) = setUserProperty("levels_completed", count.toString())

fun AnalyticsLogger.setPreferredGameModeProperty(mode: String) = setUserProperty("preferred_mode", mode)

fun AnalyticsLogger.setLifetimePlayTimeMinutesProperty(minutes: Long) = setUserProperty("play_time_min", minutes.toString())

fun AnalyticsLogger.setTotalSessionsProperty(count: Int) = setUserProperty("total_sessions", count.toString())
