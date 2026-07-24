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
2. In the [Firebase console](https://console.firebase.google.com/), open your project.
3. **Upgrade to the Blaze (pay-as-you-go) plan** if you're still on the free Spark plan — see
   "Which billing plan do you actually need" below. This is the single most important step for
   surviving real traffic; everything else in this file works on either plan, but Spark's daily
   quota will start silently failing writes long before 10,000 users.
4. **Build → Firestore Database → Create database.** Any region is fine (pick one close to your
   expected players); start in **production mode** (the security rules below replace the default
   deny-all with the actual rules this app needs — never ship in Firestore's "test mode," which
   allows unrestricted read/write to anyone).
5. **Build → Authentication → Sign-in method → Anonymous → Enable.**
6. **Firestore Database → Rules tab** — paste the contents of `firestore.rules` (repo root) and
   publish. Without this step, every Firestore read/write fails with a `PERMISSION_DENIED` error;
   `ProgressionRepositoryImpl` and `LeaderboardRepositoryImpl` both catch this and fall
   back/degrade gracefully rather than crash, but nothing will actually be scalable or
   leaderboard-visible until the rules are live.
7. *(Recommended, optional for a first test)* Deploy the Cloud Functions in `functions/` for
   server-side score re-validation — see "Deploying the Cloud Functions" below.
8. Rebuild and run the app. `MemoryArchitectApp.onCreate()` signs in anonymously on first launch;
   Profile/XP/coins/streak now read and write through Firestore, and Profile → Leaderboards shows
   entries after a scored round.

## Which billing plan do you actually need

Firestore's free **Spark** plan gives roughly **50,000 reads, 20,000 writes, and 20,000 deletes
per day, per project** — a hard ceiling, not a soft warning. A single scored round in this app
does on the order of 4-6 Firestore operations (profile read+write, statistics/leaderboard
read+write, an achievement-triggered write here and there). At 10,000 active users playing even a
handful of rounds a day, that ceiling is gone within the first hour, and every write after it
starts failing with `RESOURCE_EXHAUSTED` — which this app handles gracefully (no crash, XP/coins
just stop updating server-side until quota resets), but that's obviously not "smoothly."

**For any real user base beyond a small closed beta, switch the project to the Blaze
(pay-as-you-go) plan** (Firebase console → bottom-left plan badge → "Modify plan"). Blaze has no
hard daily ceiling — you pay for what you use, and Firestore/Auth/Functions all scale to millions
of users on Google's own infrastructure without any code change on this app's side. This is a
billing-account decision only you can make (it requires a payment method on file), not something
fixable in code.

## What works before you do this

Everything except real cross-device sync and leaderboards. Every Firestore call in this app is
guarded by `FirebaseAvailability.isConfigured` and a resolved player uid, and every failure maps
to a graceful `Outcome.Error` — so gameplay, scoring, and the local Statistics dashboard all keep
working. What's different without this setup: XP/coins/streak fall back to the single-shared mock
backend (fine solo, unsafe for more than one real player), and the Leaderboard screen shows a
clear "unavailable" state instead of live rankings.

## Deploying the Cloud Functions

`functions/src/index.ts` re-validates every leaderboard write server-side (score bounds, "a
lifetime total can only increase," a per-player write cooldown) — a second layer on top of
`firestore.rules`, which can bound value *shape* but can't cheaply compare against write history.
This is genuinely deployable, real code, but deploying it requires the Firebase CLI and your own
login, which this environment doesn't have access to:

```bash
npm install -g firebase-tools   # once
firebase login                  # once, opens a browser
cd functions && npm install
firebase deploy --only functions
```

Firestore Security Rules alone (the Rules step above) already give real protection — a client
cannot read or write another player's private profile/reward document, cannot write another
player's public leaderboard entry, and every field is type/range-checked on every write. The Cloud
Functions layer is what closes the remaining gap (history-aware checks, rate limiting) for a
production launch; the app works and is reasonably protected without it, just not as hardened.

## Security hardening: nonces, Monthly boards, Google Sign-In, avatars, Play Integrity

This section covers a later round of hardening on top of everything above: replay-attack
protection for score submissions, a Monthly leaderboard with richer public profile fields (League,
avatar, country, verified badge, win streak, achievement count), a Google Sign-In upgrade path for
anonymous players, and a soft/advisory Play Integrity device-attestation signal. All of it is real,
wired code — what's below is the console/CLI setup each piece still needs, none of which is
achievable from this environment.

### Firestore TTL policies (submissionNonces, deviceIntegrity)

`submissionNonces/{uid}_{nonce}` (replay protection for `FirestoreProgressionRemoteSource
.submitScore` — see that class's doc) and `deviceIntegrity/{uid}` (Play Integrity verdicts) both
self-prune via a Firestore TTL policy rather than a scheduled cleanup job. TTL policies aren't
settable via rules or client code, only the console or `gcloud`:

1. **Firestore Database → Indexes → TTL tab → Create policy.**
2. Collection group `submissionNonces`, timestamp field `createdAtEpochMs`. **This field is stored
   as an epoch-milliseconds integer, not a Firestore `Timestamp`** — the TTL feature requires an
   actual `Timestamp` field, so either add a parallel `expiresAt: FieldValue.serverTimestamp()`-style
   field before enabling TTL, or delete stale nonce docs via a small scheduled Cloud Function
   instead (`db.collection("submissionNonces").where("createdAtEpochMs", "<", cutoff)`, deleted in
   batches) if you'd rather not change the write shape. Either is a small, standalone addition on
   top of what's already here.
3. Same idea for `deviceIntegrity`, timestamp field `verifiedAtEpochMs`, ~24h retention (matches
   `functions/src/index.ts`'s own `INTEGRITY_TTL_MS`).

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

### Play Integrity (soft/advisory only — genuinely can't be verified from this environment)

`DeviceIntegrityChecker`/`verifyDeviceIntegrity` are real, complete code, but Play Integrity only
ever returns real verdicts once:

1. This app is uploaded to **Google Play Console** (at least an internal testing track) under the
   exact package name in `functions/src/index.ts`'s `PACKAGE_NAME` constant (currently
   `com.suman.memoryarchitect` — confirm it matches your actual Play Console listing).
2. The Play Console app is **linked to this Firebase project** (Play Console → your app → App
   integrity, or via Firebase console → Project settings → Integrations → Play Integrity).
3. The Cloud Functions service account has the **Play Integrity API** enabled for this Google Cloud
   project (console.cloud.google.com → APIs & Services → enable "Play Integrity API").
4. `functions/` gets `npm install` (adds the new `googleapis` dependency this feature needs) before
   deploying.

Until all four are done, every call silently fails (see `DeviceIntegrityChecker`'s doc for why
that's the intended, expected behavior, not a bug to chase) and `validateProfileWrite` simply marks
every write `flaggedForReview: true` — a non-blocking marker for future manual/automated review,
never a revert. Nothing in gameplay, scoring, or progression is gated on this signal existing.

## Scaling beyond this initial design

- **Leaderboards**: the Global Leaderboard's "top 100 + a rank via a `count()` aggregation query"
  approach scales to a much larger player base without changes; if you outgrow it, the next step
  is a scheduled Cloud Function that materializes top-N snapshots so live queries never scan the
  full collection, plus `startAfter()` pagination past the first page.
- **Progression**: `playerProfiles/{uid}`/`dailyRewards/{uid}` are single-document-per-player,
  which is exactly Firestore's best-case write pattern (no hotspotting - 10,000 concurrent players
  are 10,000 independent documents, not contention on one shared counter). The transaction-based
  writes automatically retry on contention; no further work is needed for this to hold at 10,000+
  concurrent users once the Blaze plan is active.
- **Level generation** (`LevelApi`/`mock-backend/`): still a REST call to whatever `BASE_URL`
  points at. It's stateless (pure function of a seed, no per-user data), so it's safe at any
  concurrency *if it's actually deployed somewhere real* - the debug build's LAN IP and the
  release build's placeholder `api.memoryarchitect.example.com` are both dev-only stand-ins. For
  10,000+ real users this needs genuine hosting (Cloud Run, a small VM, or porting `generation.js`'s
  logic into a Cloud Function) - this is an infrastructure/deployment decision outside what this
  codebase can do for you, since it requires an account and a hosting choice only you can make.
