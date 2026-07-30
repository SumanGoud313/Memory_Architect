# Premium Shop (real-money cosmetic bundles) setup

This project ships with a complete, production-ready Premium Shop already wired in code - 7 new
real-money cosmetic bundles (Founder's Pack, Starter Bundle, and 5 themed collections) sold via
Google Play Billing. It sits alongside the pre-existing `remove_ads_lifetime` purchase
(`BILLING_SETUP.md`) - same `BillingClient`, same Play Console, a separate product list. Like this
project's other external integrations (`FIREBASE_SETUP.md`, `LEADERBOARD_SETUP.md`,
`BILLING_SETUP.md`, `POINTS_SHOP_SETUP.md`), it stays inert until you create the products in
**Google Play Console** - nothing in this repo can do that step for you. This project runs
entirely on the free Firebase Spark plan (no Cloud Functions anywhere - see the Spark migration
report), so there's no separate "grant server access" step either.

## What's already wired in code

- **Catalog**: `domain/progression/PremiumShopCatalog.kt` - 7 cosmetic-bundle products (53
  premium-only cosmetics total, `domain/progression/PremiumCatalog.kt`), plus the pre-existing
  `remove_ads_lifetime` entry, all rendered from one `PremiumProductCard`/`PremiumProductDetailDialog`
  pair in the Shop's 💎 Premium tab (`ui/screens/shop/ShopScreen.kt`) - visually and structurally
  separate from the 🪙 Coin tab's existing `ShopItemRow`s.
- **Billing**: `core/billing/SharedBillingClient.kt` (the one `BillingClient` this app has) and
  `core/billing/BillingManagerImpl.kt` - one manager for both `remove_ads_lifetime` and every
  Premium Shop bundle, branching on `BillingProductType` (Play Billing mechanics) and
  `BillingEntitlementKind` (what actually gets granted) - see that class's own doc.
- **Purchase verification**: `core/billing/PurchaseSignatureVerifier.kt` checks the Play Billing
  Library's own purchase signature against your app's Play Console public key, entirely on-device -
  a patched client can't forge this without Google's private signing key. `core/billing/
  CosmeticCollectionGrantor.kt` then grants the bundle's cosmetics via a Firestore transaction,
  guarded by a `claimedPurchaseTokens/{sha256(token)}` replay lock (`firestore.rules`-enforced
  create-only, so the same token can never be claimed twice). See `BILLING_SETUP.md`'s "Why no
  server-side purchase verification" for the accepted trade-off this design makes.
- **Developer Test Mode**: `core/debug/DebugTestGrantor.debugGrantPremiumProduct()` - grants a
  product's cosmetics locally without any real Play Billing transaction, so every screen/flow is
  fully testable before a single product exists in Play Console. See below for how it's gated.

## Why this stays inert until you do the steps below

Google Play Billing refuses to return real product details/prices for a product ID Play Console
doesn't know about. Until the products exist there: `productStates` in `BillingManagerImpl` stays
empty for these product IDs, every `PremiumProductCard` shows a "Loading price…" state that - after
the query genuinely fails, which it always will with no product configured - flips to a "Price
unavailable" message with a Retry button (its Buy button stays disabled throughout). **Developer
Test Mode (below) is unaffected by any of this** - it's the intended way to build/test/demo the
whole feature before Play Console is touched at all.

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

## No server-side Play Developer API step

Earlier revisions of this feature called `androidpublisher.purchases.products.get` from a Cloud
Function to re-verify a purchase server-side before granting it. This project no longer does that -
it runs no Cloud Functions at all, to stay on the free Firebase Spark plan (see the Spark migration
report). `PurchaseSignatureVerifier` (on-device signature check) plus `CosmeticCollectionGrantor`'s
Firestore replay guard (`claimedPurchaseTokens/{sha256(token)}`, enforced by `firestore.rules`) are
the full extent of purchase verification now - see `BILLING_SETUP.md`'s "Why no server-side
purchase verification" for what that trades away (refund/chargeback revocation) and why it's an
accepted trade-off for this app. Nothing to grant, nothing to deploy, no Google Cloud service
account setup needed.

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
4. Add a new `PremiumProduct` entry to `domain/progression/PremiumShopCatalog.kt` - this is now the
   single source of truth for what each bundle grants (`CosmeticCollectionGrantor` reads it
   directly, no separate server-side copy to keep in sync).
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
