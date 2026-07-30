# Points Economy (Shop / Collections / Lucky Spin) setup

This project ships with a complete, production-ready Point Shop already wired in code - a real
32-item cosmetic catalog across 8 categories, a spend/purchase pipeline, a Collections gallery, and
a cosmetic-only Lucky Spin, all spending the existing `coins` currency players already earn from
gameplay. This project runs entirely on the free Firebase Spark plan - no Cloud Functions anywhere
(see the Spark migration report) - so every check on a purchase is either the mock backend (dev
only), a Firestore transaction, or a `firestore.rules` shape/range bound; there is no server-side
re-verification that a coin deduction matches a specific catalog receipt after the fact. This is an
accepted, documented trade-off, not a setup step you still need to complete.

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

## What actually protects a purchase today

Both the mock backend and the real Firestore transaction (`FirestoreShopRemoteSource.purchase`)
enforce affordability/ownership at write time - a purchase can't go through without enough coins or
grant an item twice. On top of that, `firestore.rules`' `isValidProfile` rejects any single write
that decreases `coins` by more than 4,000 (well above the priciest 32-item catalog price of 3,500),
regardless of whether it's paired with a real receipt. What's genuinely not checked: that a specific
coin decrease matches a specific catalog item's exact price - a modified/rooted client could in
principle forge a plausible-looking decrease not tied to any real purchase, which only a Cloud
Function re-deriving "does this receipt's price match this sku's real catalog price" could close.
This project deliberately doesn't run one (see the Spark migration report).

## Setup

Nothing to deploy - `playerCosmetics/{uid}` and `purchaseReceipts/{uid}_{nonce}` are already covered
by the checked-in `firestore.rules` (publish it via **Firestore Database → Rules** in the Firebase
Console, or `firebase deploy --only firestore:rules`, same as every other collection in this app).
No new collections need pre-creating - Firestore creates them on first write - no new console
toggles, no new API to enable.

## Expanding the catalog later

Every new item is a pure data change, never new code:

1. Add a `CosmeticId` entry (`domain/model/CosmeticId.kt`).
2. Add its `CosmeticDefinition` (`domain/progression/ShopCatalog.kt`) - category, rarity, price.
3. Add its visual spec (`ui/theme/CosmeticVisualCatalog.kt`) - a color list + icon, no art asset.
4. Mirror its price in `mock-backend/shop.js`'s `CATALOG_PRICES` (the dev-only mock backend's own
   copy) - keep both in sync, the existing "mirrors X" convention this codebase already uses for
   `ProgressionRules`.

Adding a brand-new **category** additionally means: extend `CosmeticCategory` (domain), give it a
render case in `CosmeticGlyph.kt`, and add its display name in `CosmeticDisplay.kt`'s
`toDisplayName()`.

## Live Events (seasonal drops)

`domain/progression/LiveEventCatalog.kt` ships nine real templates (New Year, Valentine's Day,
Holi, Summer, Independence Day, Halloween, Diwali, Christmas, Anniversary), each with a default
window and a curated `featuredCosmeticIds` list drawn from existing `ShopCatalog`/`PremiumCatalog`
entries - no exclusive-to-event cosmetics. Which template (if any) is actually live is decided at
runtime by Remote Config, not by this file's default windows - see `FIREBASE_SETUP.md`'s Remote
Config table. To add a tenth event: add its cosmetics to `ShopCatalog` as above, then add one
`LiveEvent(id, startEpochSecond, endEpochSecond, featuredCosmeticIds)` entry to
`LiveEventCatalog.events`. No new screens, no new code paths.

## Out of scope by design (see the implementation plan for the full reasoning)

- Only 8 cosmetic categories ship (not the full 18-category wishlist) - all data-driven, easy to
  extend per above.
- Nine seasonal templates ship with real windows/featured cosmetics, but going live in production
  is still a Remote Config action (see `FIREBASE_SETUP.md`), not automatic.
- No "Prestige" mechanic - "Memory Rank" (Profile) is a pure display-tier label derived from
  existing level, not a new earned/stored value.
- No offline purchase/spin retry queue - purchases/spins fail cleanly offline rather than queuing.
- No pity system for Lucky Spin - duplicate-refund is the only fairness mechanism.
- Nothing here ever touches `LeaderboardRepositoryImpl`, `players/{uid}`, periodic leaderboard
  entries, `ScoringEngine`, gameplay timer logic, or `AchievementCatalog`/`AchievementEvaluator` -
  the existing skill-based leaderboard and gameplay are provably unaffected.
