# Remove Ads (Google Play Billing) setup

This project ships with a complete, production-ready lifetime "Remove Ads" purchase already wired
in code - see `core/billing/BillingManager.kt`, `BillingManagerImpl.kt`,
`ui/screens/removeads/RemoveAdsScreen.kt` (Settings → Remove Ads). Like this project's other
external integrations (`FIREBASE_SETUP.md`, `LEADERBOARD_SETUP.md`), it stays inert until one
product is created for this app in the **Google Play Console** - nothing else in this project can
do that step for you, it requires your own Play Console access and (for real, non-test purchases)
an app that's already been uploaded to at least an internal testing track.

## What you need to create

**Play Console → your app → Monetize → Products → In-app products → Create product**

| Field | Value |
|---|---|
| Product ID | `remove_ads_lifetime` (must match exactly - hardcoded in `BillingManagerImpl.REMOVE_ADS_PRODUCT_ID`) |
| Product type | Managed product (non-consumable) - Play Console calls this an "in-app product," not a subscription |
| Name | e.g. "Remove Ads (Lifetime)" |
| Description | e.g. "Removes all ads permanently for this Google Play account." |

## Regional pricing

Set the **base price in USD** under **Play Console → the product → Price** to:

| Product | Base price (USD) |
|---|---|
| Remove Ads (Lifetime) | $3.99 |

Then let Play Console auto-convert every other region's price from that base using its own current
exchange rates and local pricing patterns (e.g. psychological rounding), rather than typing in
converted values yourself - that auto-conversion is what makes every player automatically see their
own local currency and a region-appropriate price with Google Play handling tax, with no
per-country price ever hardcoded in this app.

**Nothing in the app hardcodes any of this.** `BillingManagerImpl` only ever reads
`ProductDetails.oneTimePurchaseOfferDetails.formattedPrice` - Play's own already-localized price
string for whichever account/region is actually asking - so every player always sees their own
currency, sourced entirely from what you configure here.

## Testing before going live

1. Add your own Google account (and any testers') under **Play Console → Setup → License
   testing** - license testers can "buy" the product with a test payment method that never
   actually charges.
2. Upload a signed build to at least the **Internal testing** track (Play Billing purchase flows
   do not work against a locally-signed debug APK installed via `adb install` outside of a
   Play-distributed build for a license tester's account - this is a Play Store platform
   requirement, not a limitation of this app's code).
3. Install the app **from the Play Store** (internal testing link) on a license-tester account and
   confirm: Buy Now shows the real localized price, completes a test purchase, ads-removed state
   persists, and Restore Purchase works after uninstall/reinstall on the same account.

## What already works once the product exists

- **Localized pricing** - read live from Play, never hardcoded (see above).
- **Purchase flow** - `Buy Now` launches Play's own purchase UI; every supported local payment
  method (cards, UPI, carrier billing, Google Pay, gift cards, etc.) is Play's to offer, this app
  never touches payment details.
- **Acknowledgement** - every granted purchase is acknowledged automatically, so Play never
  auto-refunds it after 3 days for being unacknowledged.
- **Automatic restore** - `MemoryArchitectApp.onCreate()` connects to Play Billing and re-queries
  this account's owned products on every cold start, so a reinstall or a new device signed into
  the same Google Play account is ad-free again with no action needed. `Restore Purchase` on the
  Remove Ads screen is the same query, run again explicitly, for the rare case the automatic one
  missed (e.g. it ran before the device ever came online).
- **Pending purchases** - a payment method that settles asynchronously (UPI collect, carrier
  billing) is surfaced as "Purchase pending," not a failure; the entitlement grants itself the
  moment Play reports it as actually purchased.

## Why no server-side purchase verification

This app verifies purchases via `PurchaseSignatureVerifier` (checks the Play Billing Library's own
purchase signature against your app's Play Console public key, entirely on-device - a patched
client can't forge this without Google's private signing key, so this is real, meaningful
protection against a fabricated purchase) plus `CosmeticCollectionGrantor`'s Firestore replay guard
(`claimedPurchaseTokens/{sha256(token)}`, enforced by `firestore.rules`, not a server). This project
deliberately runs no Cloud Function anywhere - a from-day-one decision to stay on the free Firebase
Spark plan (see the Spark migration report) - so the Google Play Developer API's server-side
purchase-state polling is out of scope by design, not a gap awaiting future hardening. The one thing
that trade-off does give up: this app never learns if a purchase is later refunded/charged back, so
a refunded purchase's grant is never automatically revoked (only Google's own Developer API would
know that happened) - a real, accepted trade-off for a game with no subscription/renewal revenue at
stake.
