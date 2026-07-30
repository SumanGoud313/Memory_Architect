# Leaderboard & scalable-progression setup

This project ships with a complete competitive leaderboard system (Global/Daily/Weekly boards,
player rank tiers, achievements, a statistics dashboard) **and** a real, per-player-scoped
progression backend (XP, coins, streaks, daily login rewards) already wired in code — see
`core/auth/PlayerIdentityManager.kt`, `data/repository/LeaderboardRepositoryImpl.kt`,
`data/repository/FirestoreProgressionRemoteSource.kt`, `ui/screens/leaderboard/`,
`ui/screens/statistics/`. Like the rest of this project's Firebase integration (see
`FIREBASE_SETUP.md`), it stays **inert and non-blocking** until two products are turned on for
this project in the Firebase console. Nothing else in this project can do that step for you — it
requires your own access to the console.

## Why this setup matters more than it might look — read this even if you don't care about leaderboards

Before this setup exists, **every player's XP/coins/streak/daily-reward state is read from and
written to a single, process-wide in-memory object in `mock-backend/index.js`** (`let
playerProfile = {...}`). That file says it outright: *"Dev-only mock backend... In-memory storage,
not for production use."* It was never wrong to build it that way for solo local development —
the bug is only real once a second person plays. With two real devices talking to this backend,
they silently share and overwrite each other's progress. At any real user count, this isn't a
performance problem, it's **data corruption from the first additional player onward**.

`ProgressionRepositoryImpl` now picks between two backends per call
(`activeRemoteSource()`):

- **`FirestoreProgressionRemoteSource`** — a real per-player datastore (`playerProfiles/{uid}`,
  `dailyRewards/{uid}`), used automatically whenever Firestore is configured and a player identity
  has resolved (essentially always, a few seconds after any real launch). Score submissions and
  reward claims run inside a Firestore transaction, so two concurrent writes for the same player
  (two devices signed into the same account, a retried request) can never lose an update or
  double-claim a reward.
- **`MockBackendProgressionRemoteSource`** — the original single-shared-profile mock backend,
  now purely a **local development convenience** for whenever Firestore isn't configured. It is
  never appropriate for more than one simultaneous player.

Completing this setup is what makes the *first* real backend for this app's core progression
actually exist — not just an optimization.

## What you need to enable

`FIREBASE_SETUP.md` already covers connecting a Firebase project (Analytics/Crashlytics/
Performance/Remote Config all "just work" once `google-services.json` exists). This adds two more
products, enabled inside that same project:

1. **Firestore Database** — where `playerProfiles/{uid}`, `dailyRewards/{uid}`, `players/{uid}`,
   and the Daily/Weekly leaderboard entries all live.
2. **Authentication → Anonymous sign-in** — a stable per-device player identity with no login
   screen, no email/password, no username. `PlayerIdentityManager` silently signs every device in
   once on first launch.

## Steps

1. Complete `FIREBASE_SETUP.md` first if you haven't (a real `app/google-services.json` must
   already be in place).
2. In the [Firebase console](https://console.firebase.google.com/), open your project. The free
   **Spark** plan is all this project ever needs - it deliberately runs no Cloud Functions
   anywhere (see the Spark migration report), so there's no Blaze upgrade step here at all.
3. **Build → Firestore Database → Create database.** Any region is fine (pick one close to your
   expected players); start in **production mode** (the security rules below replace the default
   deny-all with the actual rules this app needs — never ship in Firestore's "test mode," which
   allows unrestricted read/write to anyone).
4. **Build → Authentication → Sign-in method → Anonymous → Enable.**
5. **Firestore Database → Rules tab** — paste the contents of `firestore.rules` (repo root) and
   publish. Without this step, every Firestore read/write fails with a `PERMISSION_DENIED` error;
   `ProgressionRepositoryImpl` and `LeaderboardRepositoryImpl` both catch this and fall
   back/degrade gracefully rather than crash, but nothing will actually be scalable or
   leaderboard-visible until the rules are live.
6. Rebuild and run the app. `MemoryArchitectApp.onCreate()` signs in anonymously on first launch;
   Profile/XP/coins/streak now read and write through Firestore, and Profile → Leaderboards shows
   entries after a scored round.

## Spark's daily quota, and whether it's actually a problem

Firestore's free **Spark** plan gives roughly **50,000 reads, 20,000 writes, and 20,000 deletes
per day, per project** — a hard ceiling, not a soft warning. A single scored round in this app
does on the order of 4-6 Firestore operations (profile read+write, statistics/leaderboard
read+write, an achievement-triggered write here and there). At a large enough active user count
playing even a handful of rounds a day, that ceiling could be reached, and every write past it
starts failing with `RESOURCE_EXHAUSTED` — which this app handles gracefully (no crash, XP/coins
just stop updating server-side until quota resets the next day). If you outgrow Spark's quota,
upgrading to Blaze (pay-as-you-go) removes the daily ceiling with zero code changes on this app's
side - a billing-account decision only you can make, not something this project requires from day
one.

## What works before you do this

Everything except real cross-device sync and leaderboards. Every Firestore call in this app is
guarded by `FirebaseAvailability.isConfigured` and a resolved player uid, and every failure maps
to a graceful `Outcome.Error` — so gameplay, scoring, and the local Statistics dashboard all keep
working. What's different without this setup: XP/coins/streak fall back to the single-shared mock
backend (fine solo, unsafe for more than one real player), and the Leaderboard screen shows a
clear "unavailable" state instead of live rankings.

## Why there's no server-side leaderboard re-validation

`firestore.rules` bounds every leaderboard write's *shape* (score/accuracy/completion-time ranges,
identity-field enums) but can't cheaply compare a write against a player's own history (e.g. "a
lifetime total can only increase," a per-player write cooldown) - that would need a Cloud Function,
which this project deliberately doesn't run (see the Spark migration report). Firestore Security
Rules alone still give real protection — a client cannot read or write another player's private
profile/reward document, cannot write another player's public leaderboard entry, and every field is
type/range-checked on every write - just not history-aware. This is an accepted trade-off for
staying on the free Spark plan, not a step you still need to complete.

## Security hardening: nonces, Monthly boards, Google Sign-In, avatars, Play Integrity

This section covers a later round of hardening on top of everything above: replay-attack
protection for score submissions, a Monthly leaderboard with richer public profile fields (League,
avatar, country, verified badge, win streak, achievement count), a Google Sign-In upgrade path for
anonymous players, and a soft/advisory Play Integrity device-attestation signal. All of it is real,
wired code — what's below is the console/CLI setup each piece still needs, none of which is
achievable from this environment.

### Firestore TTL policy (submissionNonces)

`submissionNonces/{uid}_{nonce}` (replay protection for `FirestoreProgressionRemoteSource
.submitScore` — see that class's doc) self-prunes via a Firestore TTL policy rather than a
scheduled cleanup job. TTL policies aren't settable via rules or client code, only the console or
`gcloud`:

1. **Firestore Database → Indexes → TTL tab → Create policy.**
2. Collection group `submissionNonces`, timestamp field `createdAtEpochMs`. **This field is stored
   as an epoch-milliseconds integer, not a Firestore `Timestamp`** — the TTL feature requires an
   actual `Timestamp` field, so either add a parallel `expiresAt: FieldValue.serverTimestamp()`-style
   field before enabling TTL, or periodically delete stale nonce docs some other way
   (`db.collection("submissionNonces").where("createdAtEpochMs", "<", cutoff)`, deleted in batches)
   if you'd rather not change the write shape. This project runs no scheduled Cloud Function to do
   that deletion for you (see the Spark migration report) - either a manual/scripted cleanup or the
   TTL approach above works without one.

### Cloud Storage (custom avatar upload)

1. **Build → Storage → Get started**, same project, any region.
2. **Storage → Rules tab** — paste `storage.rules` (repo root) and publish. Without this, every
   upload from `AvatarUploadRepositoryImpl` fails with `PERMISSION_DENIED`; the app already catches
   this and just leaves the curated glyph-based avatar in place (see `AvatarCatalog.kt`) rather than
   crashing or blocking Settings.

### Google Sign-In upgrade path

1. **Authentication → Sign-in method → Google → Enable.** This generates an OAuth **Web** Client
   ID (not the Android one) in the same screen.
2. Paste that value into `app/build.gradle.kts`'s `GOOGLE_WEB_CLIENT_ID` buildConfigField (currently
   an empty-string placeholder — see the comment right above it). `AccountViewModel`/`AccountSection.kt`
   treat a blank value as "not configured yet" and simply hide the "Sign in with Google" button, so
   the app works identically before and after this step, just without the upgrade option visible.
3. No other code change needed — `PlayerIdentityManager.linkWithGoogle` already upgrades the
   existing anonymous `uid` in place via `linkWithCredential`, preserving every already-earned
   `playerProfiles/{uid}` / `players/{uid}` value rather than starting fresh.

### Play Integrity - removed

`DeviceIntegrityChecker` is now a no-op stub. Decoding a Play Integrity token requires Google's
Play Integrity API, which needs a server-side credential a client can't hold safely - the same
category of check the Google Play Developer API purchase verification needs (see
`BILLING_SETUP.md`). Since this project runs no Cloud Function, there's no Spark-compatible
replacement, so this signal was removed outright rather than left half-wired. It was always
advisory-only and never gated gameplay/scoring/progression, so removing it changes no player-facing
behavior.

## Scaling beyond this initial design

- **Leaderboards**: the Global Leaderboard's "top 100 + a rank via a `count()` aggregation query"
  approach scales to a much larger player base without changes; if you outgrow it, the next step is
  periodically materializing top-N snapshots (a scheduled job of some kind) so live queries never
  scan the full collection, plus `startAfter()` pagination past the first page.
- **Progression**: `playerProfiles/{uid}`/`dailyRewards/{uid}` are single-document-per-player,
  which is exactly Firestore's best-case write pattern (no hotspotting - many concurrent players
  are that many independent documents, not contention on one shared counter). The transaction-based
  writes automatically retry on contention; no further work is needed for this to hold at scale.
- **Level generation**: Classic/Practice/Daily/Weekly Challenge all generate entirely on-device now
  (`domain/generation/LevelGenerator.kt`/`PeriodicChallengeGenerator.kt`) - no network call, no
  hosting, no `BASE_URL` dependency at all for real builds. `mock-backend/` only ever backs local
  dev/testing when Firebase isn't configured.
