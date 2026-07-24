# Points Economy (Shop / Collections / Lucky Spin) setup

This project ships with a complete, production-ready Point Shop already wired in code - a real
32-item cosmetic catalog across 8 categories, a spend/purchase pipeline, a Collections gallery, and
a cosmetic-only Lucky Spin, all spending the existing `coins` currency players already earn from
gameplay. Like this project's other Firebase-backed features (`FIREBASE_SETUP.md`,
`LEADERBOARD_SETUP.md`), the client-side purchase flow works immediately (against the mock backend,
or against Firestore with only client-side/rules validation), but the **anti-cheat hardening layer**
- server-side verification that a coin deduction actually corresponds to a legitimate catalog
purchase - stays inert until you deploy the updated Cloud Functions and republish the Firestore
rules described below.

## What's already wired in code

- **Catalog**: `domain/progression/ShopCatalog.kt` - 32 cosmetics (4 rarities × 8 categories:
  Avatar Frames, Profile Borders, Name Colors, Timer Styles, Victory Animations, Confetti Effects,
  Sticker Packs, Trophy/Relics), all rendered from pure Compose primitives (gradients/color
  swatches/icon glyphs) - no external art assets to host or ship.
- **Spend/purchase**: `data/repository/ShopRepositoryImpl.kt`, mirroring
  `ProgressionRepositoryImpl`'s mock-backend/Firestore split exactly. Purchases/spins fail cleanly
  offline (no optimistic local grant) rather than queuing for later - a deliberate choice, since a
  purchase is a real economic transaction.
- **Lucky Spin**: `domain/progression/LuckySpinEngine.kt` - client-computed rarity/item roll
  (Common 60% / Rare 28% / Epic 10% / Legendary 2%), duplicate rolls refund 50% of the item's coin
  price instead of a no-op.
- **UI**: Profile → Shop / Collections / Lucky Spin buttons (`ui/screens/shop/`).

## Why this stays inert until you do the steps below

The Cloud Function that validates a `coins` decrease actually corresponds to a real catalog price
(`validatePurchaseReceipt` in `functions/src/index.ts`) is **not deployed automatically** - same
reasoning as `LEADERBOARD_SETUP.md`'s `validateGlobalStats`/`validateProfileWrite`: this repo has no
access to your Firebase CLI credentials. Until you deploy it, a purchase still works end-to-end
(the mock backend and the Firestore transaction both enforce affordability/ownership at write time),
but a modified/rooted client could in principle forge a `playerProfiles/{uid}` write with a
implausible coin decrease and it would only be caught by the *bounded* check already live in
`validateProfileWrite` (rejects a single-write decrease over 4,000 coins - well above the priciest
32-item catalog price of 3,500), not matched against an actual receipt, until you deploy.

## What you need to do

1. **Deploy the updated Cloud Functions** (adds `validatePurchaseReceipt` and
   `validateCosmeticsWrite`, and relaxes `validateProfileWrite`'s coin-decrease check from
   "never decreases" to "bounded decrease"):
   ```
   cd functions
   npm install
   firebase deploy --only functions
   ```
2. **Republish `firestore.rules`** (adds `playerCosmetics/{uid}` and `purchaseReceipts/{receiptId}`
   match blocks) via **Firestore Database → Rules** in the Firebase Console, or:
   ```
   firebase deploy --only firestore:rules
   ```
3. Nothing else - no new collections need pre-creating (Firestore creates `playerCosmetics/{uid}`
   and `purchaseReceipts/{uid}_{nonce}` documents on first write, same as every other collection in
   this app), no new console toggles, no new API to enable.

## Expanding the catalog later

Every new item is a pure data change, never new code:

1. Add a `CosmeticId` entry (`domain/model/CosmeticId.kt`).
2. Add its `CosmeticDefinition` (`domain/progression/ShopCatalog.kt`) - category, rarity, price.
3. Add its visual spec (`ui/theme/CosmeticVisualCatalog.kt`) - a color list + icon, no art asset.
4. Mirror its price in **both** `mock-backend/shop.js`'s `CATALOG_PRICES` and
   `functions/src/shopCatalog.ts`'s `SHOP_CATALOG_PRICES` (server-side validation reads these, not
   the Kotlin catalog - keep all three in sync, the existing "mirrors X" convention this codebase
   already uses for `ProgressionRules`).

Adding a brand-new **category** additionally means: extend `CosmeticCategory` (domain), give it a
render case in `CosmeticGlyph.kt`, and add its display name in `CosmeticDisplay.kt`'s
`toDisplayName()`.

## Live Events (seasonal drops)

`domain/progression/LiveEventCatalog.kt` ships with one permanently-expired template entry - the
framework is proven (unit-tested) but no seasonal content exists yet. To run a real event
(Halloween/Christmas/New Year/Summer/Anniversary): add the new cosmetics to `ShopCatalog` as above,
then add one `LiveEvent(id, startEpochSecond, endEpochSecond, featuredCosmeticIds)` entry to
`LiveEventCatalog.events`. No new screens, no new code paths.

## Out of scope by design (see the implementation plan for the full reasoning)

- Only 8 cosmetic categories ship (not the full 18-category wishlist) - all data-driven, easy to
  extend per above.
- No real seasonal content yet - the framework is proven, empty.
- No "Prestige" mechanic - "Memory Rank" (Profile) is a pure display-tier label derived from
  existing level, not a new earned/stored value.
- No offline purchase/spin retry queue - purchases/spins fail cleanly offline rather than queuing.
- No pity system for Lucky Spin - duplicate-refund is the only fairness mechanism.
- Nothing here ever touches `LeaderboardRepositoryImpl`, `players/{uid}`, periodic leaderboard
  entries, `ScoringEngine`, gameplay timer logic, or `AchievementCatalog`/`AchievementEvaluator` -
  the existing skill-based leaderboard and gameplay are provably unaffected.
