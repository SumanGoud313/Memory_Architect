package com.suman.memoryarchitect.ui.theme

import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.domain.model.RewardId

/** A milestone-reward accent — a sweep of colors the equipped palette lends to
 * [com.suman.memoryarchitect.ui.screens.profile.LevelProgressRing] wherever it's shown. */
data class AccentPalette(val id: RewardId, val sweep: List<Color>)

/** One entry per [RewardId] palette reward — swap in for the default terracotta/gold sweep once
 * equipped. Kept separate from [MemoryArchitectColors] since these are player-earned, not the
 * app's own chrome. */
object AccentPaletteCatalog {
    private val byId: Map<RewardId, AccentPalette> = listOf(
        AccentPalette(RewardId.PALETTE_OCEAN, listOf(Color(0xFF3E7CB1), Color(0xFF7FD1C9), Color(0xFF3E7CB1))),
        AccentPalette(RewardId.PALETTE_FOREST, listOf(Color(0xFF4C7A4A), Color(0xFFA9C978), Color(0xFF4C7A4A))),
        AccentPalette(RewardId.PALETTE_ORCHID, listOf(Color(0xFF8E5AA6), Color(0xFFE38FC2), Color(0xFF8E5AA6))),
        AccentPalette(RewardId.PALETTE_MIDNIGHT, listOf(Color(0xFF4A4E8F), Color(0xFFC7CBEF), Color(0xFF4A4E8F))),
        AccentPalette(RewardId.PALETTE_SUNSET, listOf(Color(0xFFD1653E), Color(0xFFF2B366), Color(0xFFD1653E))),
        AccentPalette(RewardId.PALETTE_EMBER, listOf(Color(0xFFA13A2E), Color(0xFFE68A5C), Color(0xFFA13A2E))),
    ).associateBy { it.id }

    fun get(id: RewardId?): AccentPalette? = id?.let { byId[it] }
}