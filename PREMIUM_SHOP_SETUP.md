# Premium Shop (real-money cosmetic bundles) setup

This project ships with a complete, production-ready Premium Shop already wired in code - 7 new
real-money cosmetic bundles (Founder's Pack, Starter Bundle, and 5 themed collections) sold via
Google Play Billing, each server-verified against the Google Play Developer API before anything is
granted. It sits alongside the pre-existing `remove_ads_lifetime` purchase (`BILLING_SETUP.md`) -
same `BillingClient`, same Play Console, a separate product list. Like this project's other
external integrations (`FIREBASE_SETUP.md`, `LEADERBOARD_SETUP.md`, `BILLING_SETUP.md`,
`POINTS_SHOP_SETUP.md`), it stays inert until you create the products in **Google Play Console**
and grant this project's Cloud Functions access to read purchase state - nothing in this repo can
do either step for you.

## What's already wired in code

- **Catalog**: `domain/progression/PremiumShopCatalog.kt` - 7 cosmetic-bundle products (53
  premium-only cosmetics total, `domain/progression/PremiumCatalog.kt`), plus the pre-existing
  `remove_ads_lifetime` entry, all rendered from one `PremiumProductCard`/`PremiumProductDetailDialog`
  pair in the Shop's 💎 Premium tab (`ui/screens/shop/ShopScreen.kt`) - visually and structurally
  separate from the 🪙 Coin tab's existing `ShopItemRow`s.
- **Billing**: `core/billing/SharedBillingClient.kt` (the one `BillingClient` this app is allowed
  to have, shared with the pre-existing `BillingManagerImpl`) and `core/billing/PremiumShopManagerImpl.kt`
  (queries/purchases/restores the 7 bundle product IDs, isolated from `BillingManagerImpl`'s own
  in-flight purchase by a separate `inFlightProductId` flag so the two flows can never cross-contaminate
  each other's state).
- **Server-side verification**: `functions/src/index.ts`'s `verifyPremiumPurchase` - unlike the
  coin Point Shop's `purchase()` (a trusted client write, only checked *after* the fact by
  `validateCosmeticsWrite`), a premium grant is **never** written until this function has confirmed
  the purchase with Google's own Play Developer API, with two anti-fraud layers (account binding via
  `setObfuscatedAccountId`, and a `claimedPurchaseTokens/{sha256(token)}` replay guard) - see that
  function's doc comment in `functions/src/index.ts` for the full design.
- **Developer Test Mode**: `core/debug/DebugTestGrantor.debugGrantPremiumProduct()` - grants a
  product's cosmetics locally without any real Play Billing transaction, so every screen/flow is
  fully testable before a single product exists in Play Console. See below for how it's gated.

## Why this stays inert until you do the steps below

Google Play Billing refuses to return real product details/prices for a product ID Play Console
doesn't know about, and `verifyPremiumPurchase` cannot call the Play Developer API at all until
this Firebase project's Cloud Functions service account is granted read access to your app's
purchase data. Until both are done: `productPrices` in `PremiumShopManagerImpl` stays empty, every
`PremiumProductCard` shows a "Loading price…" state that - after the query genuinely fails, which
it always will with no product configured - flips to a "Price unavailable" message with a Retry
button (its Buy button stays disabled throughout), and `verifyPremiumPurchase` would fail even if a
purchase somehow completed. **Developer Test Mode (below) is unaffected by any of this** - it's the
intended way to build/test/demo the whole feature before Play Console is touched at all.

## What you need to create

**Play Console → your app → Monetize → Products → In-app products → Create product**, once per row:

| Product ID | Name (suggested) |
|---|---|
| `founders_pack` | Founder's Pack |
| `starter_bundle` | Starter Bundle |
| `royal_collection` | Royal Collection |
| `cyber_collection` | Cyber Collection |
| `space_collection` | Space Collection |
| `nature_collection` | Nature Collection |
| `luxury_collection` | Luxury Collection |

Every one is a **Managed product (non-consumable)** - Play Console calls this an "in-app product,"
not a subscription. Product IDs must match `domain/progression/PremiumShopCatalog.kt`'s constants
exactly (they're hardcoded there, same convention `BillingManagerImpl.REMOVE_ADS_PRODUCT_ID`
already uses for `remove_ads_lifetime`).

## Grant Cloud Functions access to the Play Developer API

`verifyPremiumPurchase` calls `androidpublisher.purchases.products.get` using this Firebase
project's default Cloud Functions service account. That account needs explicit access:

1. **Play Console → Setup → API access.** Link this Firebase project's Google Cloud project (same
   one `LEADERBOARD_SETUP.md`'s Play Integrity linking uses, if you've already done that step -
   it's the same underlying Cloud project either way).
2. Under the linked project's service accounts, find the **App Engine default service account**
   (`<project-id>@appspot.gserviceaccount.com` - this is what Cloud Functions runs as by default)
   and grant it access with the **"View financial data"** permission at minimum (required for
   `purchases.products.get` to return purchase state).
3. Enable the **Google Play Android Developer API** for this Google Cloud project
   (console.cloud.google.com → APIs & Services → enable "Google Play Android Developer API").
4. Deploy the updated Cloud Functions:
   ```
   cd functions
   npm install
   firebase deploy --only functions
   ```

No `firestore.rules` change is needed - `claimedPurchaseTokens` and `premiumPurchases` are
Admin-SDK-only collections (Cloud Functions bypass rules entirely), same pattern
`deviceIntegrity/{uid}` already uses.

## Testing before going live

Same platform constraints as `BILLING_SETUP.md`'s Remove Ads testing section, since they share one
`BillingClient`:

1. Add testers under **Play Console → Setup → License testing**.
2. Upload a signed build to at least **Internal testing** (Play Billing purchase flows do not work
   against a locally-signed debug APK installed via `adb install`).
3. Install from the Play Store internal testing link on a license-tester account and confirm: each
   Premium tab card shows a real localized price, a purchase completes, the granted cosmetics
   appear equip-ready in Collections immediately, and Restore Purchases re-grants after
   uninstall/reinstall on the same account.

## Developer Test Mode - and how to go to production

Every Premium Shop card and the Shop screen's `BuildConfig.DEBUG`-gated row both call
`ShopViewModel.debugGrantPremiumProduct(productId)`, which calls
`DebugTestGrantor.debugGrantPremiumProduct()` - it writes the product's granted cosmetics directly
to `playerCosmetics/{uid}.ownedSkus` (same full-shape Firestore write `unlockAllCosmeticsForTesting()`
already uses), with **no real Play Billing transaction and no server verification**. This is what
lets every purchase-success path, every live preview, and Collections integration be fully tested
before a single product exists in Play Console.

**Nothing needs to change in code to "switch to production."** Every debug entry point is already
wrapped in `if (BuildConfig.DEBUG)` at its call site (`ui/screens/shop/ShopScreen.kt`) - the same
gate this codebase already uses for its coin-shop debug row and `SettingsScreen`'s analytics
dashboard. A release build (`BuildConfig.DEBUG == false`) never compiles that UI into the APK at
all, so there is no toggle to flip, no build flavor to switch, and no risk of shipping a build where
Developer Test Mode is accidentally reachable.

## Regional pricing

Set each product's **base price in USD** exactly as below under **Play Console → the product →
Price** - Play Console then auto-converts to every other supported currency/region using its own
current exchange rates and local pricing patterns (e.g. psychological rounding like ₹399 instead of
a raw conversion). Do not type in converted values for other countries yourself; let Play Console's
own regional pricing template do that, so every player automatically sees their local currency, a
region-appropriate price, and Google Play-handled local taxes with no per-country logic anywhere in
this app.

| Product | Base price (USD) |
|---|---|
| Starter Bundle | $2.99 |
| Royal Collection | $4.99 |
| Cyber Collection | $4.99 |
| Space Collection | $4.99 |
| Nature Collection | $4.99 |
| Luxury Collection | $6.99 |
| Founder's Pack | $5.99 |

Founder's Pack is priced above any single themed collection deliberately (it bundles 8 items across
every category plus the one-time "Founder" identity) but below buying multiple collections
separately - the bundle-discount math a player can do in their head is part of what should make it
feel like the obviously good deal. As with Remove Ads (`BILLING_SETUP.md`), **nothing in the app
hardcodes any of this** - `PremiumShopManagerImpl` only ever reads
`ProductDetails.oneTimePurchaseOfferDetails.formattedPrice`, Play's own already-localized string.

## Expanding the catalog later

Same shape as `POINTS_SHOP_SETUP.md`'s coin-catalog expansion, but through the premium-only files:

1. Add `CosmeticId` entries (`domain/model/CosmeticId.kt`).
2. Add their `CosmeticDefinition`s to `domain/progression/PremiumCatalog.kt` (never `ShopCatalog.kt`
   - a premium-only id must never be coin-purchasable or Lucky-Spin-eligible).
3. Add their visual specs (`ui/theme/CosmeticVisualCatalog.kt`).
4. Add a new `PremiumProduct` entry to `domain/progression/PremiumShopCatalog.kt` and a matching
   grant list to `functions/src/premiumCatalog.ts`'s `PREMIUM_PRODUCT_GRANTS` (server-side truth for
   what `verifyPremiumPurchase` actually grants - keep both in sync).
5. Create the matching product in Play Console (see above).

## Out of scope by design

- No launcher-icon theming ("Premium Icon Themes") - real launcher icons are static manifest
  `activity-alias` entries; runtime switching is a materially different, OEM-inconsistent effort.
- No changes to `BillingManagerImpl`/`remove_ads_lifetime` - the one purchase flow that already
  worked stays untouched; `remove_ads_lifetime` still verifies purely client-side (see
  `BILLING_SETUP.md`'s "Recommended future hardening" note) even though the new premium bundles now
  verify server-side.
- No new "Title" reward - `CosmeticCategory`'s own doc already states cosmetics stay disjoint from
  `RewardKind` (the free milestone system Titles belong to); Founder's Pack substitutes a
  distinctive `NAME_COLOR` for the same effect instead.
- Nothing here ever touches gameplay, timer, scoring, or leaderboard fairness - every premium
  product is provably cosmetic-only, same guarantee `POINTS_SHOP_SETUP.md`'s coin shop already
  makes for coins.
