package com.suman.memoryarchitect.ui.screens.modeselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.feature.modeselect.ModeSelectViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.IconGlassButton
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ModeCardMinHeight = 88.dp
private val CardVerticalSpacing = 14.dp
private val ScreenHorizontalMargin = 24.dp
private val TopExtraPadding = 28.dp
private val BottomExtraPadding = 24.dp

/** Compose replacement for ModeSelectFragment — one card per [GameMode], plus a progress footer
 * so the screen never trails off into empty space. Safe-area aware: top/bottom padding starts
 * from the status/navigation bar insets and adds breathing room on top of them, never instead of
 * them, since the app draws edge-to-edge (targetSdk 36) with no other inset handling to match. */
@Composable
fun ModeSelectScreen(
    onClassicSelected: () -> Unit,
    onOtherModeSelected: (GameMode, DifficultyTier) -> Unit,
    onOpenRemoveAds: () -> Unit = {},
    onOpenCosmetics: () -> Unit = {},
    onOpenLuckySpin: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    viewModel: ModeSelectViewModel = hiltViewModel(),
) {
    val particles = rememberParticleFieldState()
    val scope = rememberCoroutineScope()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val hasRemovedAds by viewModel.hasRemovedAds.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current
    val modes = GameMode.entries.toList()

    // Re-fetches every time this screen is (re-)entered, not just once - see
    // ModeSelectViewModel.refresh()'s doc for why a one-time init fetch isn't enough here.
    LaunchedEffect(Unit) { viewModel.refresh() }

    AmbientBackground(nearParticles = particles) {
        val systemBarPadding = WindowInsets.systemBars.asPaddingValues()

        // Centers the content vertically when it's shorter than the viewport (tall/tablet
        // screens) instead of leaving dead space pinned to the bottom; once content exceeds the
        // viewport it naturally fills it and scrolls, same as a top-anchored list would.
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = ScreenHorizontalMargin + systemBarPadding.calculateStartPadding(layoutDirection),
                        end = ScreenHorizontalMargin + systemBarPadding.calculateEndPadding(layoutDirection),
                        top = systemBarPadding.calculateTopPadding() + TopExtraPadding,
                        bottom = systemBarPadding.calculateBottomPadding() + BottomExtraPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(CardVerticalSpacing),
            ) {
                modes.forEachIndexed { index, mode ->
                    val unlockAtEpochSecond = when (mode) {
                        GameMode.DAILY_CHALLENGE -> progress.dailyChallengeUnlockAtEpochSecond
                        GameMode.WEEKLY_CHALLENGE -> progress.weeklyChallengeUnlockAtEpochSecond
                        GameMode.CLASSIC, GameMode.PRACTICE -> null
                    }
                    ModeCard(
                        mode = mode,
                        unlockAtEpochSecond = unlockAtEpochSecond,
                        onSelected = { selected, accent ->
                            particles.confettiBurst(Offset(400f, 400f), listOf(accent, Color.White))
                            scope.launch {
                                delay(220)
                                if (selected == GameMode.CLASSIC) {
                                    onClassicSelected()
                                } else {
                                    // Practice starts at BEGINNER - the most forgiving tier
                                    // (fewest objects, no rotation, most generous per-object
                                    // pace - see the earlier cognitive-difficulty audit) - so a
                                    // new player's first taste of the game is confidence-building
                                    // rather than a MEDIUM-tier wall. Daily/Weekly ignore this
                                    // value entirely (both now use a fixed object count/timer
                                    // regardless of tier - see DifficultyEngine), so MEDIUM here
                                    // is inert for them, kept only for clarity.
                                    val tier = if (selected == GameMode.PRACTICE) DifficultyTier.BEGINNER else DifficultyTier.MEDIUM
                                    onOtherModeSelected(selected, tier)
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = ModeCardMinHeight).staggeredReveal(index),
                    )
                }
                ModeSelectProgressCard(progress = progress, modifier = Modifier.staggeredReveal(modes.size))
            }

            // One evenly-spaced row (Cosmetics / Lucky Spin / Remove Ads) rather than the old
            // two-corner-pinned layout - Arrangement.SpaceBetween naturally keeps equal spacing
            // whether Remove Ads is showing (3 icons) or already owned and hidden (2 icons), with
            // no extra layout branching needed either way.
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = ScreenHorizontalMargin + systemBarPadding.calculateStartPadding(layoutDirection),
                        end = ScreenHorizontalMargin + systemBarPadding.calculateEndPadding(layoutDirection),
                        top = systemBarPadding.calculateTopPadding() + TopExtraPadding,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconGlassButton(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = stringResource(R.string.missions_header),
                    onClick = onOpenMissions,
                    label = stringResource(R.string.missions_header),
                    modifier = Modifier.staggeredReveal(0),
                )

                IconGlassButton(
                    icon = Icons.Filled.Inventory2,
                    contentDescription = stringResource(R.string.inventory_header),
                    onClick = onOpenInventory,
                    label = stringResource(R.string.inventory_header),
                    modifier = Modifier.staggeredReveal(0),
                )

                IconGlassButton(
                    icon = Icons.Filled.Palette,
                    contentDescription = stringResource(R.string.profile_cosmetics_header),
                    onClick = onOpenCosmetics,
                    label = stringResource(R.string.profile_cosmetics_header),
                    modifier = Modifier.staggeredReveal(0),
                )

                IconGlassButton(
                    icon = Icons.Filled.Casino,
                    contentDescription = stringResource(R.string.lucky_spin_title),
                    onClick = onOpenLuckySpin,
                    label = stringResource(R.string.lucky_spin_title),
                    modifier = Modifier.staggeredReveal(0),
                )

                if (!hasRemovedAds) {
                    IconGlassButton(
                        icon = Icons.Filled.Block,
                        contentDescription = stringResource(R.string.settings_remove_ads),
                        onClick = onOpenRemoveAds,
                        label = stringResource(R.string.settings_remove_ads),
                        modifier = Modifier.staggeredReveal(0),
                    )
                }
            }
        }
    }
}
