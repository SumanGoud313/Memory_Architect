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

/** [attemptNumber] is how many times (including this one) the player has started this exact level
 * number in this app install (see `FrustrationTracker.recordLevelStart`) - the explicit "retry
 * count" signal, rather than something only inferable by counting `level_restarted` rows oneself. */
fun AnalyticsLogger.logLevelRestarted(mode: GameMode, levelNumber: Int, attemptNumber: Int) =
    logEvent("level_restarted", mapOf("mode" to mode.name, "level_number" to levelNumber, "attempt_number" to attemptNumber))

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
// Rewarded-ad economy - the free-tier "X_used" events above cover only free-budget spends;
// these three cover a rewarded-ad grant specifically redeemed, so "how many players lean on ads
// vs. the free budget" is one filtered query away instead of indistinguishable inside one event.
// [rewardedXUsed] is the running per-level count after this grant (1..RewardedAssistLimits' cap),
// so "average rewarded ads per level" and "rewarded ad completion rate" (the two funnel metrics
// the analytics brief asks for) are both derivable by joining these against
// [logRewardedAdResult]'s "rewarded"/"cancelled"/"failed:*" outcomes for the same feature/level,
// rather than needing yet another raw event of their own.
// ===================================================================================

fun AnalyticsLogger.logRewardedHintUsed(mode: GameMode, levelNumber: Int, rewardedHintsUsed: Int) =
    logEvent(
        "rewarded_hint_used",
        mapOf("mode" to mode.name, "level_number" to levelNumber, "rewarded_hints_used" to rewardedHintsUsed),
    )

fun AnalyticsLogger.logRewardedRedoUsed(mode: GameMode, levelNumber: Int, rewardedRedosUsed: Int) =
    logEvent(
        "rewarded_redo_used",
        mapOf("mode" to mode.name, "level_number" to levelNumber, "rewarded_redos_used" to rewardedRedosUsed),
    )

fun AnalyticsLogger.logRewardedRewatchUsed(mode: GameMode, levelNumber: Int, rewardedRewatchesUsed: Int) =
    logEvent(
        "rewarded_rewatch_used",
        mapOf("mode" to mode.name, "level_number" to levelNumber, "rewarded_rewatches_used" to rewardedRewatchesUsed),
    )

/** [feature] is `"hint"`/`"redo"`/`"rewatch"` - fires the instant that helper's rewarded-ad budget
 * (see `RewardedAssistLimits`) is fully spent for this level attempt, pairing with
 * [logLevelAbandonedAfterRewardLimit] to answer "do players who hit the wall keep playing or
 * leave," and on its own, "which levels/features get farmed hardest for ad rewards." */
fun AnalyticsLogger.logRewardLimitReached(feature: String, mode: GameMode, levelNumber: Int) =
    logEvent("reward_limit_reached", mapOf("feature" to feature, "mode" to mode.name, "level_number" to levelNumber))

/** Fires from [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.onCleared] alongside
 * [logLevelQuitMidway], but only when at least one [logRewardLimitReached] already fired earlier
 * in this same attempt - isolates "hit a reward wall and left" from ordinary quits for retention
 * analysis, the "Level abandonment after reaching reward limit" metric the brief asks for. */
fun AnalyticsLogger.logLevelAbandonedAfterRewardLimit(mode: GameMode, levelNumber: Int) =
    logEvent("level_abandoned_after_reward_limit", mapOf("mode" to mode.name, "level_number" to levelNumber))

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
// Missions & Monthly Goals - see MissionCatalog/MissionRepository. [period] is one of
// MissionPeriod's names (DAILY/WEEKLY/MONTHLY/EVENT). [logMissionAssigned]/[logMissionProgressed]
// are logged client-side from MissionsViewModel's own before/after comparison of consecutive
// [com.suman.memoryarchitect.domain.model.ActiveMission] snapshots (assigned = a (missionId,
// periodKey) seen for the first time; progressed = currentCount increased) rather than threading
// new instrumentation through every recordMissionEvent call site across the app - the same
// "reuse existing signals" principle the Missions design itself is built on.
// ===================================================================================

fun AnalyticsLogger.logMissionAssigned(missionId: String, period: String) =
    logEvent("mission_assigned", mapOf("mission_id" to missionId, "period" to period))

fun AnalyticsLogger.logMissionProgressed(missionId: String, period: String, currentCount: Int, targetCount: Int) =
    logEvent(
        "mission_progressed",
        mapOf("mission_id" to missionId, "period" to period, "current_count" to currentCount, "target_count" to targetCount),
    )

fun AnalyticsLogger.logMissionCompleted(missionId: String, period: String) =
    logEvent("mission_completed", mapOf("mission_id" to missionId, "period" to period))

fun AnalyticsLogger.logMissionRewardClaimed(missionId: String, period: String, coinsAwarded: Long, xpAwarded: Long) =
    logEvent(
        "mission_reward_claimed",
        mapOf("mission_id" to missionId, "period" to period, "coins_awarded" to coinsAwarded, "xp_awarded" to xpAwarded),
    )

/** A filtered view of [logMissionCompleted] where `period == "MONTHLY"` would already answer this,
 * but the plan's analytics catalog calls it out as its own event name - logged alongside, not
 * instead of, [logMissionCompleted] for that one mission. */
fun AnalyticsLogger.logMonthlyGoalCompleted(missionId: String) =
    logEvent("monthly_goal_completed", mapOf("mission_id" to missionId))

// ===================================================================================
// Inventory - see InventoryItemKind/InventoryRepository. [source] distinguishes where a grant
// came from (e.g. "mission_claim", "daily_reward") without needing a separate event per source.
// ===================================================================================

fun AnalyticsLogger.logInventoryItemGranted(kind: String, quantity: Int, source: String) =
    logEvent("inventory_item_granted", mapOf("kind" to kind, "quantity" to quantity, "source" to source))

fun AnalyticsLogger.logInventoryItemConsumed(kind: String, quantity: Int) =
    logEvent("inventory_item_consumed", mapOf("kind" to kind, "quantity" to quantity))

// ===================================================================================
// Memory Journey - see MemoryJourneyCatalog/MemoryJourneyRules. [source] is one of
// "level_completed"/"mission_claim" - the two currently-wired point sources (achievement/streak
// bonuses fold into a "level_completed" round's total rather than logging separately, since
// they're only ever awarded as part of that same submitScore call).
// ===================================================================================

fun AnalyticsLogger.logMemoryJourneyPointsEarned(source: String, points: Long) =
    logEvent("memory_journey_points_earned", mapOf("source" to source, "points" to points))

fun AnalyticsLogger.logMemoryJourneyTierReached(tierId: String) =
    logEvent("memory_journey_tier_reached", mapOf("tier_id" to tierId))

// ===================================================================================
// Seasonal Events - see LiveEventCatalog/GetActiveLiveEventUseCase. [logEventCurrencyEarned] is
// deliberately not defined - there is no event-scoped currency in this build (Phase 2's Fragments/
// Chests/Rotating Shop, which Seasonal Events would otherwise layer on top of, is out of scope for
// now - see LiveEventCatalog's own doc), so nothing in the app could ever fire it truthfully.
// [logEventCompleted] is also not defined: detecting "this event's window just ended" reliably
// needs a scheduled check independent of the player opening the app, which doesn't exist here -
// firing it opportunistically from a client screen would systematically undercount by however long
// the player's next visit is delayed past the event's actual end.
// ===================================================================================

fun AnalyticsLogger.logEventStartedClientView(eventId: String) =
    logEvent("event_started_client_view", mapOf("event_id" to eventId))

// ===================================================================================
// Returning player - see ReturningPlayerRules. [gapDays] is whole days since lastPlayedEpochDay.
// ===================================================================================

fun AnalyticsLogger.logReturningPlayerSession(gapDays: Long) =
    logEvent("returning_player_session", mapOf("gap_days" to gapDays))

// ===================================================================================
// Notifications - see core/notifications/. [category] is one of NotificationCategory's names.
// ===================================================================================

fun AnalyticsLogger.logNotificationScheduled(category: String) =
    logEvent("notification_scheduled", mapOf("category" to category))

fun AnalyticsLogger.logNotificationTapped(category: String) =
    logEvent("notification_tapped", mapOf("category" to category))

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
 * micros value (1,000,000 micros = 1 unit of [currencyCode]). [adFormat] defaults to "rewarded"
 * (this event's original, only caller) - [BannerAdView]/`InterstitialAdControllerImpl` pass
 * "banner"/"interstitial" instead so revenue reporting can be broken down by format. */
fun AnalyticsLogger.logAdRevenue(feature: String, valueMicros: Long, currencyCode: String, adFormat: String = "rewarded") =
    logEvent(
        FirebaseAnalytics.Event.AD_IMPRESSION,
        mapOf(
            FirebaseAnalytics.Param.AD_PLATFORM to "admob",
            FirebaseAnalytics.Param.AD_SOURCE to "AdMob",
            FirebaseAnalytics.Param.AD_FORMAT to adFormat,
            FirebaseAnalytics.Param.AD_UNIT_NAME to feature,
            FirebaseAnalytics.Param.CURRENCY to currencyCode,
            FirebaseAnalytics.Param.VALUE to valueMicros / 1_000_000.0,
        ),
    )

// ===================================================================================
// Banner ads - see BannerAdView. [placement] is the screen it's shown on ("mode_select"/"shop"/
// "missions"/"lucky_spin"/"settings"/"achievements").
// ===================================================================================

fun AnalyticsLogger.logBannerImpression(placement: String) = logEvent("banner_impression", mapOf("placement" to placement))

fun AnalyticsLogger.logBannerClicked(placement: String) = logEvent("banner_clicked", mapOf("placement" to placement))

/** The one banner-specific failure signal - unlike rewarded/interstitial, a banner load failure was
 * previously invisible (the composable just renders nothing, silently), so this is what makes
 * "banner load success/failure rate" actually measurable rather than only inferable from a missing
 * [logBannerImpression]. */
fun AnalyticsLogger.logBannerLoadFailed(placement: String) = logEvent("banner_load_failed", mapOf("placement" to placement))

// ===================================================================================
// Interstitial ads - see InterstitialAdController/InterstitialPacingGate. [placement] is the
// natural-transition point the attempt fired from (currently only "level_select_return", but
// kept as a param rather than hardcoded so a future second trigger point doesn't need a new event).
// ===================================================================================

fun AnalyticsLogger.logInterstitialRequested(placement: String) = logEvent("interstitial_requested", mapOf("placement" to placement))

fun AnalyticsLogger.logInterstitialLoaded(placement: String) = logEvent("interstitial_loaded", mapOf("placement" to placement))

fun AnalyticsLogger.logInterstitialLoadFailed(placement: String) = logEvent("interstitial_load_failed", mapOf("placement" to placement))

fun AnalyticsLogger.logInterstitialShown(placement: String) = logEvent("interstitial_shown", mapOf("placement" to placement))

fun AnalyticsLogger.logInterstitialShowFailed(placement: String) = logEvent("interstitial_show_failed", mapOf("placement" to placement))

fun AnalyticsLogger.logInterstitialClicked(placement: String) = logEvent("interstitial_clicked", mapOf("placement" to placement))

fun AnalyticsLogger.logInterstitialDismissed(placement: String) = logEvent("interstitial_dismissed", mapOf("placement" to placement))

/** Fires whenever [InterstitialPacingGate] decides *not* to show one - never a failure, just the
 * pacing rules doing their job. [reason] is one of "disabled"/"emergency_disabled"/"cooldown"/
 * "cooldown_after_rewarded"/"session_cap"/"daily_cap"/"first_session_protected"/
 * "first_levels_protected" - lets you tell "ads are working but paced out" apart from "ads are
 * broken" in the funnel. */
fun AnalyticsLogger.logInterstitialSkipped(placement: String, reason: String) =
    logEvent("interstitial_skipped", mapOf("placement" to placement, "reason" to reason))

// ===================================================================================
// Ad load latency - one event shape shared by rewarded/interstitial/banner rather than a
// per-format duplicate, since "how long did loading take, and did it succeed" is the same
// question for all three. [adFormat] is "rewarded"/"interstitial"/"banner"; [placement] is the
// feature/placement id each format already tags its other events with.
// ===================================================================================

fun AnalyticsLogger.logAdLoadLatency(adFormat: String, placement: String, latencyMs: Long, success: Boolean) =
    logEvent(
        "ad_load_latency",
        mapOf("ad_format" to adFormat, "placement" to placement, "latency_ms" to latencyMs, "success" to success),
    )

// ===================================================================================
// Remove Ads conversion - see BillingManagerImpl.applyPurchases. Fires only for a fresh purchase
// completing in this session (never for the silent re-verification every app start already does,
// and never for a Restore Purchases finding an existing entitlement) - the distinction "Remove Ads
// conversion rate" actually needs. Uses Firebase's own recommended `purchase` event schema, the
// IAP-revenue equivalent of [logAdRevenue]'s `ad_impression` schema for ad revenue, so both feed
// the same "average revenue per active user" reporting Firebase computes automatically.
// ===================================================================================

fun AnalyticsLogger.logRemoveAdsPurchased(priceMicros: Long, currencyCode: String) =
    logEvent(
        FirebaseAnalytics.Event.PURCHASE,
        mapOf(
            FirebaseAnalytics.Param.ITEM_ID to "remove_ads_lifetime",
            FirebaseAnalytics.Param.CURRENCY to currencyCode,
            FirebaseAnalytics.Param.VALUE to priceMicros / 1_000_000.0,
        ),
    )

// ===================================================================================
// Unified Billing funnel - see core.billing.BillingManager/BillingManagerImpl. One shared shape
// across every product this app sells (Remove Ads, every Premium Collection, and any future
// consumable/subscription), so a new product's funnel is visible for free rather than needing its
// own event set the way logRemoveAdsPurchased above still does (kept as-is, name locked in by prior
// dashboards - logPurchaseSuccess/logCollectionPurchased fire alongside it on a Remove Ads/Collection
// grant respectively, they are not replacements for it).
// ===================================================================================

fun AnalyticsLogger.logShopViewed() = logEvent("shop_viewed")

fun AnalyticsLogger.logProductViewed(productId: String) = logEvent("product_viewed", mapOf("product_id" to productId))

fun AnalyticsLogger.logPurchaseStarted(productId: String) = logEvent("purchase_started", mapOf("product_id" to productId))

fun AnalyticsLogger.logPurchaseSuccess(productId: String) = logEvent("purchase_success", mapOf("product_id" to productId))

fun AnalyticsLogger.logPurchaseCancelled(productId: String) = logEvent("purchase_cancelled", mapOf("product_id" to productId))

/** [reason] is a short machine-readable tag (a [com.suman.memoryarchitect.core.billing.BillingFailureReason]
 * name, `"signature_invalid"`, `"unknown_product"`, `"not_ready"`, or a raw Play `BillingResponseCode`
 * name) - never a user-facing message, same "plain reason string, not display text" convention
 * [logInterstitialSkipped] already uses. */
fun AnalyticsLogger.logPurchaseFailed(productId: String, reason: String) =
    logEvent("purchase_failed", mapOf("product_id" to productId, "reason" to reason))

fun AnalyticsLogger.logPurchasePending(productId: String) = logEvent("purchase_pending", mapOf("product_id" to productId))

/** Fires once per product a [com.suman.memoryarchitect.core.billing.BillingManager.restorePurchases]/
 * automatic-startup query re-grants - a purchase that was already owned and is simply being
 * re-confirmed, never a fresh conversion (that's [logPurchaseSuccess]/[logRemoveAdsPurchased]/
 * [logCollectionPurchased]'s job). */
fun AnalyticsLogger.logPurchaseRestored(productId: String) = logEvent("purchase_restored", mapOf("product_id" to productId))

/** Fires once per explicit "Restore Purchases" tap, regardless of how many products it found -
 * [restoredCount] is how many were actually re-granted, `0` being a legitimate, common outcome
 * (nothing to restore), not an error. */
fun AnalyticsLogger.logRestoreCompleted(restoredCount: Int) = logEvent("restore_completed", mapOf("restored_count" to restoredCount))

/** Fires alongside [logPurchaseSuccess] specifically for a
 * [com.suman.memoryarchitect.domain.model.BillingEntitlementKind.COSMETIC_COLLECTION] grant - the
 * Premium Collection equivalent of [logRemoveAdsPurchased], using the same Firebase `purchase`
 * revenue schema so Collection revenue feeds the same ARPU reporting ad revenue and Remove Ads
 * already do. */
fun AnalyticsLogger.logCollectionPurchased(productId: String, priceMicros: Long, currencyCode: String) =
    logEvent(
        FirebaseAnalytics.Event.PURCHASE,
        mapOf(
            FirebaseAnalytics.Param.ITEM_ID to productId,
            FirebaseAnalytics.Param.CURRENCY to currencyCode,
            FirebaseAnalytics.Param.VALUE to priceMicros / 1_000_000.0,
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
