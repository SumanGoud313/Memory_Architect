package com.suman.memoryarchitect.ui.screens.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.feature.missions.MissionsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Today's/this week's/this month's active missions - reached via a corner button on
 * [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen], same relationship
 * [com.suman.memoryarchitect.ui.screens.profile.AchievementsScreen] has to Profile. The active set
 * is fully deterministic (see [com.suman.memoryarchitect.domain.progression.MissionCatalog]) so
 * every device shows the same three Daily/three Weekly/one Monthly mission on a given day. */
@Composable
fun MissionsScreen(onBack: () -> Unit, viewModel: MissionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.missions_header),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var rowIndex = 1
                    MissionPeriod.entries.forEach { period ->
                        val missionsForPeriod = uiState.missions.filter { it.definition.period == period }
                        if (missionsForPeriod.isEmpty()) return@forEach
                        Text(
                            text = stringResource(period.toSectionTitleRes()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MemoryArchitectColors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp).staggeredReveal(rowIndex),
                        )
                        rowIndex++
                        missionsForPeriod.forEach { mission ->
                            MissionRow(
                                mission = mission,
                                isClaiming = uiState.claimingMissionId == mission.definition.id,
                                onClaim = { viewModel.claim(mission.definition.id) },
                                modifier = Modifier.staggeredReveal(rowIndex),
                            )
                            rowIndex++
                        }
                    }
                }
            }
        }
    }
}

private fun MissionPeriod.toSectionTitleRes(): Int = when (this) {
    MissionPeriod.DAILY -> R.string.missions_section_daily
    MissionPeriod.WEEKLY -> R.string.missions_section_weekly
    MissionPeriod.MONTHLY -> R.string.missions_section_monthly
}
