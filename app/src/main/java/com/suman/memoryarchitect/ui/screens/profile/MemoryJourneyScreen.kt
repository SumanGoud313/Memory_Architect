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
import com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog
import com.suman.memoryarchitect.feature.profile.MemoryJourneyViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The full tier showcase - reached via Profile's "Memory Journey" button. A permanent, never-
 * resetting lifetime track (see [MemoryJourneyCatalog]'s doc) - unlike Missions/Inventory, there's
 * no claim button here, only the bar and the tier list. */
@Composable
fun MemoryJourneyScreen(onBack: () -> Unit, viewModel: MemoryJourneyViewModel = hiltViewModel()) {
    val standing by viewModel.standing.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.memory_journey_header),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            val currentStanding = standing
            if (currentStanding == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MemoryJourneyBar(standing = currentStanding, modifier = Modifier.fillMaxWidth().staggeredReveal(0))
                    MemoryJourneyCatalog.tiers.forEachIndexed { index, tier ->
                        MemoryJourneyTierRow(
                            tier = tier,
                            isUnlocked = currentStanding.totalPoints >= tier.thresholdPoints,
                            modifier = Modifier.staggeredReveal(index + 1),
                        )
                    }
                }
            }
        }
    }
}
