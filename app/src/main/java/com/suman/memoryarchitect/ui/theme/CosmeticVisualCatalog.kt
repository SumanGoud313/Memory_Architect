package com.suman.memoryarchitect.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.suman.memoryarchitect.domain.model.CosmeticId

/** How a Point Shop cosmetic actually renders - pure Compose primitives only (gradients/color
 * swatches/simple icon glyphs), no art assets, same "domain never imports Color" split
 * [AccentPaletteCatalog] already establishes for [com.suman.memoryarchitect.domain.model.RewardId].
 * [gradientColors] drives every category's Canvas renderer (frame sweep, border brush, name-color
 * gradient, etc.) - [icon] is only used where a category needs a glyph on top (stickers/trophies). */
data class CosmeticVisualSpec(
    val gradientColors: List<Color>,
    val icon: ImageVector,
)

object CosmeticVisualCatalog {
    private val specs: Map<CosmeticId, CosmeticVisualSpec> = mapOf(
        // Avatar Frames
        CosmeticId.FRAME_WOVEN_CORD to spec(listOf(Color(0xFF8A7A63), Color(0xFFB8A585)), Icons.Filled.Star),
        CosmeticId.FRAME_SILVER_LAUREL to spec(listOf(Color(0xFFC7CBD1), Color(0xFFE8EAEE)), Icons.Filled.Grade),
        CosmeticId.FRAME_MOLTEN_BRONZE to spec(listOf(Color(0xFFB8623A), MemoryArchitectColors.accentTerracotta), Icons.Filled.LocalFireDepartment),
        CosmeticId.FRAME_CELESTIAL_HALO to spec(listOf(Color(0xFFCBB8F0), MemoryArchitectColors.accentGold, Color(0xFFAEE9E8)), Icons.Filled.AutoAwesome),

        // Premium Borders (flagship category - see CosmeticId.kt's doc). Common/Rare are static;
        // Epic/Legendary are isAnimated (ShopCatalog.kt) and render via PremiumBorder.kt's
        // phase-shifted-gradient technique using these same color stops.
        CosmeticId.BORDER_CLASSIC_SILVER to spec(listOf(Color(0xFFA8ACB4), Color(0xFFDCDFE3)), Icons.Filled.Grade),
        CosmeticId.BORDER_PEARL_WHITE to spec(listOf(MemoryArchitectColors.textPrimary, Color(0xFFD9CDB8)), Icons.Filled.Star),
        CosmeticId.BORDER_OBSIDIAN to spec(listOf(Color(0xFF1F1B17), Color(0xFF4A3F35), Color(0xFF6B5D4F)), Icons.Filled.Bolt),
        CosmeticId.BORDER_PLATINUM to spec(listOf(Color(0xFFD8DBDF), Color(0xFFF4F6F8), Color(0xFFAEB4BC)), Icons.Filled.WorkspacePremium),
        CosmeticId.BORDER_EMERALD to spec(listOf(Color(0xFF0F6B4C), Color(0xFF3ED8A0)), Icons.Filled.Diamond),
        CosmeticId.BORDER_SAPPHIRE to spec(listOf(Color(0xFF1A4B8C), Color(0xFF5FA3E8)), Icons.Filled.Diamond),
        CosmeticId.BORDER_RUBY to spec(listOf(MemoryArchitectColors.accentCoral, Color(0xFF9E1B4D), MemoryArchitectColors.accentCoral), Icons.Filled.LocalFireDepartment),
        CosmeticId.BORDER_CYBER_NEON to spec(listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1), Color(0xFF00F0FF)), Icons.Filled.Bolt),
        CosmeticId.BORDER_LAVA to spec(listOf(MemoryArchitectColors.accentTerracottaDark, MemoryArchitectColors.accentCoral, MemoryArchitectColors.accentAmber), Icons.Filled.LocalFireDepartment),
        CosmeticId.BORDER_ROYAL_GOLD to spec(listOf(MemoryArchitectColors.accentGoldDark, MemoryArchitectColors.accentGold, Color(0xFFFFF3D6), MemoryArchitectColors.accentGoldDark), Icons.Filled.WorkspacePremium),
        CosmeticId.BORDER_DIAMOND_GLOW to spec(listOf(Color(0xFFE8F7FF), Color(0xFFAEE9E8), Color(0xFFCBB8F0), Color(0xFFE8F7FF)), Icons.Filled.Diamond),
        CosmeticId.BORDER_GALAXY to spec(listOf(Color(0xFF241D4A), Color(0xFF6A3FA0), Color(0xFFE07FA0), Color(0xFF241D4A)), Icons.Filled.AutoAwesome),

        // Name Colors
        // Two near-identical stops, not one - android.graphics.LinearGradient (which
        // Brush.linearGradient wraps) throws "needs >= 2 number of colors" for a single-color list.
        CosmeticId.NAME_COLOR_SLATE to spec(listOf(Color(0xFFC9BFAF), Color(0xFFB4A996)), Icons.Filled.Star),
        CosmeticId.NAME_COLOR_OCEAN_FADE to spec(listOf(Color(0xFF5FA3C7), Color(0xFF8FD1E8)), Icons.Filled.WbSunny),
        CosmeticId.NAME_COLOR_SUNSET_GRADIENT to spec(listOf(MemoryArchitectColors.accentCoral, MemoryArchitectColors.accentGold), Icons.Filled.WbSunny),
        CosmeticId.NAME_COLOR_PRISM_SHIFT to spec(listOf(Color(0xFFE07FA0), Color(0xFF8FA3E8), Color(0xFF7FE0C0)), Icons.Filled.AutoAwesome),

        // Timer Styles
        CosmeticId.TIMER_CLASSIC_DOT to spec(listOf(MemoryArchitectColors.accentSage, MemoryArchitectColors.accentGold), Icons.Filled.Bolt),
        CosmeticId.TIMER_PULSE_RING to spec(listOf(Color(0xFF5FA3C7), Color(0xFFAEE9E8)), Icons.Filled.Bolt),
        CosmeticId.TIMER_EMBER_SWEEP to spec(listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentCoral), Icons.Filled.Bolt),
        CosmeticId.TIMER_STARLIGHT_ARC to spec(listOf(Color(0xFFCBB8F0), MemoryArchitectColors.accentGold), Icons.Filled.Bolt),

        // Victory Animations
        CosmeticId.VICTORY_CONFETTI_POP to spec(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentCoral), Icons.Filled.EmojiEvents),
        CosmeticId.VICTORY_GOLDEN_SPARKS to spec(listOf(MemoryArchitectColors.accentGold, Color(0xFFFFF3D6)), Icons.Filled.AutoAwesome),
        CosmeticId.VICTORY_FIREWORK_BURST to spec(listOf(Color(0xFFE07FA0), Color(0xFF8FA3E8), MemoryArchitectColors.accentGold), Icons.Filled.EmojiEvents),
        CosmeticId.VICTORY_SUPERNOVA to spec(listOf(Color(0xFFAEE9E8), Color(0xFFCBB8F0), MemoryArchitectColors.accentGold, MemoryArchitectColors.accentCoral), Icons.Filled.AutoAwesome),

        // Confetti Effects
        CosmeticId.CONFETTI_PAPER_TOSS to spec(listOf(Color(0xFFC9BFAF), MemoryArchitectColors.accentGold), Icons.Filled.Star),
        CosmeticId.CONFETTI_RIBBON_FALL to spec(listOf(MemoryArchitectColors.accentCoral, Color(0xFF8FA3E8)), Icons.Filled.Star),
        CosmeticId.CONFETTI_STAR_SHOWER to spec(listOf(MemoryArchitectColors.accentGold, Color(0xFFAEE9E8)), Icons.Filled.Star),
        CosmeticId.CONFETTI_RAINBOW_CASCADE to spec(listOf(Color(0xFFE07FA0), MemoryArchitectColors.accentAmber, Color(0xFF7FE0C0), Color(0xFF8FA3E8)), Icons.Filled.AutoAwesome),

        // Sticker Packs
        CosmeticId.STICKERS_DOODLE_PACK to spec(listOf(MemoryArchitectColors.accentSage, Color(0xFFC9BFAF)), Icons.Filled.Star),
        CosmeticId.STICKERS_ADVENTURE_PACK to spec(listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentAmber), Icons.Filled.WbSunny),
        CosmeticId.STICKERS_COSMIC_PACK to spec(listOf(Color(0xFF8FA3E8), Color(0xFFCBB8F0)), Icons.Filled.AutoAwesome),
        CosmeticId.STICKERS_MYTHIC_PACK to spec(listOf(MemoryArchitectColors.accentGold, Color(0xFFE07FA0), Color(0xFF7FE0C0)), Icons.Filled.AutoAwesome),

        // Trophy / Relics
        CosmeticId.TROPHY_BRONZE_CUP to spec(listOf(Color(0xFFB08D57), Color(0xFFD4B483)), Icons.Filled.EmojiEvents),
        CosmeticId.TROPHY_SILVER_COMPASS to spec(listOf(Color(0xFFC7CBD1), Color(0xFFE8EAEE)), Icons.Filled.EmojiEvents),
        CosmeticId.TROPHY_GOLDEN_HOURGLASS to spec(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentGoldDark), Icons.Filled.WorkspacePremium),
        CosmeticId.TROPHY_DIAMOND_CROWN to spec(listOf(Color(0xFFAEE9E8), Color(0xFFCBB8F0), MemoryArchitectColors.accentGold), Icons.Filled.Diamond),

        // Background Themes (coin-tier)
        CosmeticId.BACKGROUND_MISTY_DAWN to spec(listOf(Color(0xFFB8C4CC), Color(0xFFE8D9C4)), Icons.Filled.WbSunny),
        CosmeticId.BACKGROUND_TWILIGHT_HAZE to spec(listOf(Color(0xFF5F6A8C), Color(0xFFC98CA8)), Icons.Filled.WbSunny),
        CosmeticId.BACKGROUND_AURORA_DRIFT to spec(listOf(Color(0xFF4FBF9F), Color(0xFF8FA3E8), MemoryArchitectColors.accentGold), Icons.Filled.AutoAwesome),
        CosmeticId.BACKGROUND_STARFIELD_DEEP to spec(listOf(Color(0xFF0B1033), Color(0xFF6A3FA0), Color(0xFFAEE9E8), Color(0xFF241D4A)), Icons.Filled.AutoAwesome),

        // Profile Badges (coin-tier)
        CosmeticId.BADGE_BRONZE_LAUREL to spec(listOf(Color(0xFFB08D57), Color(0xFFD4B483)), Icons.Filled.EmojiEvents),
        CosmeticId.BADGE_SILVER_SEAL to spec(listOf(Color(0xFFC7CBD1), Color(0xFFE8EAEE)), Icons.Filled.Grade),
        CosmeticId.BADGE_GOLDEN_CREST to spec(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentGoldDark), Icons.Filled.WorkspacePremium),
        CosmeticId.BADGE_ARCHITECT_SIGIL to spec(listOf(Color(0xFFAEE9E8), Color(0xFFCBB8F0), MemoryArchitectColors.accentGold), Icons.Filled.WorkspacePremium),

        // Founder's Pack (premium) - warm terracotta/gold/amber, the app's own signature accent
        // colors, tying "founder" identity to the brand itself rather than any one collection's theme.
        CosmeticId.BORDER_FOUNDER_INAUGURAL to spec(listOf(MemoryArchitectColors.accentTerracottaDark, MemoryArchitectColors.accentGold, MemoryArchitectColors.accentAmber, MemoryArchitectColors.accentTerracottaDark), Icons.Filled.WorkspacePremium),
        CosmeticId.FRAME_FOUNDER_PIONEER to spec(listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold), Icons.Filled.WorkspacePremium),
        CosmeticId.BACKGROUND_FOUNDER_GENESIS to spec(listOf(MemoryArchitectColors.accentTerracottaDark, MemoryArchitectColors.accentAmber), Icons.Filled.AutoAwesome),
        CosmeticId.NAME_COLOR_FOUNDER_ORIGIN to spec(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentTerracotta), Icons.Filled.WorkspacePremium),
        CosmeticId.TIMER_FOUNDER_LEGACY to spec(listOf(MemoryArchitectColors.accentAmber, MemoryArchitectColors.accentGold), Icons.Filled.Bolt),
        CosmeticId.VICTORY_FOUNDER_TRIUMPH to spec(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentTerracotta, Color(0xFFFFF3D6)), Icons.Filled.EmojiEvents),
        CosmeticId.CONFETTI_FOUNDER_JUBILEE to spec(listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold, MemoryArchitectColors.accentAmber), Icons.Filled.AutoAwesome),
        CosmeticId.BADGE_FOUNDER_EMBLEM to spec(listOf(MemoryArchitectColors.accentGoldDark, MemoryArchitectColors.accentGold, Color(0xFFFFF3D6)), Icons.Filled.WorkspacePremium),

        // Starter Bundle (premium) - gentler sunrise tones, an entry-level premium tier positioned
        // below the flagship collections.
        CosmeticId.BORDER_STARTER_SUNRISE to spec(listOf(MemoryArchitectColors.accentAmber, Color(0xFFF4C77A)), Icons.Filled.WbSunny),
        CosmeticId.BACKGROUND_STARTER_HORIZON to spec(listOf(Color(0xFFF4C77A), MemoryArchitectColors.accentSage), Icons.Filled.WbSunny),
        CosmeticId.TIMER_STARTER_COMPASS to spec(listOf(MemoryArchitectColors.accentAmber, MemoryArchitectColors.accentSage), Icons.Filled.Bolt),
        CosmeticId.VICTORY_STARTER_SPARK to spec(listOf(MemoryArchitectColors.accentAmber, Color(0xFFF4C77A)), Icons.Filled.AutoAwesome),
        CosmeticId.FRAME_STARTER_WAYFARER to spec(listOf(Color(0xFFF4C77A), MemoryArchitectColors.accentAmber), Icons.Filled.WbSunny),

        // Royal Collection (premium) - deep purple + royal gold
        CosmeticId.BORDER_ROYAL_CROWN to spec(listOf(Color(0xFF3A1B5C), Color(0xFFD4AF37), Color(0xFF6A3FA0), Color(0xFF3A1B5C)), Icons.Filled.WorkspacePremium),
        CosmeticId.FRAME_ROYAL_CREST to spec(listOf(Color(0xFF6A3FA0), Color(0xFFD4AF37)), Icons.Filled.WorkspacePremium),
        CosmeticId.NAME_COLOR_ROYAL_VELVET to spec(listOf(Color(0xFF3A1B5C), Color(0xFFD4AF37)), Icons.Filled.WorkspacePremium),
        CosmeticId.TIMER_ROYAL_HOURGLASS to spec(listOf(Color(0xFFD4AF37), Color(0xFF6A3FA0)), Icons.Filled.Bolt),
        CosmeticId.VICTORY_ROYAL_FANFARE to spec(listOf(Color(0xFFD4AF37), Color(0xFF6A3FA0), Color(0xFFFFF3D6)), Icons.Filled.EmojiEvents),
        CosmeticId.CONFETTI_ROYAL_PETALS to spec(listOf(Color(0xFF6A3FA0), Color(0xFFD4AF37), Color(0xFFE07FA0)), Icons.Filled.AutoAwesome),
        CosmeticId.BACKGROUND_ROYAL_THRONE to spec(listOf(Color(0xFF3A1B5C), Color(0xFF6A3FA0), Color(0xFFD4AF37)), Icons.Filled.AutoAwesome),
        CosmeticId.BADGE_ROYAL_SEAL to spec(listOf(Color(0xFFD4AF37), Color(0xFF3A1B5C)), Icons.Filled.WorkspacePremium),

        // Cyber Collection (premium) - neon cyan/magenta on dark navy
        CosmeticId.BORDER_CYBER_CIRCUIT to spec(listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1), Color(0xFF0A0E27), Color(0xFF00F0FF)), Icons.Filled.Bolt),
        CosmeticId.FRAME_CYBER_VISOR to spec(listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1)), Icons.Filled.Bolt),
        CosmeticId.NAME_COLOR_CYBER_GLITCH to spec(listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1)), Icons.Filled.Bolt),
        CosmeticId.TIMER_CYBER_PULSE to spec(listOf(Color(0xFFFF2DD1), Color(0xFF00F0FF)), Icons.Filled.Bolt),
        CosmeticId.VICTORY_CYBER_OVERDRIVE to spec(listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1), Color(0xFF0A0E27)), Icons.Filled.Bolt),
        CosmeticId.CONFETTI_CYBER_SPARKS to spec(listOf(Color(0xFF00F0FF), Color(0xFFFF2DD1), Color(0xFFAEE9E8)), Icons.Filled.Bolt),
        CosmeticId.BACKGROUND_CYBER_GRID to spec(listOf(Color(0xFF0A0E27), Color(0xFF00F0FF), Color(0xFFFF2DD1)), Icons.Filled.Bolt),
        CosmeticId.BADGE_CYBER_CHIP to spec(listOf(Color(0xFF00F0FF), Color(0xFF0A0E27)), Icons.Filled.Bolt),

        // Space Collection (premium) - deep indigo/violet/starlight
        CosmeticId.BORDER_SPACE_ORBIT to spec(listOf(Color(0xFF0B1033), Color(0xFF6A3FA0), Color(0xFFAEE9E8), Color(0xFF0B1033)), Icons.Filled.RocketLaunch),
        CosmeticId.FRAME_SPACE_NEBULA to spec(listOf(Color(0xFF6A3FA0), Color(0xFFE07FA0)), Icons.Filled.RocketLaunch),
        CosmeticId.NAME_COLOR_SPACE_COSMOS to spec(listOf(Color(0xFF0B1033), Color(0xFF6A3FA0)), Icons.Filled.RocketLaunch),
        CosmeticId.TIMER_SPACE_PULSAR to spec(listOf(Color(0xFFAEE9E8), Color(0xFF6A3FA0)), Icons.Filled.Bolt),
        CosmeticId.VICTORY_SPACE_SUPERNOVA to spec(listOf(Color(0xFFAEE9E8), Color(0xFFCBB8F0), MemoryArchitectColors.accentGold, Color(0xFFE07FA0)), Icons.Filled.AutoAwesome),
        CosmeticId.CONFETTI_SPACE_STARDUST to spec(listOf(Color(0xFFAEE9E8), Color(0xFF6A3FA0), Color(0xFFE07FA0)), Icons.Filled.AutoAwesome),
        CosmeticId.BACKGROUND_SPACE_GALAXY to spec(listOf(Color(0xFF0B1033), Color(0xFF6A3FA0), Color(0xFFE07FA0), Color(0xFF241D4A)), Icons.Filled.RocketLaunch),
        CosmeticId.BADGE_SPACE_COMET to spec(listOf(Color(0xFFAEE9E8), Color(0xFF0B1033)), Icons.Filled.RocketLaunch),

        // Nature Collection (premium) - forest green/leaf/earth
        CosmeticId.BORDER_NATURE_VINE to spec(listOf(Color(0xFF2E5339), Color(0xFF8FBF6F), MemoryArchitectColors.accentSage, Color(0xFF2E5339)), Icons.Filled.Park),
        CosmeticId.FRAME_NATURE_LEAF to spec(listOf(Color(0xFF8FBF6F), Color(0xFF2E5339)), Icons.Filled.Park),
        CosmeticId.NAME_COLOR_NATURE_MOSS to spec(listOf(Color(0xFF2E5339), Color(0xFF8FBF6F)), Icons.Filled.Park),
        CosmeticId.TIMER_NATURE_BLOOM to spec(listOf(MemoryArchitectColors.accentSage, Color(0xFF8FBF6F)), Icons.Filled.Bolt),
        CosmeticId.VICTORY_NATURE_BLOSSOM to spec(listOf(Color(0xFF8FBF6F), Color(0xFFE07FA0), MemoryArchitectColors.accentSage), Icons.Filled.AutoAwesome),
        CosmeticId.CONFETTI_NATURE_PETALS to spec(listOf(Color(0xFFE07FA0), Color(0xFF8FBF6F), Color(0xFFC9BFAF)), Icons.Filled.AutoAwesome),
        CosmeticId.BACKGROUND_NATURE_FOREST to spec(listOf(Color(0xFF2E5339), Color(0xFF8FBF6F), Color(0xFFC9BFAF)), Icons.Filled.Park),
        CosmeticId.BADGE_NATURE_ACORN to spec(listOf(Color(0xFF8FBF6F), Color(0xFF2E5339)), Icons.Filled.Park),

        // Luxury Collection (premium) - onyx black/platinum gold/diamond white
        CosmeticId.BORDER_LUXURY_ONYX to spec(listOf(Color(0xFF16130F), MemoryArchitectColors.accentGoldDark, MemoryArchitectColors.accentGold, Color(0xFF16130F)), Icons.Filled.Diamond),
        CosmeticId.FRAME_LUXURY_DIAMOND to spec(listOf(Color(0xFFE8F7FF), MemoryArchitectColors.accentGold), Icons.Filled.Diamond),
        CosmeticId.NAME_COLOR_LUXURY_PLATINUM to spec(listOf(Color(0xFF16130F), MemoryArchitectColors.accentGold), Icons.Filled.Diamond),
        CosmeticId.TIMER_LUXURY_CHRONOGRAPH to spec(listOf(MemoryArchitectColors.accentGold, Color(0xFF16130F)), Icons.Filled.Bolt),
        CosmeticId.VICTORY_LUXURY_SPOTLIGHT to spec(listOf(MemoryArchitectColors.accentGold, Color(0xFFFFF3D6), Color(0xFF16130F)), Icons.Filled.EmojiEvents),
        CosmeticId.CONFETTI_LUXURY_GOLDLEAF to spec(listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.accentGoldDark, Color(0xFFE8F7FF)), Icons.Filled.AutoAwesome),
        CosmeticId.BACKGROUND_LUXURY_PENTHOUSE to spec(listOf(Color(0xFF16130F), MemoryArchitectColors.accentGold, Color(0xFFE8F7FF)), Icons.Filled.Diamond),
        CosmeticId.BADGE_LUXURY_CREST to spec(listOf(MemoryArchitectColors.accentGold, Color(0xFF16130F)), Icons.Filled.Diamond),
    )

    private fun spec(colors: List<Color>, icon: ImageVector) = CosmeticVisualSpec(colors, icon)

    fun get(id: CosmeticId): CosmeticVisualSpec = specs.getValue(id)
}
