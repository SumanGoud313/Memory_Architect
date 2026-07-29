/**
 * Server-side leaderboard validation for Memory Architect.
 *
 * NOT deployed by this repository automatically - see ../LEADERBOARD_SETUP.md for how to deploy
 * this via the Firebase CLI (`firebase deploy --only functions`), which requires your own Firebase
 * CLI login/credentials that this codebase has no access to.
 *
 * Why this exists alongside firestore.rules: Security Rules can bound *shape* (is this a number,
 * is it in a plausible range) but can't cheaply compare against a player's own write history or
 * apply a cooldown, and can't run arbitrary logic. This function is the second, authoritative
 * layer - it re-validates every write with admin privileges immediately after it lands, and
 * reverts (deletes) anything that shouldn't have gotten through, so even a forged write that
 * somehow satisfies firestore.rules (a bug in the rules, a future rules regression) still can't
 * permanently corrupt the leaderboard.
 *
 * The plausibility bounds here intentionally mirror domain/security/LeaderboardAntiCheat.kt's
 * constants - keep both in sync, the same "mirrors X" convention already used between
 * mock-backend/progression.js and the Kotlin domain layer for progression math.
 */

import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { google } from "googleapis";
import { SHOP_CATALOG_PRICES, SPIN_COIN_OUTCOME_AMOUNTS, SPIN_DUPLICATE_REFUND_FRACTION } from "./shopCatalog";
import { PREMIUM_GRANTED_SKUS, PREMIUM_PRODUCT_GRANTS } from "./premiumCatalog";
import { activeMissionIds, definitionFor, periodFor, MAX_MISSION_INVENTORY_GRANT_PER_KIND } from "./missions";
import { nextDailyRewardCycleDay, canClaimDailyReward } from "./dailyReward";

admin.initializeApp();
const db = admin.firestore();

// Must match the app's actual Play Console package listing once this project is linked there -
// see LEADERBOARD_SETUP.md's "Play Integrity" section. Decoding a token for the wrong package
// name always fails, so this is one more thing that genuinely can't be verified from this
// environment (no Play Console access) - it's set to the app's applicationId as the correct
// starting value, but confirm it against the real Play Console listing before deploying.
const PACKAGE_NAME = "com.suman.memoryarchitect";
const INTEGRITY_TTL_MS = 24 * 60 * 60_000; // 24h - a verdict older than this is treated as stale
// Mirrors BillingManagerImpl.kt's own REMOVE_ADS_PRODUCT_ID - the legacy one-time "Remove Ads"
// product, verified by verifyRemoveAdsPurchase below.
const REMOVE_ADS_PRODUCT_ID = "remove_ads_lifetime";

// Mirrors ScoringRules.Default (domain/scoring/ScoringRules.kt) - the same numbers
// LeaderboardAntiCheat.kt's maxPlausibleScore uses, applied per-object rather than per-level here
// since this function doesn't have the level spec (object count) available - it validates against
// a fixed, generously-large per-round ceiling instead (see MAX_PLAUSIBLE_PERIODIC_SCORE).
const MAX_PLAUSIBLE_PERIODIC_SCORE = 5000; // one Daily/Weekly/Monthly round, not a lifetime total
const MAX_PLAUSIBLE_ROUND_DURATION_MS = 30 * 60_000; // 30 minutes
// Global's bestScore is a running max of one round's score (never a lifetime sum - see
// GlobalLeaderboardStats.kt's doc for why a running max was chosen over a cumulative total), so it
// shares the exact same per-round ceiling as the Daily/Weekly/Monthly boards.
const MAX_PLAUSIBLE_GLOBAL_SCORE = MAX_PLAUSIBLE_PERIODIC_SCORE;
const MIN_RESUBMISSION_INTERVAL_MS = 3_000; // reject writes from the same player faster than this

// Mirrors ProgressionRules.Default (domain/progression/ProgressionRules.kt) - a generous ceiling on
// how much xp/coins ONE playerProfiles/{uid} write can plausibly grant, well above any real
// single-round result, but far below the account-wide bounds firestore.rules already enforces
// (0 to 1,000,000,000) - this is what actually stops a client from jumping straight to that ceiling
// in one forged write.
const MAX_PLAUSIBLE_XP_GAIN_PER_WRITE = 5_000;
const MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE = 2_000;

// Mirrors StreakRules.Default (domain/model/StreakRules.kt) - exactly one Streak Shield can ever
// be granted (a streak-milestone crossing or a Daily Reward bonus day) or consumed (covering one
// missed day) per single submitScore/claimDailyReward write - never both in the same write, and
// never more than one of either.
const MAX_PLAUSIBLE_SHIELD_GAIN_PER_WRITE = 1;
const MAX_PLAUSIBLE_SHIELD_LOSS_PER_WRITE = 1;

// Mirrors MemoryJourneyRules.Default (domain/model/MemoryJourneyRules.kt) - a generous ceiling
// above the single largest per-write grant that table can produce (a perfect Classic round that
// also crosses a streak milestone and unlocks a couple of achievements, or a Monthly mission
// claim), well below the account-wide bound firestore.rules enforces.
const MAX_PLAUSIBLE_JOURNEY_POINTS_GAIN_PER_WRITE = 500;

// A legitimate decrease now exists (a Point Shop purchase/spin, see FirestoreShopRemoteSource.kt)
// - this bounds it the same generous-ceiling way MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE already bounds
// increases, well above the priciest single catalog item/spin cost (see shopCatalog.ts) but far
// below the account-wide bound firestore.rules enforces. validatePurchaseReceipt below is the
// second, independent check - this one alone doesn't correlate the decrease to a specific receipt.
const MAX_PLAUSIBLE_COINS_SPEND_PER_WRITE = 4_000;

// Every distinct client operation that writes playerProfiles/{uid} - mirrored against the
// `lastWriteSource` string each one now tags its write with (FirestoreProgressionRemoteSource.submitScore/
// claimDailyReward, FirestoreMissionRemoteSource.claimMissionReward, FirestoreShopRemoteSource.purchase/spin),
// so validateProfileWrite below can rate-limit each independently instead of one shared bucket.
const PROFILE_WRITE_SOURCES = new Set([
  "submit_score",
  "claim_daily_reward",
  "claim_mission_reward",
  "shop_purchase",
  "shop_spin",
  "mystery_chest_open",
  "xp_boost_consumed",
  "claim_mission_category_bonus",
  "mission_unlock_all",
]);

/** Re-validates every `players/{uid}` write (the Global Leaderboard's source document). */
export const validateGlobalStats = onDocumentWritten("players/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return; // a delete - nothing to validate

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  if (typeof data?.bestScore !== "number" || data.bestScore < 0 || data.bestScore > MAX_PLAUSIBLE_GLOBAL_SCORE) {
    problems.push("bestScore out of range");
  }
  if (typeof data?.averageAccuracy !== "number" || data.averageAccuracy < 0 || data.averageAccuracy > 1) {
    problems.push("averageAccuracy out of range");
  }
  if (typeof data?.highestLevel !== "number" || data.highestLevel < 1 || data.highestLevel > 100) {
    problems.push("highestLevel out of range");
  }

  const before = event.data?.before;
  if (before?.exists) {
    const previousScore = before.data()?.bestScore ?? 0;
    // A running max should never go down - a decrease means either a client bug or a forged write,
    // never a legitimate outcome of playing more rounds (a worse round just leaves it unchanged).
    if (typeof data?.bestScore === "number" && data.bestScore < previousScore) {
      problems.push(`bestScore decreased (${previousScore} -> ${data.bestScore})`);
    }
  }

  if (await isRateLimited(uid, "global")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid players/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

/** Re-validates every `playerProfiles/{uid}` write - the actual XP/coins economy
 * FirestoreProgressionRemoteSource.submitScore/claimDailyReward write to, and (unlike
 * `players/{uid}`/the periodic leaderboard entries above) the one collection that had **no**
 * server-side re-validation at all before this function existed. firestore.rules' isValidProfile
 * already bounds shape/range and (via updatedAtEpochMs vs request.time) write staleness; this adds
 * the one thing rules can't do cheaply - comparing against the player's own previous xp/coins. */
export const validateProfileWrite = onDocumentWritten("playerProfiles/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const before = event.data?.before;
  if (before?.exists) {
    const previousXp = before.data()?.xp ?? 0;
    const previousCoins = before.data()?.coins ?? 0;
    // xp/coins are lifetime-cumulative counters (see PlayerProfile.kt) - like bestScore above
    // (a running max, monotonic for a different reason), they should only ever go up, and never by
    // more than one write plausibly grants.
    if (typeof data?.xp === "number" && data.xp < previousXp) {
      problems.push(`xp decreased (${previousXp} -> ${data.xp})`);
    } else if (typeof data?.xp === "number" && data.xp - previousXp > MAX_PLAUSIBLE_XP_GAIN_PER_WRITE) {
      problems.push(`xp increased implausibly (${previousXp} -> ${data.xp})`);
    }
    if (typeof data?.coins === "number" && data.coins < previousCoins) {
      // A decrease is now legitimate (Point Shop purchase/spin) - bounded, not banned. See
      // MAX_PLAUSIBLE_COINS_SPEND_PER_WRITE's comment; validatePurchaseReceipt below is the
      // second, independent check that ties this to an actual catalog-priced receipt.
      if (previousCoins - data.coins > MAX_PLAUSIBLE_COINS_SPEND_PER_WRITE) {
        problems.push(`coins decreased implausibly (${previousCoins} -> ${data.coins})`);
      }
    } else if (typeof data?.coins === "number" && data.coins - previousCoins > MAX_PLAUSIBLE_COINS_GAIN_PER_WRITE) {
      problems.push(`coins increased implausibly (${previousCoins} -> ${data.coins})`);
    }

    const previousShields = before.data()?.streakShields ?? 0;
    if (typeof data?.streakShields === "number") {
      const shieldDelta = data.streakShields - previousShields;
      if (shieldDelta > MAX_PLAUSIBLE_SHIELD_GAIN_PER_WRITE) {
        problems.push(`streakShields increased implausibly (${previousShields} -> ${data.streakShields})`);
      } else if (-shieldDelta > MAX_PLAUSIBLE_SHIELD_LOSS_PER_WRITE) {
        problems.push(`streakShields decreased implausibly (${previousShields} -> ${data.streakShields})`);
      }
    }

    // journeyPoints is lifetime-cumulative, like xp/coins above - it should only ever go up.
    const previousJourneyPoints = before.data()?.journeyPoints ?? 0;
    if (typeof data?.journeyPoints === "number") {
      if (data.journeyPoints < previousJourneyPoints) {
        problems.push(`journeyPoints decreased (${previousJourneyPoints} -> ${data.journeyPoints})`);
      } else if (data.journeyPoints - previousJourneyPoints > MAX_PLAUSIBLE_JOURNEY_POINTS_GAIN_PER_WRITE) {
        problems.push(`journeyPoints increased implausibly (${previousJourneyPoints} -> ${data.journeyPoints})`);
      }
    }
  }

  // Scoped per write-source (see PROFILE_WRITE_SOURCES) rather than one shared "profile" bucket -
  // without this, two *different* legitimate actions landing within the cooldown window (e.g.
  // finishing a level, then immediately buying a shop item with the coins just earned) would
  // revert the second write's entire profile doc, including a coin/xp change that had nothing to
  // do with the first write. lastWriteSource is client-supplied, so it's checked against a known
  // set rather than trusted outright: an unrecognized/missing value falls back to the original
  // shared "profile" scope, preserving the old conservative behavior for anything that doesn't
  // use this pattern (a stale client, a forged write omitting the field).
  const source = typeof data?.lastWriteSource === "string" && PROFILE_WRITE_SOURCES.has(data.lastWriteSource)
    ? data.lastWriteSource
    : "unknown";
  if (await isRateLimited(uid, `profile:${source}`)) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid playerProfiles/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
    return;
  }

  // Advisory-only device attestation (see verifyDeviceIntegrity below) - never reverts a write
  // solely on a missing/stale/failed verdict, only marks it for later review. A hard gate here
  // would break every debug/emulator build and anyone before Play Console linking is complete,
  // which is not a state this app's own dev/test flow can avoid triggering. Guarded against
  // infinite retrigger: only writes flaggedForReview when its value would actually change, so the
  // one resulting re-invocation of this same function sees nothing left to change and no-ops.
  const integritySnapshot = await db.collection("deviceIntegrity").doc(uid).get();
  const integrityData = integritySnapshot.data();
  const isFresh = typeof integrityData?.verifiedAtEpochMs === "number" && Date.now() - integrityData.verifiedAtEpochMs < INTEGRITY_TTL_MS;
  const shouldFlag = !integritySnapshot.exists || !isFresh || integrityData?.passed !== true;
  if (data?.flaggedForReview !== shouldFlag) {
    await after.ref.update({ flaggedForReview: shouldFlag });
  }
});

/** Callable from the client (`DeviceIntegrityChecker.kt`) with a Play Integrity token obtained via
 * `IntegrityManagerFactory` - decodes and verifies it server-side against Google's Play Integrity
 * API (the token itself is opaque/signed, a client can't fake a passing verdict by construction)
 * and records the verdict at `deviceIntegrity/{uid}` for [validateProfileWrite] above to read.
 * Requires this Firebase project's Play Integrity API to be enabled and this app's Play Console
 * listing to be linked - see LEADERBOARD_SETUP.md. Untestable from this environment; will throw
 * `internal` for any caller until that setup is complete, which the client already treats as a
 * soft/best-effort failure (see DeviceIntegrityChecker's doc). */
export const verifyDeviceIntegrity = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign-in required");
  }
  const uid = request.auth.uid;
  const integrityToken = request.data?.integrityToken;
  if (typeof integrityToken !== "string" || integrityToken.length === 0) {
    throw new HttpsError("invalid-argument", "integrityToken is required");
  }

  try {
    const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/playintegrity"] });
    const playintegrity = google.playintegrity({ version: "v1", auth });
    const response = await playintegrity.v1.decodeIntegrityToken({
      packageName: PACKAGE_NAME,
      requestBody: { integrityToken },
    });

    const verdict = response.data.tokenPayloadExternal;
    const appRecognitionVerdict = verdict?.appIntegrity?.appRecognitionVerdict ?? null;
    const deviceRecognitionVerdict = verdict?.deviceIntegrity?.deviceRecognitionVerdict ?? [];
    const passed = appRecognitionVerdict === "PLAY_RECOGNIZED" && deviceRecognitionVerdict.includes("MEETS_DEVICE_INTEGRITY");

    await db.collection("deviceIntegrity").doc(uid).set({
      verifiedAtEpochMs: Date.now(),
      passed,
      appRecognitionVerdict,
      deviceRecognitionVerdict,
    });

    return { passed };
  } catch (error) {
    logger.warn(`verifyDeviceIntegrity failed for ${uid}`, error);
    throw new HttpsError("internal", "Integrity verification failed");
  }
});

/** Callable from the client (`core.billing.PremiumShopManager`) right after a Google Play purchase
 * completes - server-verifies it against the Play Developer API before granting any cosmetic, so
 * (unlike the coin Point Shop's `purchase()`, which trusts a client-initiated Firestore write,
 * only re-validated after the fact by validateCosmeticsWrite) a premium grant is never possible
 * without Google itself having confirmed the purchase first. Requires this Firebase project's
 * Cloud Functions service account to have Android Publisher API access - see
 * PREMIUM_SHOP_SETUP.md. Untestable from this environment (needs a real Play Console purchase).
 *
 * Two anti-fraud layers, both required:
 * 1. **Account binding** - `PremiumShopManager` sets `.setObfuscatedAccountId(sha256(uid))`
 *    when launching the purchase; this function recomputes that hash from `request.auth.uid` and
 *    compares it against what Play's own API response reports, so a purchase token stolen from
 *    another account is rejected on its very first use, not only on replay.
 * 2. **Replay guard** - `claimedPurchaseTokens/{sha256(purchaseToken)}` records which uid first
 *    claimed a given token. A second claim by the *same* uid (a retry, or Restore Purchases)
 *    short-circuits to success without re-calling the Play API; a claim by a *different* uid is
 *    rejected outright. Both this and `premiumPurchases/{uid}_{productId}` (a separate,
 *    human/query-friendly audit record - kept distinct from the replay lock so a refund-and-rebuy
 *    cycle producing a second token for the same product still reads as one clean record per
 *    product owned) are Admin-SDK-only - no `firestore.rules` entry needed, same "Functions bypass
 *    rules entirely" pattern already used for `deviceIntegrity/{uid}`. */
export const verifyPremiumPurchase = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign-in required");
  }
  const uid = request.auth.uid;
  const productId = request.data?.productId;
  const purchaseToken = request.data?.purchaseToken;
  if (typeof productId !== "string" || typeof purchaseToken !== "string" || purchaseToken.length === 0) {
    throw new HttpsError("invalid-argument", "productId and purchaseToken are required");
  }
  const grantedSkus = PREMIUM_PRODUCT_GRANTS[productId];
  if (!grantedSkus) {
    throw new HttpsError("invalid-argument", `Unknown premium product: ${productId}`);
  }

  const tokenHash = crypto.createHash("sha256").update(purchaseToken).digest("hex");
  const tokenRef = db.collection("claimedPurchaseTokens").doc(tokenHash);
  const tokenSnapshot = await tokenRef.get();
  if (tokenSnapshot.exists) {
    const claim = tokenSnapshot.data()!;
    if (claim.uid !== uid) {
      logger.warn(`verifyPremiumPurchase: token already claimed by a different uid (product ${productId})`);
      throw new HttpsError("permission-denied", "This purchase token belongs to a different account");
    }
    return { granted: true }; // Safe replay (retry/restore) - already verified and granted.
  }

  try {
    const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
    const androidpublisher = google.androidpublisher({ version: "v3", auth });
    const response = await androidpublisher.purchases.products.get({
      packageName: PACKAGE_NAME,
      productId,
      token: purchaseToken,
    });

    const purchase = response.data;
    if (purchase.purchaseState !== 0) {
      // 0 = purchased, 1 = canceled, 2 = pending - only a completed purchase grants anything.
      throw new HttpsError("failed-precondition", "Purchase is not in a completed state");
    }
    const expectedAccountHash = crypto.createHash("sha256").update(uid).digest("hex");
    if (purchase.obfuscatedExternalAccountId && purchase.obfuscatedExternalAccountId !== expectedAccountHash) {
      logger.warn(`verifyPremiumPurchase: account id mismatch for uid ${uid}, product ${productId}`);
      throw new HttpsError("permission-denied", "This purchase does not belong to the signed-in account");
    }

    // Reset the shared cosmetics rate-limit doc immediately before granting - see
    // validateCosmeticsWrite's isRateLimited call below. Without this, a premium grant landing
    // within 3 seconds of an unrelated coin action (equip/purchase/spin) would get reverted by
    // that same cooldown, silently undoing a purchase Google has already charged the player for.
    await db.collection("rateLimits").doc(`${uid}_cosmetics`).delete();

    const cosmeticsRef = db.collection("playerCosmetics").doc(uid);
    const cosmeticsSnapshot = await cosmeticsRef.get();
    const existingData = cosmeticsSnapshot.data();
    const existingOwned: string[] = Array.isArray(existingData?.ownedSkus) ? existingData!.ownedSkus : [];
    const existingEquipped = typeof existingData?.equipped === "object" && existingData?.equipped !== null ? existingData!.equipped : {};
    const updatedOwned = Array.from(new Set([...existingOwned, ...grantedSkus]));
    // Full-shape write every time, same "never a partial write to a maybe-not-yet-existing
    // document" discipline DebugTestGrantor.kt documents on the Kotlin side.
    await cosmeticsRef.set(
      { ownedSkus: updatedOwned, equipped: existingEquipped, updatedAtEpochMs: Date.now() },
      { merge: true },
    );

    await tokenRef.set({ uid, productId, claimedAtEpochMs: Date.now() });
    await db.collection("premiumPurchases").doc(`${uid}_${productId}`).set({
      uid, productId, grantedSkus, purchasedAtEpochMs: Date.now(),
    });

    return { granted: true };
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    logger.warn(`verifyPremiumPurchase failed for ${uid}/${productId}`, error);
    throw new HttpsError("internal", "Purchase verification failed");
  }
});

/** Same shape and reasoning as [verifyPremiumPurchase] immediately above, for the legacy
 * `remove_ads_lifetime` one-time product instead of the 7 Premium Shop cosmetic bundles -
 * production-readiness audit flagged that `BillingManagerImpl` granted `hasRemovedAds` from the
 * raw client-reported [com.android.billingclient.api.Purchase.purchaseState] alone, unlike Premium
 * Shop's already-server-verified path. `removeAdsPurchases/{uid}` is a new Admin-SDK-only
 * collection (no `firestore.rules` entry needed, same as `premiumPurchases`/`claimedPurchaseTokens`
 * above) - purely a server-side audit record; `hasRemovedAds` itself stays a local
 * `UserPreferencesDataStore` flag exactly as it already was, restored on reinstall via Play
 * Billing's own `queryPurchasesAsync` (Google's servers remember the purchase regardless of app
 * reinstall) - this function only gates *whether the client is allowed to set that flag at all*,
 * closing the "a patched client fakes PURCHASED without a real purchase" gap. */
export const verifyRemoveAdsPurchase = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign-in required");
  }
  const uid = request.auth.uid;
  const purchaseToken = request.data?.purchaseToken;
  if (typeof purchaseToken !== "string" || purchaseToken.length === 0) {
    throw new HttpsError("invalid-argument", "purchaseToken is required");
  }

  const tokenHash = crypto.createHash("sha256").update(purchaseToken).digest("hex");
  const tokenRef = db.collection("claimedPurchaseTokens").doc(tokenHash);
  const tokenSnapshot = await tokenRef.get();
  if (tokenSnapshot.exists) {
    const claim = tokenSnapshot.data()!;
    if (claim.uid !== uid) {
      logger.warn("verifyRemoveAdsPurchase: token already claimed by a different uid");
      throw new HttpsError("permission-denied", "This purchase token belongs to a different account");
    }
    return { granted: true }; // Safe replay (retry/restore) - already verified and granted.
  }

  try {
    const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
    const androidpublisher = google.androidpublisher({ version: "v3", auth });
    const response = await androidpublisher.purchases.products.get({
      packageName: PACKAGE_NAME,
      productId: REMOVE_ADS_PRODUCT_ID,
      token: purchaseToken,
    });

    const purchase = response.data;
    if (purchase.purchaseState !== 0) {
      // 0 = purchased, 1 = canceled, 2 = pending - only a completed purchase grants anything.
      throw new HttpsError("failed-precondition", "Purchase is not in a completed state");
    }
    const expectedAccountHash = crypto.createHash("sha256").update(uid).digest("hex");
    if (purchase.obfuscatedExternalAccountId && purchase.obfuscatedExternalAccountId !== expectedAccountHash) {
      logger.warn(`verifyRemoveAdsPurchase: account id mismatch for uid ${uid}`);
      throw new HttpsError("permission-denied", "This purchase does not belong to the signed-in account");
    }

    await tokenRef.set({ uid, productId: REMOVE_ADS_PRODUCT_ID, claimedAtEpochMs: Date.now() });
    await db.collection("removeAdsPurchases").doc(uid).set({ uid, purchasedAtEpochMs: Date.now() });

    return { granted: true };
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    logger.warn(`verifyRemoveAdsPurchase failed for ${uid}`, error);
    throw new HttpsError("internal", "Purchase verification failed");
  }
});

/** Callable, admin-privileged reset for the calling player's own per-uid progress documents -
 * the piece [com.suman.memoryarchitect.data.repository.LocalProgressResetRepositoryImpl]'s "Reset
 * Progress" was always missing once a real Firebase project is configured: it only ever reset the
 * dev mock backend and the local Room cache, so the next successful online read silently
 * repopulated everything from Firestore. The client SDK has no way to do this itself - deleting a
 * document is unconditionally denied by firestore.rules (no `allow delete` exists on any of these
 * collections), and even a client write attempting to zero xp/coins/journeyPoints back to 0 would
 * be rejected outright by validateProfileWrite's "these only ever increase" check. Deletes rather
 * than writes a zeroed shape: an absent `playerProfiles/{uid}` doc already reads as
 * [com.suman.memoryarchitect.domain.model.PlayerProfile.EMPTY] everywhere downstream (every
 * `toProfile()` helper in this file already treats `!exists()` that way), so there's no second
 * "empty" shape to construct and keep in sync with isValidProfile.
 *
 * Deliberately does not touch `players/{uid}` (the public leaderboard denormalization) or
 * `playerCosmetics/{uid}` - a local progress reset isn't meant to also erase a player's
 * competitive leaderboard history or owned cosmetics, matching this app's existing "Reset
 * Progress" scope (statistics/profile/missions/inventory, not purchases or leaderboard standing).
 */
export const resetPlayerData = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign-in required");
  }
  const uid = request.auth.uid;
  const collectionsToReset = ["playerProfiles", "dailyRewards", "inventory", "missionClaims", "returningPlayerGifts"];
  await Promise.all(collectionsToReset.map((collection) => db.collection(collection).doc(uid).delete()));
  return { reset: true };
});

/** Re-validates every Daily/Weekly/Monthly leaderboard entry write. */
export const validatePeriodicEntry = onDocumentWritten(
  "{collection}/{period}/entries/{uid}",
  async (event) => {
    const periodicCollections = ["leaderboardDaily", "leaderboardWeekly", "leaderboardMonthly"];
    if (!periodicCollections.includes(event.params.collection)) return;

    const after = event.data?.after;
    if (!after?.exists) return;

    const data = after.data();
    const uid = event.params.uid;
    const problems: string[] = [];

    if (typeof data?.score !== "number" || data.score < 0 || data.score > MAX_PLAUSIBLE_PERIODIC_SCORE) {
      problems.push("score out of range");
    }
    if (typeof data?.accuracy !== "number" || data.accuracy < 0 || data.accuracy > 1) {
      problems.push("accuracy out of range");
    }
    if (
      typeof data?.completionTimeMs !== "number" ||
      data.completionTimeMs < 0 ||
      data.completionTimeMs > MAX_PLAUSIBLE_ROUND_DURATION_MS
    ) {
      problems.push("completionTimeMs out of range");
    }

    const before = event.data?.before;
    if (before?.exists) {
      const previousScore = before.data()?.score ?? 0;
      // Same "only ever improves" invariant LeaderboardRepositoryImpl.writePeriodicEntry already
      // enforces client-side - re-checked here so it can't be bypassed by writing to Firestore
      // directly.
      if (typeof data?.score === "number" && data.score < previousScore) {
        problems.push(`score decreased (${previousScore} -> ${data.score})`);
      }
    }

    if (await isRateLimited(uid, event.params.collection)) {
      problems.push("resubmitted faster than the allowed interval");
    }

    if (problems.length > 0) {
      logger.warn(`Reverting invalid ${event.params.collection}/${event.params.period}/entries/${uid} write: ${problems.join(", ")}`);
      if (before?.exists) {
        await after.ref.set(before.data()!);
      } else {
        await after.ref.delete();
      }
    }
  },
);

/** Re-validates every `purchaseReceipts/{receiptId}` write - the record
 * FirestoreShopRemoteSource.purchase/spin writes alongside the coin deduction. Independent of
 * validateProfileWrite's coin-decrease bound above: this checks the receipt's own shape and price
 * against the mirrored catalog (shopCatalog.ts), so a forged receipt with a fabricated cheap price
 * can't pair with a real decrease to make an expensive item look paid-for. Cheap - no
 * cross-document query needed, the receipt already carries everything it claims.
 *
 * PURCHASE and SPIN are validated differently: PURCHASE's `sku` is always a real catalog item at
 * its exact listed price. SPIN's `priceCoins` is a coins *delta*, not a cost (spins are free, see
 * SpinRules.kt's doc) - `sku: "COINS"` (a coins-kind reward) is valid with `priceCoins` one of
 * SPIN_COIN_OUTCOME_AMOUNTS, and a real catalog `sku` (a cosmetic-kind reward) is valid with
 * `priceCoins` either 0 (a fresh grant) or the exact duplicate-refund amount for that sku's price. */
export const validatePurchaseReceipt = onDocumentWritten("purchaseReceipts/{receiptId}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const problems: string[] = [];

  if (data?.kind !== "PURCHASE" && data?.kind !== "SPIN") {
    problems.push("invalid kind");
  } else if (data.kind === "PURCHASE") {
    if (typeof data?.sku !== "string" || !(data.sku in SHOP_CATALOG_PRICES)) {
      problems.push("unknown sku");
    } else {
      const expectedPrice = SHOP_CATALOG_PRICES[data.sku];
      if (typeof data?.priceCoins !== "number" || data.priceCoins !== expectedPrice) {
        problems.push(`priceCoins mismatch (expected ${expectedPrice}, got ${data?.priceCoins})`);
      }
    }
  } else {
    // SPIN
    if (typeof data?.priceCoins !== "number") {
      problems.push("priceCoins missing or not a number");
    } else if (data?.sku === "COINS") {
      if (!SPIN_COIN_OUTCOME_AMOUNTS.includes(data.priceCoins)) {
        problems.push(`priceCoins ${data.priceCoins} not a configured spin coin-outcome amount`);
      }
    } else if (typeof data?.sku === "string" && data.sku in SHOP_CATALOG_PRICES) {
      const expectedRefund = Math.round(SHOP_CATALOG_PRICES[data.sku] * SPIN_DUPLICATE_REFUND_FRACTION);
      if (data.priceCoins !== 0 && data.priceCoins !== expectedRefund) {
        problems.push(`priceCoins ${data.priceCoins} doesn't match 0 (fresh) or ${expectedRefund} (duplicate refund)`);
      }
    } else {
      problems.push("unknown sku for SPIN receipt");
    }
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid purchaseReceipts/${event.params.receiptId} write: ${problems.join(", ")}`);
    await after.ref.delete();
  }
});

/** Re-validates every `playerCosmetics/{uid}` write - cosmetics are permanent once owned, so the
 * owned-set may only ever grow (before ⊆ after), never reference an unknown sku, and `equipped`
 * may only reference skus already present in `ownedSkus`. */
export const validateCosmeticsWrite = onDocumentWritten("playerCosmetics/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const ownedSkus: string[] = Array.isArray(data?.ownedSkus) ? data.ownedSkus : [];
  for (const sku of ownedSkus) {
    // A sku is legitimate if it's coin-priced (SHOP_CATALOG_PRICES) OR a premium bundle grant
    // (PREMIUM_GRANTED_SKUS, see premiumCatalog.ts) - without the second check here, the moment
    // verifyPremiumPurchase below grants a premium-only sku, this same-collection trigger would
    // revert it within the same second, since a premium sku is never in SHOP_CATALOG_PRICES.
    if (!(sku in SHOP_CATALOG_PRICES) && !PREMIUM_GRANTED_SKUS.has(sku)) {
      problems.push(`unknown sku in ownedSkus: ${sku}`);
    }
  }

  const before = event.data?.before;
  if (before?.exists) {
    const previousOwned: string[] = Array.isArray(before.data()?.ownedSkus) ? before.data()!.ownedSkus : [];
    const ownedSet = new Set(ownedSkus);
    const missing = previousOwned.filter((sku) => !ownedSet.has(sku));
    if (missing.length > 0) {
      problems.push(`ownedSkus shrank: lost ${missing.join(", ")}`);
    }
  }

  const equipped: Record<string, string> = typeof data?.equipped === "object" && data?.equipped !== null ? data.equipped : {};
  const ownedSet = new Set(ownedSkus);
  for (const sku of Object.values(equipped)) {
    if (!ownedSet.has(sku)) {
      problems.push(`equipped references unowned sku: ${sku}`);
    }
  }

  if (await isRateLimited(uid, "cosmetics")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid playerCosmetics/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

// How many epoch-days a claim's own lastClaimedEpochDay may differ from the server's real current
// day - a small allowance for timezone/clock-skew edge cases right at day rollover, not a loophole:
// nextDailyRewardCycleDay still only ever advances the cycle by one day per legitimate write, so
// this bounds how far a forged write could ever "time travel" through the cycle in one shot.
const MAX_DAILY_REWARD_CLOCK_SKEW_DAYS = 1;

/** Re-validates every `dailyRewards/{uid}` write - unlike missionClaims/returningPlayerGifts, this
 * collection previously had no independent server-side re-derivation at all: a forged write could
 * repeatedly claim the richest day's payout, throttled only by the rate limiter below. Re-derives
 * the expected cycleDay from the *stored previous* state (never trusted from the write itself, the
 * same discipline validateMissionClaims already applies) and bounds the claimed day against the
 * server's own clock, so a claim can never advance the cycle faster than one real day at a time. */
export const validateDailyReward = onDocumentWritten("dailyRewards/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const claimedOnEpochDay = data?.lastClaimedEpochDay;
  const cycleDay = data?.cycleDay;
  if (typeof claimedOnEpochDay !== "number" || typeof cycleDay !== "number") {
    problems.push("lastClaimedEpochDay/cycleDay are not numbers");
  } else {
    const before = event.data?.before;
    const previousClaimedOnEpochDay: number | null = before?.exists ? (before.data()?.lastClaimedEpochDay ?? null) : null;
    const previousCycleDay: number = before?.exists ? (before.data()?.cycleDay ?? 0) : 0;

    const nowEpochDay = Math.floor(Date.now() / 86_400_000);
    if (Math.abs(claimedOnEpochDay - nowEpochDay) > MAX_DAILY_REWARD_CLOCK_SKEW_DAYS) {
      problems.push(`lastClaimedEpochDay implausible vs server clock (${claimedOnEpochDay} vs ${nowEpochDay})`);
    }
    if (!canClaimDailyReward(previousClaimedOnEpochDay, claimedOnEpochDay)) {
      problems.push("already claimed for this day");
    }
    const expectedCycleDay = nextDailyRewardCycleDay(previousClaimedOnEpochDay, previousCycleDay, claimedOnEpochDay);
    if (cycleDay !== expectedCycleDay) {
      problems.push(`cycleDay does not match the derived cycle (${cycleDay} vs expected ${expectedCycleDay})`);
    }
  }

  if (await isRateLimited(uid, "dailyReward")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid dailyRewards/${uid} write: ${problems.join(", ")}`);
    const before = event.data?.before;
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

/** Re-validates every `missionClaims/{uid}` write - the double-claim guard
 * FirestoreMissionRemoteSource.claimMissionReward writes alongside the coin/xp/inventory grant.
 * Independent of validateProfileWrite/validateInventoryWrite's plausibility bounds below: this
 * checks that each newly-added claim key actually names a mission that was part of *that period's*
 * deterministic rotation (see missions.ts's activeMissionIds, recomputed here from scratch rather
 * than trusted), so a forged claim for a mission that was never active can't pair with a
 * plausible-looking profile/inventory increase to look legitimate. */
export const validateMissionClaims = onDocumentWritten("missionClaims/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const claimedKeys: Record<string, unknown> = typeof data?.claimedKeys === "object" && data?.claimedKeys !== null ? data.claimedKeys : {};
  const before = event.data?.before;
  const previousKeys: Record<string, unknown> = before?.exists && typeof before.data()?.claimedKeys === "object" ? before.data()!.claimedKeys : {};

  // Claims are append-only, exactly like playerCosmetics.ownedSkus can only ever grow.
  const missingKeys = Object.keys(previousKeys).filter((key) => !(key in claimedKeys));
  if (missingKeys.length > 0) {
    problems.push(`claimedKeys shrank: lost ${missingKeys.join(", ")}`);
  }

  const newKeys = Object.keys(claimedKeys).filter((key) => !(key in previousKeys));
  if (newKeys.length > 1) {
    problems.push("more than one new mission claimed in a single write");
  }
  for (const key of newKeys) {
    const separatorIndex = key.lastIndexOf("_");
    const missionId = key.slice(0, separatorIndex);
    const periodKey = Number(key.slice(separatorIndex + 1));
    const definition = definitionFor(missionId);
    const period = periodFor(missionId);
    if (!definition || !period || Number.isNaN(periodKey)) {
      problems.push(`malformed claim key: ${key}`);
      continue;
    }
    if (!activeMissionIds(period, periodKey).includes(missionId)) {
      problems.push(`${missionId} was never active for period ${periodKey}`);
    }
  }

  if (await isRateLimited(uid, "missionClaims")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid missionClaims/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

const CATEGORY_BONUS_PERIODS = new Set(["DAILY", "WEEKLY", "MONTHLY"]);

/** Re-validates every `missionCategoryBonusClaims/{uid}` write - the double-grant guard
 * FirestoreMissionRemoteSource.claimCategoryBonus writes alongside the coin/xp/inventory grant.
 * Deliberately its own collection/validator, not folded into validateMissionClaims above - a
 * `"{period}_{periodKey}"` key would fail that function's MissionId-shaped check. Shape/format
 * only (append-only, one new key per write, key parses as a known period + numeric periodKey) -
 * unlike validateMissionClaims, this doesn't re-derive "was the whole set actually claimed"
 * (that would need a cross-document read of missionClaims/{uid}); the Firestore *client*
 * transaction already is the authoritative gate for that, the same "shape here, real gate in the
 * transaction" split validateMissionRefreshState below also uses. */
export const validateMissionCategoryBonusClaims = onDocumentWritten("missionCategoryBonusClaims/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const claimedKeys: Record<string, unknown> = typeof data?.claimedKeys === "object" && data?.claimedKeys !== null ? data.claimedKeys : {};
  const before = event.data?.before;
  const previousKeys: Record<string, unknown> = before?.exists && typeof before.data()?.claimedKeys === "object" ? before.data()!.claimedKeys : {};

  const missingKeys = Object.keys(previousKeys).filter((key) => !(key in claimedKeys));
  if (missingKeys.length > 0) {
    problems.push(`claimedKeys shrank: lost ${missingKeys.join(", ")}`);
  }

  const newKeys = Object.keys(claimedKeys).filter((key) => !(key in previousKeys));
  if (newKeys.length > 1) {
    problems.push("more than one new category bonus claimed in a single write");
  }
  for (const key of newKeys) {
    const separatorIndex = key.lastIndexOf("_");
    const period = key.slice(0, separatorIndex);
    const periodKey = Number(key.slice(separatorIndex + 1));
    if (!CATEGORY_BONUS_PERIODS.has(period) || Number.isNaN(periodKey)) {
      problems.push(`malformed category bonus claim key: ${key}`);
    }
  }

  if (await isRateLimited(uid, "missionCategoryBonusClaims")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid missionCategoryBonusClaims/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

/** Re-validates every `missionRefreshState/{uid}` write - the "pay 1000 coins to skip the
 * countdown" override FirestoreMissionRemoteSource.unlockAllMissionsEarly writes alongside the
 * coin deduction (that deduction itself is already bounded by validateProfileWrite above). Shape
 * plus a monotonic check only - each forced periodKey may only move forward, mirroring
 * playerCosmetics.ownedSkus' "may only grow" discipline; the actual "was every mission in all
 * three periods really claimed" eligibility lives in the Firestore transaction itself, same split
 * validateMissionCategoryBonusClaims above documents. */
export const validateMissionRefreshState = onDocumentWritten("missionRefreshState/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];
  const before = event.data?.before;
  const previous = before?.exists ? before.data() : null;

  for (const field of ["dailyForcedPeriodKey", "weeklyForcedPeriodKey", "monthlyForcedPeriodKey"]) {
    const value = data?.[field];
    if (value !== null && value !== undefined && typeof value !== "number") {
      problems.push(`${field} is not a number or null`);
      continue;
    }
    const previousValue = previous?.[field];
    if (typeof previousValue === "number" && typeof value === "number" && value < previousValue) {
      problems.push(`${field} decreased (${previousValue} -> ${value})`);
    }
  }

  if (await isRateLimited(uid, "missionRefreshState")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid missionRefreshState/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

// Mirrors ReturningPlayerRules.Default.mediumGapDays (domain/progression/ReturningPlayerRules.kt) -
// the only threshold this function needs to re-validate; SHORT tier never grants anything.
const RETURNING_PLAYER_MEDIUM_GAP_DAYS = 7;

/** Re-validates every `returningPlayerGifts/{uid}` write - independently re-derives eligibility
 * (a real gap since `playerProfiles/{uid}.lastPlayedEpochDay`, not already claimed for this exact
 * day) from admin-privileged reads rather than trusting the client's own
 * GetReturningPlayerWelcomeUseCase check, the same "re-verify after the fact" layer
 * validateMissionClaims/validateProfileWrite already provide for their own collections. */
export const validateReturningPlayerGiftWrite = onDocumentWritten("returningPlayerGifts/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const lastClaimedOnEpochDay = data?.lastClaimedOnEpochDay;
  if (typeof lastClaimedOnEpochDay !== "number") {
    problems.push("lastClaimedOnEpochDay is not a number");
  } else {
    const before = event.data?.before;
    const previousClaimedOnEpochDay = before?.exists ? before.data()?.lastClaimedOnEpochDay : undefined;
    if (previousClaimedOnEpochDay === lastClaimedOnEpochDay) {
      problems.push("gift already claimed for this exact day");
    }
    const profileSnapshot = await db.collection("playerProfiles").doc(uid).get();
    const lastPlayedEpochDay = profileSnapshot.exists ? profileSnapshot.data()?.lastPlayedEpochDay : undefined;
    const gapDays = typeof lastPlayedEpochDay === "number" ? lastClaimedOnEpochDay - lastPlayedEpochDay : null;
    if (gapDays === null || gapDays < RETURNING_PLAYER_MEDIUM_GAP_DAYS) {
      problems.push(`gap of ${gapDays} days is too short to be eligible`);
    }
  }

  if (await isRateLimited(uid, "returningPlayerGift")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid returningPlayerGifts/${uid} write: ${problems.join(", ")}`);
    const before = event.data?.before;
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

/** Re-validates every `inventory/{uid}` write - written by both
 * FirestoreMissionRemoteSource.claimMissionReward (a grant) and .consumeInventoryItem (a spend).
 * A spend is unbounded here (consuming your own already-owned items can't create value out of
 * nothing); only an *increase* is bounded, the same "plausibility, not full re-derivation" the
 * profile/cosmetics validators above already apply. */
export const validateInventoryWrite = onDocumentWritten("inventory/{uid}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;

  const data = after.data();
  const uid = event.params.uid;
  const problems: string[] = [];

  const quantities: Record<string, unknown> = typeof data?.quantities === "object" && data?.quantities !== null ? data.quantities : {};
  const before = event.data?.before;
  const previousQuantities: Record<string, unknown> =
    before?.exists && typeof before.data()?.quantities === "object" ? before.data()!.quantities : {};

  for (const [kind, value] of Object.entries(quantities)) {
    if (typeof value !== "number" || value < 0) {
      problems.push(`invalid quantity for ${kind}`);
      continue;
    }
    const previous = typeof previousQuantities[kind] === "number" ? (previousQuantities[kind] as number) : 0;
    if (value - previous > MAX_MISSION_INVENTORY_GRANT_PER_KIND) {
      problems.push(`${kind} increased implausibly (${previous} -> ${value})`);
    }
  }

  if (await isRateLimited(uid, "inventory")) {
    problems.push("resubmitted faster than the allowed interval");
  }

  if (problems.length > 0) {
    logger.warn(`Reverting invalid inventory/${uid} write: ${problems.join(", ")}`);
    if (before?.exists) {
      await after.ref.set(before.data()!);
    } else {
      await after.ref.delete();
    }
  }
});

/** A lightweight per-uid cooldown, backed by a small `rateLimits/{uid}_{scope}` doc - cheap (one
 * extra read+write per submission) and enough to block a scripted rapid-fire abuse loop, without
 * needing a queue or external rate-limiting service for a game with this write volume.
 *
 * [scope] matters: a single scored round writes `playerProfiles/{uid}`, `players/{uid}`, and a
 * periodic entry collection within milliseconds of each other via
 * GameplayViewModel.submitReconstruction/pushToLeaderboards - sharing one cooldown doc across all
 * three would make each round's *own* legitimate writes look like a rapid-fire resubmission of
 * each other and revert them. Each collection gets its own independent cooldown instead. */
async function isRateLimited(uid: string, scope: string): Promise<boolean> {
  const ref = db.collection("rateLimits").doc(`${uid}_${scope}`);
  const now = Date.now();
  const snapshot = await ref.get();
  const lastWriteEpochMs = snapshot.exists ? (snapshot.data()?.lastWriteEpochMs as number | undefined) : undefined;
  await ref.set({ lastWriteEpochMs: now }, { merge: true });
  if (lastWriteEpochMs === undefined) return false;
  return now - lastWriteEpochMs < MIN_RESUBMISSION_INTERVAL_MS;
}
