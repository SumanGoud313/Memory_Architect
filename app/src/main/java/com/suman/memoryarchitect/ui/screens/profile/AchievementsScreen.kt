package com.suman.memoryarchitect.ui.screens.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.domain.achievements.AchievementCatalog
import com.suman.memoryarchitect.feature.profile.AchievementsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/** Which tab is showing - purely UI state, never persisted. [ACHIEVEMENTS] is the default; [UNLOCKS]
 * is the former standalone Rewards screen, folded in here rather than kept as its own
 * Profile button - see [UnlocksBody]. */
private enum class AchievementsHubTab { ACHIEVEMENTS, UNLOCKS }

/** The full achievement catalog (locked + unlocked), plus the reward timeline as an "Unlocks" tab -
 * reached via Profile's single "Achievements" button. Pulled out of the Profile scroll itself so
 * Profile stays a short overview and this becomes its own dedicated destination, same relationship
 * [com.suman.memoryarchitect.ui.screens.statistics.StatisticsScreen] already has to Profile. */
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val particles = rememberParticleFieldState()
    var selectedTab by remember { mutableStateOf(AchievementsHubTab.ACHIEVEMENTS) }
    // The real, currently-rendered banner height (0.dp whenever nothing shows) - see
    // AdaptiveBannerAd's own doc. Without this, the last row of either tab could scroll to a
    // resting position still hidden underneath the banner overlay below.
    var bannerHeight by remember { mutableStateOf(0.dp) }

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.profile_achievements_header),
                    onBack = onBack,
                    modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
                )

                AchievementsHubTabRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).staggeredReveal(1),
                )

                when (selectedTab) {
                    AchievementsHubTab.ACHIEVEMENTS -> AchievementsBody(bannerHeight = bannerHeight)
                    AchievementsHubTab.UNLOCKS -> UnlocksBody(bannerHeight = bannerHeight)
                }
            }

            // Substitutes for the old standalone "Rewards" screen this app used to have (merged
            // into this screen's Unlocks tab - see this file's own doc) as the closest real
            // equivalent to the plan's "Rewards Screen" placement. Overlaid rather than reflowed
            // into the column above (unlike Missions/Settings) since both AchievementsBody/
            // UnlocksBody already own their own full-size scrollable content. See
            // AdaptiveBannerAd's own doc for why this renders nothing at all for a Remove Ads
            // purchaser.
            AdaptiveBannerAd(
                placement = "achievements",
                onHeightChanged = { bannerHeight = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun AchievementsBody(bannerHeight: Dp = 0.dp, viewModel: AchievementsViewModel = hiltViewModel()) {
    val unlockedIds by viewModel.unlockedIds.collectAsStateWithLifecycle()
    val ids = unlockedIds
    if (ids == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = bannerHeight),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AchievementCatalog.definitions.forEachIndexed { index, definition ->
                AchievementRow(
                    id = definition.id,
                    isUnlocked = definition.id in ids,
                    modifier = Modifier.staggeredReveal(index / 4),
                )
            }
        }
    }
}

/** Same lightweight chip-row shape [com.suman.memoryarchitect.ui.screens.shop.CosmeticsHubScreen]'s
 * own tab row uses (this app has no shared `TabRow` primitive - each screen with tabs builds its
 * own small chip row). */
@Composable
private fun AchievementsHubTabRow(selected: AchievementsHubTab, onSelect: (AchievementsHubTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AchievementsHubTabChip(
            text = stringResource(R.string.profile_achievements_header),
            isSelected = selected == AchievementsHubTab.ACHIEVEMENTS,
            onClick = { onSelect(AchievementsHubTab.ACHIEVEMENTS) },
            modifier = Modifier.weight(1f),
        )
        AchievementsHubTabChip(
            text = stringResource(R.string.profile_unlocks_header),
            isSelected = selected == AchievementsHubTab.UNLOCKS,
            onClick = { onSelect(AchievementsHubTab.UNLOCKS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AchievementsHubTabChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
