# Analytics Events

The full event/parameter/user-property catalog this app sends through
`core/analytics/AnalyticsLogger`, plus recommended Firebase reports, funnels, and audiences built
on top of it. See `FIREBASE_SETUP.md` for how to actually connect a Firebase project — until then,
every event below still logs to Logcat (`adb logcat -s Analytics`) in debug builds, or to the
debug-only Analytics Dashboard (Settings → "Analytics Dashboard (debug)").

Every typed logging function lives in `core/analytics/AnalyticsEvents.kt` — call sites never build
an event name/param map by hand.

## What Firebase already gives you for free

A few things are deliberately **not** custom events here, because re-logging them would be a
banned duplicate on top of Firebase's own automatic collection:

| What | Where it comes from |
|---|---|
| `first_open`, `session_start`, `app_update` | Automatically logged the instant the SDK is configured |
| App version, OS version, device model, screen size, country, language | Automatic default dimensions on every event |
| DAU / WAU / MAU | Computed by Firebase from automatic collection — see [Recommended Reports](#recommended-custom-reports) |
| Average session duration, sessions/user | Computed by Firebase from automatic collection + [`session_ended`](#session_ended) |
| D1 / D7 / D30 retention | Computed by Firebase's built-in Retention report |

## Player lifecycle

### `app_foregrounded`
Logged from `AppLifecycleTracker.onStart` — app-wide (not per-Activity/per-screen), fires once per
foreground transition.
No parameters.

### `app_backgrounded`
Logged from `AppLifecycleTracker.onStop`.
No parameters.

### `session_ended`
Logged from `GameplayViewModel.onCleared` — a *gameplay* session (time spent on the Gameplay
screen for one attempt), not the app-wide session `app_foregrounded`/`app_backgrounded` track.
| Param | Type | Notes |
|---|---|---|
| `mode` | string | `GameMode` name, nullable |
| `duration_ms` | long | Wall-clock time this ViewModel instance was alive |

## Screens

### `screen_view` *(Firebase's own standard event/params)*
Compose Navigation has no Fragment/Activity boundary per screen for Firebase's automatic
screen_view tracking to attach to (this whole app is one Activity), so every
`composable<Route.X>{}` block logs this manually via `ScreenViewTrackerViewModel`.
| Param | Type | Values |
|---|---|---|
| `screen_name` | string | `home`, `mode_select`, `level_select`, `profile`, `settings`, `gameplay`, `analytics_dashboard` |

## Gameplay flow

### `mode_selected`
Logged from `ModeSelectViewModel.onModeSelected`, on every tap of a mode card.
| Param | Type |
|---|---|
| `mode` | string (`GameMode` name) |

### `level_started`
Logged from `GameplayViewModel.startMemorizePhase` — the moment a level attempt truly begins
(after any tutorial/mechanic-intro overlay is dismissed, never before).
| Param | Type | Notes |
|---|---|---|
| `mode` | string | |
| `level_number` | int | |
| `room_theme` | string | Scene type (`kitchen`, `bedroom`, …) |
| `object_count` | int | Targets + distractors shown |
| `object_density` | float | `object_count` / that room's total slot capacity |
| `rotation_enabled` | bool | True if any object in this scene is rotated |
| `order_mode_enabled` | bool | |
| `memorize_timer_ms` | long | |

### `level_restarted`
Logged alongside `level_started` when `FrustrationTracker` reports this level number has already
been started earlier this session (a genuinely new `GameplayViewModel` instance is created on
every replay, so this is tracked in a session-scoped singleton, not on the ViewModel itself).
| Param | Type |
|---|---|
| `mode` | string |
| `level_number` | int |

### `memorize_started`
Logged alongside `level_started`, same parameter shape — kept as its own event because "started
the level" and "started the memorize phase" are useful to distinguish once Rewatch replays start
re-entering Memorize without re-triggering `level_started`.

### `memorize_finished`
Logged when the memorize countdown runs to its natural end. A player who quits mid-Memorize
produces [`level_quit_midway`](#level_quit_midway) instead, never this.
| Param | Type |
|---|---|
| `mode` | string |
| `level_number` | int |

### `rebuild_started`
Logged when Reconstruct begins (the "Rebuild the scene" phase).
Same params as `memorize_finished`.

### `submit_pressed`
Logged only for a genuine player tap of Submit (never on timer auto-submit — see `time_expired`).
| Param | Type |
|---|---|
| `mode` | string |
| `level_number` | int |
| `objects_placed` | int |

### `time_expired`
Logged when the Reconstruct countdown reaches zero and auto-submits.
Same params as `submit_pressed`.

### `level_completed`
The single most important event in this catalog — nearly every business question in
[Recommended Reports](#recommended-custom-reports) is built from this one. Logged once per
resolved attempt: immediately for Practice (no server round-trip to wait on), or after the score
submission resolves for scored modes (so `xp_awarded`/`coins_awarded` are real, not always-null).
| Param | Type | Notes |
|---|---|---|
| `mode`, `level_number`, `room_theme`, `object_count`, `object_density`, `rotation_enabled`, `order_mode_enabled`, `memorize_timer_ms` | — | Same as `level_started` |
| `stars` | int | |
| `accuracy` | float | Scene accuracy 0–1 |
| `passed` | bool? | Only meaningful for Classic; null everywhere else |
| `time_taken_ms` | long | |
| `mistakes_count` | int | Objects placed with `positionAccuracy < 1.0` |
| `hints_used`, `redos_used`, `rewatches_used` | int | This attempt's totals |
| `xp_awarded`, `coins_awarded` | long? | Null for Practice (never awards them) or if the submission never resolved |

**"Level failed" is not a separate event.** It's `level_completed` filtered to `passed = false` —
logging a second near-duplicate event for the same moment would violate "avoid duplicate events,"
and a single event means failure-rate and completion-rate analysis can never disagree with each
other.

### `level_quit_midway`
Logged from `GameplayViewModel.onCleared` whenever the round never reached Finished — covers both
"player backed out" and "process/Activity torn down mid-round" the same way.
| Param | Type |
|---|---|
| `mode` | string |
| `level_number` | int |
| `phase` | string (`memorize` / `hidden` / `reconstruct`) |
| `elapsed_ms` | long |

## Assist features

### `hint_used` / `redo_used` / `rewatch_used`
Logged on every individual use (not just when the budget runs out).
| Param | Type |
|---|---|
| `mode` | string |
| `level_number` | int |

## Frustration detection

### `frustration_signal`
One consolidated event with a `signal_type` param, rather than eight near-duplicate event names —
"which levels are difficult" becomes one filtered/grouped query instead of eight separate ones.
| Param | Type | Notes |
|---|---|---|
| `signal_type` | string | See below |
| `mode` | string | |
| `level_number` | int | |
| `detail` | string? | Short free-form context (e.g. the retry count) |

`signal_type` values and their triggers:

| Value | Fires when |
|---|---|
| `repeated_retry` | Same level started ≥3 times this session |
| `quit_during_memorize` | Quit while `phase = memorize` |
| `quit_during_reconstruct` | Quit while `phase = reconstruct` |
| `excessive_hints` / `excessive_redo` / `excessive_rewatch` | That assist used ≥3 times on one attempt |
| `long_duration` | Reconstruct took >90% of the level's time limit |
| `repeated_failures` | Classic level failed 3 consecutive attempts in a row |

## Feature usage

### `achievement_unlocked`
Logged once per achievement, from the score-submission response (`ScoreSubmissionResult
.newlyUnlockedAchievements`) — never re-derived or re-checked elsewhere, so it can't double-fire.
| Param | Type |
|---|---|
| `achievement_id` | string (`AchievementId` name) |

### `haptics_toggled`
| Param | Type |
|---|---|
| `enabled` | bool |

*(Practice/Daily/Weekly/Classic usage, Hint/Redo/Rewatch usage, and Settings/Achievements-opened
are all derivable from `mode_selected`/`level_started`/`hint_used`/etc. and `screen_view` — not
duplicated as their own events.)*

## Monetization

Two layers, deliberately not duplicates of each other:

- **Ad-funnel events** (from `RewardedAdControllerImpl`, the raw AdMob SDK callbacks) — show
  *where* in the funnel an attempt succeeded or dropped off.
- **`rewarded_ad_result`** (from `RewardedAdFlow`, one layer up) — the *feature's* perspective:
  did watching an ad ultimately grant the reward.

### `ad_requested` → `ad_loaded` | `ad_load_failed` → `ad_shown` → `ad_closed` | `ad_show_failed`
| Param | Type | Applies to |
|---|---|---|
| `feature` | string (`hint`/`redo`/`rewatch`) | All |
| `earned_reward` | bool | `ad_closed` only |

### `rewarded_ad_result`
| Param | Type | Notes |
|---|---|---|
| `feature` | string | |
| `result` | string | `rewarded`, `cancelled`, or `failed:<reason>` |

### `ad_impression` *(Firebase's own standard AdMob-revenue event/params — not invented here)*
Logged from `OnPaidEventListener`, once per ad impression that generated revenue.
| Param | Firebase constant |
|---|---|
| `ad_platform` | `"admob"` |
| `ad_source` | `"AdMob"` |
| `ad_format` | `"rewarded"` |
| `ad_unit_name` | the feature (`hint`/`redo`/`rewatch`) |
| `currency`, `value` | From AdMob's `AdValue` |

### `memory_warning`
Logged from `MemoryArchitectApp.onTrimMemory`.
| Param | Type |
|---|---|
| `level` | string (Android `TRIM_MEMORY_*` constant name) |

## Performance (not events — Performance Monitoring traces)

`core/analytics/PerformanceTracer.kt`. App startup, screen rendering, and network requests are
normally captured **automatically** by the Performance Monitoring Gradle plugin — no custom code.

**Currently disabled** (`enableFirebasePerfPlugin = false` in `app/build.gradle.kts`): that plugin
doesn't yet support Android Gradle Plugin 9.x — see FIREBASE_SETUP.md's "Known limitation"
section for the open upstream issue. Automatic screen-rendering/network-request traces aren't
being collected until that's re-enabled. The custom `level_load` trace (around
`GenerateLevelUseCase` in `GameplayViewModel.loadLevel` — the one gap automatic instrumentation
couldn't see anyway, since Practice/Classic never touch the network) is unaffected — it's a plain
SDK call, not something the plugin provides, and works today.

## Crashes (not events — Crashlytics)

Fatal (uncaught) exceptions are captured automatically once Crashlytics is configured — nothing to
log. Non-fatals: `ErrorMapper` (the single choke point every repository's catch block already
flows through) calls `CrashReporter.recordException` for `AppError.Server`/`AppError.Unknown` —
deliberately **not** for `AppError.Network` (a user being offline is routine, not a bug; logging
it as a non-fatal would drown out anything actually worth investigating).

## User properties

Firebase caps property names at 24 characters and values at 36 — every one below stays well under
both. Set from `ProfileViewModel` (Profile is a natural, low-frequency sync point) except
`preferred_mode`/`total_sessions`/`play_time_min`, set where their underlying counters live.

| Property | Type | Source |
|---|---|---|
| `highest_level` | string (int) | `LevelCampaignProgress.maxUnlockedLevel` |
| `lifetime_xp` | string (long) | `PlayerProfile.xp` |
| `lifetime_coins` | string (long) | `PlayerProfile.coins` |
| `lifetime_stars` | string (int) | Sum of `LevelCampaignProgress.bestStars` |
| `levels_completed` | string (int) | `PlayerStatistics.gamesPlayed` |
| `preferred_mode` | string | Most-picked `GameMode` this device has ever selected (`UserPreferencesDataStore`) |
| `play_time_min` | string (long) | Lifetime foreground minutes (`UserPreferencesDataStore`) |
| `total_sessions` | string (int) | App-foreground count (`UserPreferencesDataStore`) |

**Not tracked:** "Highest Daily Challenge" / "Highest Weekly Challenge." Unlike Classic, those
modes have no persistent numbered-level concept to hold a "highest" of, and the closest real
equivalent — best score specifically in that mode — isn't tracked anywhere in the existing data
model (`PlayerStatistics.bestScore` is cross-mode). Left out rather than approximated with a
number that wouldn't mean what the property name claims.

---

## Recommended custom reports

Build these in Firebase Analytics → Explore → Free-form:

| Report | Dimensions | Metrics | Answers |
|---|---|---|---|
| Level difficulty | `level_number` (from `level_completed`) | Count where `passed=false` ÷ total | *Which level has the highest failure rate?* |
| Level abandonment | `level_number` (from `level_quit_midway`) | Event count | *Which level has the highest quit rate?* |
| Level pacing | `level_number` | Average `time_taken_ms` (from `level_completed`) | *Which level has the longest completion time?* |
| Assist usage by level | `level_number` | Count of `hint_used` / `redo_used` / `rewatch_used` | *Which level uses the most hints/redo/rewatch?* |
| Room popularity | `room_theme` (from `level_started`) | Event count | *Which room theme is most popular?* |
| Mode popularity | `mode` (from `mode_selected`) | Event count | *Which game mode is played the most?* |
| Session length | — | Average of `session_ended.duration_ms` | *How long do users play?* |
| Progress stalls | `highest_level` user property | User count per value | *Where do most players stop progressing?* |
| Rewarded ad performance | `feature` (from `rewarded_ad_result`) | Count by `result` | *Which rewarded ads are watched most?* |
| Rare achievements | `achievement_id` (from `achievement_unlocked`) | Unique user count | *Which achievements are rarely unlocked?* |

## Recommended funnels

Firebase Analytics → Explore → Funnel exploration:

1. **Core gameplay loop:** `level_started` → `memorize_finished` → `rebuild_started` →
   (`submit_pressed` OR `time_expired`) → `level_completed`. Drop-off at each step shows exactly
   where players disengage within a single attempt.
2. **Rewarded ad funnel:** `ad_requested` → `ad_loaded` → `ad_shown` → `ad_closed` (with
   `earned_reward=true`). Drop-off distinguishes "ads aren't loading" from "players back out once
   they see the prompt."
3. **Campaign completion:** `level_started` filtered to `mode=CLASSIC`, stepped by `level_number`
   1 → 100. *How many users complete all 100 levels?* is the funnel's final-step reach.

## Recommended audiences

Firebase Analytics → Audiences:

- **At risk of churn:** `session_ended` not logged in the last 7 days, `total_sessions ≥ 3` (an
  established player who's gone quiet, not a first-session bounce).
- **Frustrated players:** `frustration_signal` count ≥ 3 in the last 7 days — a re-engagement or
  in-app difficulty-hint campaign candidate.
- **High-value ad watchers:** `rewarded_ad_result` with `result=rewarded` count ≥ 10 lifetime —
  candidates for a rewarded-ad-adjacent monetization test.
- **Campaign completionists:** `highest_level` user property = 100 — candidates for endgame
  content or a satisfaction survey.
- **Daily/Weekly regulars:** `mode_selected` with `mode=DAILY_CHALLENGE` on ≥5 distinct days in
  the last 7 — your most engaged recurring-content audience.
