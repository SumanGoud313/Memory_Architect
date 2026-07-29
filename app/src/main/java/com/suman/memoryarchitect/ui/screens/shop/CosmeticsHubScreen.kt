package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/** Which tab of the Cosmetics Hub is showing - purely UI state, never persisted. [SHOP] is the
 * default: the hub used to show a near-blank landing page (header + an often-empty Showcase card
 * + 3 buttons that each pushed to a separate full screen) before a player could see any actual
 * content - this makes real Shop content visible the instant the hub opens, no extra tap.
 * Lucky Spin is deliberately not a third tab here - it moved to its own icon on
 * [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen], see `Routes.kt`'s
 * `Route.LuckySpin` doc. Inventory isn't a tab here either, for the same reason - Mode Select
 * already has its own dedicated Inventory icon/route, so a second copy nested inside this hub was
 * pure duplication. */
private enum class CosmeticsHubTab { SHOP, COLLECTIONS }

/** The cosmetics hub - Showcase card + Shop/Collections as tabs in one screen (Shop selected by
 * default), reached via the round "Cosmetics" corner button on
 * [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen] - the Classic/Daily Challenge/
 * Practice/Weekly Challenge mode-picker screen. Shop/Collections no longer have their own nav
 * routes or screen chrome - [ShopScreenBody]/[CollectionsScreenBody] are pure tab bodies, each
 * keeping its own [androidx.hilt.navigation.compose.hiltViewModel] so switching tabs and back
 * never refetches. Reads [LocalEquippedCosmetics] directly (the same app-wide reactive store every
 * equipped-cosmetic renderer already uses) for the Showcase card, so it can never go stale the way
 * a one-time ViewModel-scoped fetch would after equipping something in Collections and switching
 * back. */
@Composable
fun CosmeticsHubScreen(onBack: () -> Unit, startOnCollectionsTab: Boolean = false) {
    val particles = rememberParticleFieldState()
    val equippedCosmetics = LocalEquippedCosmetics.current
    var selectedTab by remember { mutableStateOf(if (startOnCollectionsTab) CosmeticsHubTab.COLLECTIONS else CosmeticsHubTab.SHOP) }

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.profile_cosmetics_header),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )

            val stickerPackId = equippedCosmetics[CosmeticCategory.STICKER_PACK]
            val trophyId = equippedCosmetics[CosmeticCategory.TROPHY_RELIC]
            if (stickerPackId != null || trophyId != null) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).staggeredReveal(1)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_showcase_header),
                            style = MaterialTheme.typography.labelLarge,
                            color = MemoryArchitectColors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        stickerPackId?.let { CosmeticGlyph(id = it, category = CosmeticCategory.STICKER_PACK, isOwned = true, sizeDp = 44.dp) }
                        trophyId?.let { CosmeticGlyph(id = it, category = CosmeticCategory.TROPHY_RELIC, isOwned = true, sizeDp = 44.dp) }
                    }
                }
            }

            CosmeticsHubTabRow(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).staggeredReveal(2),
            )

            when (selectedTab) {
                CosmeticsHubTab.SHOP -> ShopScreenBody()
                CosmeticsHubTab.COLLECTIONS -> CollectionsScreenBody(onOpenShop = { selectedTab = CosmeticsHubTab.SHOP })
            }
        }
    }
}

/** 🪙 Shop / Collections - same lightweight chip-row shape [ShopScreenBody]'s own Coin/Premium tab
 * row uses (this app has no shared `TabRow` primitive - each screen with tabs builds its own small
 * chip row). */
@Composable
private fun CosmeticsHubTabRow(selected: CosmeticsHubTab, onSelect: (CosmeticsHubTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CosmeticsHubTabChip(
            text = stringResource(R.string.profile_shop_header),
            isSelected = selected == CosmeticsHubTab.SHOP,
            onClick = { onSelect(CosmeticsHubTab.SHOP) },
            modifier = Modifier.weight(1f),
        )
        CosmeticsHubTabChip(
            text = stringResource(R.string.profile_collections_header),
            isSelected = selected == CosmeticsHubTab.COLLECTIONS,
            onClick = { onSelect(CosmeticsHubTab.COLLECTIONS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CosmeticsHubTabChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(MemoryArchitectRadii.chip)
    Box(
        modifier = modifier
            .background(
                if (isSelected) MemoryArchitectColors.accentGold.copy(alpha = 0.18f) else MemoryArchitectColors.glassFill,
                shape,
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (isSelected) MemoryArchitectColors.accentGold else MemoryArchitectColors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
