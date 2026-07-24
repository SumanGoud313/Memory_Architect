package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.progression.RewardCatalog
import com.suman.memoryarchitect.feature.profile.RewardsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The full reward timeline (locked + unlocked) - reached via Profile's "Rewards" button. Pulled
 * out of the Profile scroll itself for the same reason as [AchievementsScreen]. */
@Composable
fun RewardsScreen(onBack: () -> Unit, viewModel: RewardsViewModel = hiltViewModel()) {
    val unlockedIds by viewModel.unlockedIds.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.profile_rewards_header),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            val ids = unlockedIds
            if (ids == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
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
    }
}
