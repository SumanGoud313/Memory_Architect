package com.suman.memoryarchitect.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Warm premium palette — replaces the old "neon glass on deep space" chrome with soft,
 * lamplit-room tones. Deliberately still dark-only (matches the old theme's choice not to
 * follow system light/dark), just shifted from violet/cyan neon to terracotta/gold/sage warmth.
 */
object MemoryArchitectColors {
    // Base surfaces
    val bgBase = Color(0xFF1B1611)
    val bgElevated = Color(0xFF241D16)
    val bgGlow = Color(0xFF332619)
    val bgBaseAlt = Color(0xFF211A14)

    // Warm glass fills/strokes (translucent, layered over bgBase/bgElevated)
    val glassFill = Color(0x1FFFF3E6)
    val glassFillPressed = Color(0x33FFF3E6)
    val glassStroke = Color(0x33FFE3C2)
    val glassStrokeBright = Color(0x66FFE3C2)

    // Accents
    val accentTerracotta = Color(0xFFE8935B)
    val accentTerracottaDark = Color(0xFFB8623A)
    val accentGold = Color(0xFFF0C875)
    val accentGoldDark = Color(0xFFC79A46)
    val accentSage = Color(0xFF8FA383)
    val accentCoral = Color(0xFFE8674B)
    val accentAmber = Color(0xFFF2B441)
    val accentBlush = Color(0xFFE0937F)

    // Text
    val textPrimary = Color(0xFFF7F1E8)
    val textSecondary = Color(0xFFC9BFAF)
    val textTertiary = Color(0xFF948977)

    // Feedback
    val success = Color(0xFF8FBF8A)
    val warning = accentAmber
    val danger = accentCoral

    val rippleWarm = Color(0x40FFE3C2)
    val objectDropShadow = Color(0x4D1B1611)

    /** A soft, layered background gradient — three warm stops from base to a subtle glow. */
    val backgroundGradient = listOf(bgBase, bgBaseAlt, bgGlow)
}
