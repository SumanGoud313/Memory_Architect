package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LocalEquippedCosmetics
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The cosmetics hub - Showcase card + Shop/Collections/Lucky Spin entry points, reached via the
 * round "Cosmetics" corner button (top-left) on [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen]
 * - the Classic/Daily Challenge/Practice/Weekly Challenge mode-picker screen.
 * Moved here wholesale from Profile's old "Cosmetics" section - same content, same [OutlineButton]
 * row, same Showcase card, just relocated. Reads [LocalEquippedCosmetics] directly (the same
 * app-wide reactive store every equipped-cosmetic renderer already uses) rather than a
 * screen-owned ViewModel fetch, so the Showcase card can never go stale the way a one-time
 * ViewModel-scoped fetch would after equipping something in Collections and navigating back. */
@Composable
fun CosmeticsHubScreen(
    onBack: () -> Unit,
    onOpenShop: () -> Unit = {},
    onOpenCollections: () -> Unit = {},
    onOpenLuckySpin: () -> Unit = {},
) {
    val particles = rememberParticleFieldState()
    val equippedCosmetics = LocalEquippedCosmetics.current

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.profile_cosmetics_header),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val stickerPackId = equippedCosmetics[CosmeticCategory.STICKER_PACK]
                val trophyId = equippedCosmetics[CosmeticCategory.TROPHY_RELIC]
                if (stickerPackId != null || trophyId != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth().staggeredReveal(1)) {
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().staggeredReveal(2),
                ) {
                    OutlineButton(
                        text = stringResource(R.string.profile_shop_header),
                        onClick = onOpenShop,
                        modifier = Modifier.weight(1f),
                        horizontalPadding = 8.dp,
                    )
                    OutlineButton(
                        text = stringResource(R.string.profile_collections_header),
                        onClick = onOpenCollections,
                        modifier = Modifier.weight(1f),
                        horizontalPadding = 8.dp,
                    )
                    OutlineButton(
                        text = stringResource(R.string.profile_lucky_spin_header),
                        onClick = onOpenLuckySpin,
                        modifier = Modifier.weight(1f),
                        horizontalPadding = 8.dp,
                    )
                }
            }
        }
    }
}
