package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity

/**
 * Static, client-known catalog of Point Shop cosmetics - same "the catalog itself is never
 * persisted" approach as [RewardCatalog]/[com.suman.memoryarchitect.domain.achievements.AchievementCatalog].
 * Four items per [CosmeticCategory] (one per [CosmeticRarity]), priced from [ShopRules]' bands.
 * `mock-backend/shop.js` and `functions/src/shopCatalog.ts` mirror this table's prices exactly -
 * keep all three in sync, the same convention [RewardCatalog]'s sibling systems already use.
 */
object ShopCatalog {
    val definitions: List<CosmeticDefinition> = listOf(
        // Avatar Frames
        CosmeticDefinition(CosmeticId.FRAME_WOVEN_CORD, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.COMMON, 560L),
        CosmeticDefinition(CosmeticId.FRAME_SILVER_LAUREL, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.RARE, 1070L),
        CosmeticDefinition(CosmeticId.FRAME_MOLTEN_BRONZE, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.EPIC, 2070L),
        CosmeticDefinition(CosmeticId.FRAME_CELESTIAL_HALO, CosmeticCategory.AVATAR_FRAME, CosmeticRarity.LEGENDARY, 4570L),

        // Premium Borders (flagship category, see CosmeticId.kt's doc) - 12 items, 3 per rarity.
        // Epic/Legendary are isAnimated = true (a slow, subtle rotating/shimmering gradient - see
        // PremiumBorder.kt); Common/Rare stay static.
        CosmeticDefinition(CosmeticId.BORDER_CLASSIC_SILVER, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.COMMON, 540L),
        CosmeticDefinition(CosmeticId.BORDER_PEARL_WHITE, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.COMMON, 620L),
        CosmeticDefinition(CosmeticId.BORDER_OBSIDIAN, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.COMMON, 720L),
        CosmeticDefinition(CosmeticId.BORDER_PLATINUM, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.RARE, 980L),
        CosmeticDefinition(CosmeticId.BORDER_EMERALD, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.RARE, 1150L),
        CosmeticDefinition(CosmeticId.BORDER_SAPPHIRE, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.RARE, 1320L),
        CosmeticDefinition(CosmeticId.BORDER_RUBY, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.EPIC, 1930L, isAnimated = true),
        CosmeticDefinition(CosmeticId.BORDER_CYBER_NEON, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.EPIC, 2200L, isAnimated = true),
        CosmeticDefinition(CosmeticId.BORDER_LAVA, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.EPIC, 2470L, isAnimated = true),
        CosmeticDefinition(CosmeticId.BORDER_ROYAL_GOLD, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 4030L, isAnimated = true),
        CosmeticDefinition(CosmeticId.BORDER_DIAMOND_GLOW, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 4700L, isAnimated = true),
        CosmeticDefinition(CosmeticId.BORDER_GALAXY, CosmeticCategory.PROFILE_BORDER, CosmeticRarity.LEGENDARY, 5370L, isAnimated = true),

        // Name Colors
        CosmeticDefinition(CosmeticId.NAME_COLOR_SLATE, CosmeticCategory.NAME_COLOR, CosmeticRarity.COMMON, 500L),
        CosmeticDefinition(CosmeticId.NAME_COLOR_OCEAN_FADE, CosmeticCategory.NAME_COLOR, CosmeticRarity.RARE, 930L),
        CosmeticDefinition(CosmeticId.NAME_COLOR_SUNSET_GRADIENT, CosmeticCategory.NAME_COLOR, CosmeticRarity.EPIC, 1870L),
        CosmeticDefinition(CosmeticId.NAME_COLOR_PRISM_SHIFT, CosmeticCategory.NAME_COLOR, CosmeticRarity.LEGENDARY, 4030L),

        // Timer Styles
        CosmeticDefinition(CosmeticId.TIMER_CLASSIC_DOT, CosmeticCategory.TIMER_STYLE, CosmeticRarity.COMMON, 600L),
        CosmeticDefinition(CosmeticId.TIMER_PULSE_RING, CosmeticCategory.TIMER_STYLE, CosmeticRarity.RARE, 1030L),
        CosmeticDefinition(CosmeticId.TIMER_EMBER_SWEEP, CosmeticCategory.TIMER_STYLE, CosmeticRarity.EPIC, 2000L),
        CosmeticDefinition(CosmeticId.TIMER_STARLIGHT_ARC, CosmeticCategory.TIMER_STYLE, CosmeticRarity.LEGENDARY, 4700L),

        // Victory Animations
        CosmeticDefinition(CosmeticId.VICTORY_CONFETTI_POP, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.COMMON, 640L),
        CosmeticDefinition(CosmeticId.VICTORY_GOLDEN_SPARKS, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.RARE, 1150L),
        CosmeticDefinition(CosmeticId.VICTORY_FIREWORK_BURST, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.EPIC, 2200L),
        CosmeticDefinition(CosmeticId.VICTORY_SUPERNOVA, CosmeticCategory.VICTORY_ANIMATION, CosmeticRarity.LEGENDARY, 5100L),

        // Confetti Effects
        CosmeticDefinition(CosmeticId.CONFETTI_PAPER_TOSS, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.COMMON, 540L),
        CosmeticDefinition(CosmeticId.CONFETTI_RIBBON_FALL, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.RARE, 950L),
        CosmeticDefinition(CosmeticId.CONFETTI_STAR_SHOWER, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.EPIC, 1910L),
        CosmeticDefinition(CosmeticId.CONFETTI_RAINBOW_CASCADE, CosmeticCategory.CONFETTI_EFFECT, CosmeticRarity.LEGENDARY, 4170L),

        // Sticker Packs
        CosmeticDefinition(CosmeticId.STICKERS_DOODLE_PACK, CosmeticCategory.STICKER_PACK, CosmeticRarity.COMMON, 580L),
        CosmeticDefinition(CosmeticId.STICKERS_ADVENTURE_PACK, CosmeticCategory.STICKER_PACK, CosmeticRarity.RARE, 1020L),
        CosmeticDefinition(CosmeticId.STICKERS_COSMIC_PACK, CosmeticCategory.STICKER_PACK, CosmeticRarity.EPIC, 2130L),
        CosmeticDefinition(CosmeticId.STICKERS_MYTHIC_PACK, CosmeticCategory.STICKER_PACK, CosmeticRarity.LEGENDARY, 4830L),

        // Trophy / Relics
        CosmeticDefinition(CosmeticId.TROPHY_BRONZE_CUP, CosmeticCategory.TROPHY_RELIC, CosmeticRarity.COMMON, 620L),
        CosmeticDefinition(CosmeticId.TROPHY_SILVER_COMPASS, CosmeticCategory.TROPHY_RELIC, CosmeticRarity.RARE, 1100L),
        CosmeticDefinition(CosmeticId.TROPHY_GOLDEN_HOURGLASS, CosmeticCategory.TROPHY_RELIC, CosmeticRarity.EPIC, 2330L),
        CosmeticDefinition(CosmeticId.TROPHY_DIAMOND_CROWN, CosmeticCategory.TROPHY_RELIC, CosmeticRarity.LEGENDARY, 5500L),

        // Background Themes - recolors AmbientBackground's gradient/glow/particle palette app-wide,
        // see CosmeticCategory.kt's doc. Epic/Legendary drift slowly via the ambient particle
        // field's own always-on motion (no isAnimated flag needed - that field only affects
        // PROFILE_BORDER rendering, see PremiumBorder.kt).
        CosmeticDefinition(CosmeticId.BACKGROUND_MISTY_DAWN, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.COMMON, 580L),
        // Every player already owns this for free (see PermanentFreeCosmetics) - not a meaningful
        // Lucky Spin prize any more, though it stays listed here for its price/rarity/category
        // metadata (ShopScreen shows it as owned rather than purchasable).
        CosmeticDefinition(CosmeticId.BACKGROUND_TWILIGHT_HAZE, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.RARE, 1030L, spinEligible = false),
        CosmeticDefinition(CosmeticId.BACKGROUND_AURORA_DRIFT, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.EPIC, 2000L),
        CosmeticDefinition(CosmeticId.BACKGROUND_STARFIELD_DEEP, CosmeticCategory.BACKGROUND_THEME, CosmeticRarity.LEGENDARY, 4700L),

        // Profile Badges
        CosmeticDefinition(CosmeticId.BADGE_BRONZE_LAUREL, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.COMMON, 600L),
        CosmeticDefinition(CosmeticId.BADGE_SILVER_SEAL, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.RARE, 1070L),
        CosmeticDefinition(CosmeticId.BADGE_GOLDEN_CREST, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.EPIC, 2070L),
        CosmeticDefinition(CosmeticId.BADGE_ARCHITECT_SIGIL, CosmeticCategory.PROFILE_BADGE, CosmeticRarity.LEGENDARY, 4570L),
    )

    private val byId: Map<CosmeticId, CosmeticDefinition> = definitions.associateBy { it.id }

    fun definitionsOfCategory(category: CosmeticCategory): List<CosmeticDefinition> =
        definitions.filter { it.category == category }

    fun definitionsOfRarity(rarity: CosmeticRarity): List<CosmeticDefinition> =
        definitions.filter { it.rarity == rarity }

    fun requireDefinition(id: CosmeticId): CosmeticDefinition =
        byId[id] ?: throw NoSuchElementException("No CosmeticDefinition for $id")

    fun definitionOrNull(id: CosmeticId): CosmeticDefinition? = byId[id]
}
