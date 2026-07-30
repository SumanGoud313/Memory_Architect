package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity

/**
 * Static catalog of every premium-only cosmetic - the real-money counterpart to [ShopCatalog],
 * deliberately kept as a **separate** object rather than folded into it, so nothing in the coin
 * purchase/spin path (`FirestoreShopRemoteSource.purchase`/`spin`, [GetShopCatalogUseCase],
 * [com.suman.memoryarchitect.domain.progression.LuckySpinEngine], the Shop's Coin tab) can ever
 * see or grant a premium id - those call sites stay on [ShopCatalog] alone by construction. Every
 * item here is granted exclusively through [PremiumShopCatalog]'s bundles, via a
 * server-verified Google Play purchase (see `functions/src/index.ts`'s `verifyPremiumPurchase`) or
 * `DebugTestGrantor.debugGrantPremiumProduct` in debug builds - never coin-purchasable, never a
 * Lucky Spin prize (`spinEligible = false` throughout).
 *
 * [CosmeticDefinition.priceCoins] is inert here (always `0L`) - only the coin-purchase transaction
 * path ever reads that field, and premium items never reach it. [CosmeticDefinition.isAnimated] is
 * only meaningful for [CosmeticCategory.PROFILE_BORDER] (see `PremiumBorder.kt`), so it's `true`
 * only for the border entries below, matching [ShopCatalog]'s own convention of leaving it `false`
 * everywhere else.
 *
 * Every id here needs a mirrored entry in `functions/src/premiumCatalog.ts` (which product grants
 * which ids) - see that file's doc - the same "keep the Kotlin and TypeScript catalogs in sync"
 * convention `shopCatalog.ts` already established for [ShopCatalog].
 */
object PremiumCatalog {
    val definitions: List<CosmeticDefinition> = listOf(
        // Founder's Pack
        CosmeticDefinition(CosmeticId.BORDER_FOUNDER_INAUGURAL, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.FRAME_FOUNDER_PIONEER, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_FOUNDER_GENESIS, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.NAME_COLOR_FOUNDER_ORIGIN, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_FOUNDER_LEGACY, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_FOUNDER_TRIUMPH, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.CONFETTI_FOUNDER_JUBILEE, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BADGE_FOUNDER_EMBLEM, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_FOUNDER_HERITAGE, CosmeticCategory.ROOM_SKIN, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),

        // Starter Bundle - an entry-level premium tier, priced/positioned below Founder's Pack
        // and the flagship collections (see PremiumShopCatalog.kt).
        CosmeticDefinition(CosmeticId.BORDER_STARTER_SUNRISE, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.RARE, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.BACKGROUND_STARTER_HORIZON, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.RARE, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_STARTER_COMPASS, CosmeticCategory.TIMER_STYLE, CosmeticRarity.RARE, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_STARTER_SPARK, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.RARE, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.FRAME_STARTER_WAYFARER, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.RARE, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_STARTER_DAWN, CosmeticCategory.ROOM_SKIN, CosmeticRarity.RARE, 0L, spinEligible = false),

        // Royal Collection
        CosmeticDefinition(CosmeticId.BORDER_ROYAL_CROWN, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.FRAME_ROYAL_CREST, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.NAME_COLOR_ROYAL_VELVET, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_ROYAL_HOURGLASS, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_ROYAL_FANFARE, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.CONFETTI_ROYAL_PETALS, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_ROYAL_THRONE, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BADGE_ROYAL_SEAL, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_ROYAL_PALACE, CosmeticCategory.ROOM_SKIN, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),

        // Cyber Collection
        CosmeticDefinition(CosmeticId.BORDER_CYBER_CIRCUIT, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.FRAME_CYBER_VISOR, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.NAME_COLOR_CYBER_GLITCH, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_CYBER_PULSE, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_CYBER_OVERDRIVE, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.CONFETTI_CYBER_SPARKS, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_CYBER_GRID, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BADGE_CYBER_CHIP, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_CYBER_GRIDWORKS, CosmeticCategory.ROOM_SKIN, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),

        // Space Collection
        CosmeticDefinition(CosmeticId.BORDER_SPACE_ORBIT, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.FRAME_SPACE_NEBULA, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.NAME_COLOR_SPACE_COSMOS, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_SPACE_PULSAR, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_SPACE_SUPERNOVA, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.CONFETTI_SPACE_STARDUST, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_SPACE_GALAXY, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BADGE_SPACE_COMET, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_SPACE_NEBULA_DOCK, CosmeticCategory.ROOM_SKIN, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),

        // Nature Collection
        CosmeticDefinition(CosmeticId.BORDER_NATURE_VINE, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.FRAME_NATURE_LEAF, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.NAME_COLOR_NATURE_MOSS, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_NATURE_BLOOM, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_NATURE_BLOSSOM, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.CONFETTI_NATURE_PETALS, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_NATURE_FOREST, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BADGE_NATURE_ACORN, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_NATURE_GROVE, CosmeticCategory.ROOM_SKIN, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),

        // Luxury Collection
        CosmeticDefinition(CosmeticId.BORDER_LUXURY_ONYX, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 0L, spinEligible = false, isAnimated = true),
        CosmeticDefinition(CosmeticId.FRAME_LUXURY_DIAMOND, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.NAME_COLOR_LUXURY_PLATINUM, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.TIMER_LUXURY_CHRONOGRAPH, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.VICTORY_LUXURY_SPOTLIGHT, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.CONFETTI_LUXURY_GOLDLEAF, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_LUXURY_PENTHOUSE, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BADGE_LUXURY_CREST, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
        CosmeticDefinition(CosmeticId.ROOM_LUXURY_SUITE, CosmeticCategory.ROOM_SKIN, CosmeticRarity.LEGENDARY, 0L, spinEligible = false),
    )

    private val byId: Map<CosmeticId, CosmeticDefinition> = definitions.associateBy { it.id }

    fun definitionOrNull(id: CosmeticId): CosmeticDefinition? = byId[id]
}
