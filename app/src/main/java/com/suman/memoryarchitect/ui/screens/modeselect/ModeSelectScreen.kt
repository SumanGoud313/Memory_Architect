package com.suman.memoryarchitect.ui.screens.modeselect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.feature.modeselect.ModeSelectViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.IconGlassButton
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ModeCardMinHeight = 88.dp
private val CardVerticalSpacing = 14.dp
private val ScreenHorizontalMargin = 24.dp
private val TopExtraPadding = 28.dp
private val BottomExtraPadding = 24.dp

// Extra breathing room above Missions/Inventory, on top of the CardVerticalSpacing every other
// gap in this list already gets - visually separates "pick a mode" from "everything else" rather
// than reading as just one more item in the same list.
private val FooterButtonsExtraTopSpacing = 20.dp

/** Compose replacement for ModeSelectFragment — one card per [GameMode], plus a Missions/Inventory
 * row anchored under the last card. Safe-area aware: top/bottom padding starts from the status/
 * navigation bar insets and adds breathing room on top of them, never instead of them, since the
 * app draws edge-to-edge (targetSdk 36) with no other inset handling to match. */
@Composable
fun ModeSelectScreen(
    onClassicSelected: () -> Unit,
    onOtherModeSelected: (GameMode, DifficultyTier) -> Unit,
    onOpenRemoveAds: () -> Unit = {},
    onOpenCosmetics: () -> Unit = {},
    onOpenLuckySpin: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    viewModel: ModeSelectViewModel = hiltViewModel(),
) {
    val particles = rememberParticleFieldState()
    val scope = rememberCoroutineScope()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val hasRemovedAds by viewModel.hasRemovedAds.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current
    // The real, currently-rendered banner height (0.dp whenever nothing shows) - see
    // AdaptiveBannerAd's own doc. BottomExtraPadding alone (a fixed 24dp) isn't enough clearance
    // for the ~50-90dp an adaptive banner actually renders at, so on a shorter viewport the last
    // mode card could scroll to a resting position still partly hidden underneath it.
    var bannerHeight by remember { mutableStateOf(0.dp) }
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
                        bottom = systemBarPadding.calculateBottomPadding() + BottomExtraPadding + bannerHeight,
                    ),
                verticalArrangement = Arrangement.spacedBy(CardVerticalSpacing),
            ) {
                // Lives in the column, right-aligned directly above Classic (the first mode
                // card), instead of a fixed top-offset overlay - anchoring it to the actual
                // column meant it drifted into Classic whenever this centered column shifted
                // (e.g. banner ad height changing the column's total measured height), since a
                // fixed distance from the screen top has no idea where Classic actually ended up.
                Row(modifier = Modifier.fillMaxWidth().staggeredReveal(0), horizontalArrangement = Arrangement.End) {
                    LeaderboardMiniButton(onClick = onOpenLeaderboard)
                }
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
                // Missions/Inventory live here, directly under the last mode card (Weekly
                // Challenge), rather than in the floating top-row icon strip with Cosmetics/Lucky
                // Spin/Remove Ads - both open a full progression list, not a quick shortcut, so
                // they read better anchored to the mode list itself.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = FooterButtonsExtraTopSpacing).staggeredReveal(modes.size),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconGlassButton(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = stringResource(R.string.missions_header),
                        onClick = onOpenMissions,
                        label = stringResource(R.string.missions_header),
                    )
                    // Labeled "Rewards" here per this session's request, even though it still opens
                    // the same Inventory screen (consumables: Hint/Redo/Rewatch tokens, Lucky Spin
                    // Tickets, Mystery Chests) - only the Mode Select label changed, not the
                    // destination or its own header.
                    IconGlassButton(
                        icon = Icons.Filled.Inventory2,
                        contentDescription = stringResource(R.string.profile_rewards_header),
                        onClick = onOpenInventory,
                        label = stringResource(R.string.profile_rewards_header),
                    )
                }
            }

            // One evenly-spaced row (Cosmetics / Lucky Spin / Remove Ads) rather than the old
            // two-corner-pinned layout - Arrangement.SpaceBetween naturally keeps equal spacing
            // whether Remove Ads is showing (3 icons) or already owned and hidden (2 icons), with
            // no extra layout branching needed either way. Missions/Inventory used to live here
            // too - see the Row placed directly under the mode card list below for where they
            // moved and why.
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
                    icon = Icons.Filled.ShoppingCart,
                    contentDescription = stringResource(R.string.profile_shop_header),
                    onClick = onOpenCosmetics,
                    label = stringResource(R.string.profile_shop_header),
                    modifier = Modifier.staggeredReveal(0),
                )

                IconGlassButton(
                    icon = Icons.Filled.Attractions,
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

            // Bottom-anchored, never overlapping the scrollable mode-card column above (which
            // already reserves its own bottom padding) - a non-gameplay screen, exactly the
            // placement this ad format is meant for. Renders nothing at all for a Remove Ads
            // purchaser or if Remote Config has banners off - see AdaptiveBannerAd's own doc.
            AdaptiveBannerAd(
                placement = "mode_select",
                onHeightChanged = { bannerHeight = it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = systemBarPadding.calculateBottomPadding()),
            )
        }
    }
}

@Composable
private fun LeaderboardMiniButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val tick = rememberHapticsTick()
    Box(
        modifier = modifier
            .background(MemoryArchitectColors.glassFill, RoundedCornerShape(6.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                tick()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_view_leaderboards),
            style = MaterialTheme.typography.labelSmall,
            color = MemoryArchitectColors.textPrimary,
            maxLines = 1,
        )
    }
}
