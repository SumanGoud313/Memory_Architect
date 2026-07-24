package com.suman.memoryarchitect.ui.screens.levelselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.feature.levelselect.LevelSelectUiState
import com.suman.memoryarchitect.feature.levelselect.LevelSelectViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.LevelProgressBar
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private const val TOTAL_LEVELS = 100

/** A staggered entrance capped so cells composed far down the grid (scrolled into view fresh)
 * never wait almost 5 seconds for their turn — see [LevelSelectScreen]. */
private const val MAX_STAGGER_INDEX = 11

/** Compose replacement for LevelSelectFragment — a 3-column grid of the 100 Classic levels. */
@Composable
fun LevelSelectScreen(
    onLevelSelected: (Int) -> Unit,
    viewModel: LevelSelectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    // Mirrors the old Fragment's onResume() reload — progress must reflect a just-completed level.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    AmbientBackground(nearParticles = particles) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    text = stringResource(R.string.level_select_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MemoryArchitectColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.level_select_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val state = uiState
                if (state is LevelSelectUiState.Loaded) {
                    Text(
                        text = stringResource(R.string.level_select_progress, state.maxUnlockedLevel, TOTAL_LEVELS),
                        style = MaterialTheme.typography.labelLarge,
                        color = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    LevelProgressBar(
                        fraction = state.maxUnlockedLevel.toFloat() / TOTAL_LEVELS.toFloat(),
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            val state = uiState
            if (state is LevelSelectUiState.Loaded) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(TOTAL_LEVELS) { index ->
                        val levelNumber = index + 1
                        LevelCell(
                            levelNumber = levelNumber,
                            unlocked = levelNumber <= state.maxUnlockedLevel,
                            isFrontier = levelNumber == state.maxUnlockedLevel,
                            bestTimeMs = state.bestTimes[levelNumber],
                            stars = state.bestStars[levelNumber],
                            onClick = onLevelSelected,
                            modifier = Modifier.staggeredReveal(index.coerceAtMost(MAX_STAGGER_INDEX)),
                        )
                    }
                }
            }
        }
    }
}
