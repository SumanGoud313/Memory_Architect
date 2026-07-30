package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.domain.progression.RewardCatalog
import com.suman.memoryarchitect.feature.profile.RewardsViewModel
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The full reward timeline (locked + unlocked) - the "Unlocks" tab inside
 * [AchievementsScreen], reached via Profile's "Achievements" button. A pure tab body (no header/
 * background of its own), same shape as [com.suman.memoryarchitect.ui.screens.shop.ShopScreenBody]/
 * [com.suman.memoryarchitect.ui.screens.shop.CollectionsScreenBody] - keeps its own
 * [hiltViewModel] so switching tabs and back never refetches. */
@Composable
fun UnlocksBody(bannerHeight: Dp = 0.dp, viewModel: RewardsViewModel = hiltViewModel()) {
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
            RewardCatalog.timeline.forEachIndexed { index, definition ->
                RewardRow(
                    id = definition.id,
                    kind = definition.kind,
                    isUnlocked = definition.id in ids,
                    unlockLevel = definition.unlockLevel,
                    modifier = Modifier.staggeredReveal(index / 4),
                )
            }
        }
    }
}
