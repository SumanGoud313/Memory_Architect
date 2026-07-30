package com.suman.memoryarchitect.ui.screens.shop

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.feature.inventory.InventoryActionResult
import com.suman.memoryarchitect.feature.inventory.InventoryViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The permanent home for every earned consumable (Hint/Redo/Rewatch tokens, Lucky Spin tickets,
 * XP boosts, discount coupons, mystery chests) - its own dedicated destination, reached via a
 * corner button on [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen] alongside
 * Missions, same relationship [com.suman.memoryarchitect.ui.screens.missions.MissionsScreen] has. */
@Composable
fun InventoryScreen(onBack: () -> Unit) {
    val particles = rememberParticleFieldState()
    // Reserves exactly the banner's real height above it (see AdaptiveBannerAd's own doc) so the
    // scrollable inventory list's last row never renders invisibly underneath the ad - it used to.
    var bannerHeight by remember { mutableStateOf(0.dp) }
    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.inventory_header),
                    onBack = onBack,
                    modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
                )
                InventoryScreenBody(snackbarBottomPadding = bannerHeight)
            }
        }

        // "Rewards/Inventory" in this app's own naming (see LuckySpinScreen.kt's doc) - see
        // AdaptiveBannerAd's own doc for why this renders nothing at all for a Remove Ads purchaser.
        AdaptiveBannerAd(
            placement = "inventory",
            onHeightChanged = { bannerHeight = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Pure content body, no header/background of its own - reusable if a future phase wants Inventory
 * embedded elsewhere (e.g. a future Shop tab redesign) without duplicating this list.
 * [snackbarBottomPadding] lifts the scrollable list's last row clear of [InventoryScreen]'s own
 * banner ad - `0.dp` by default so a caller with no banner of its own doesn't pad for nothing. The
 * reward Snackbar itself is centered on screen, independent of this padding. */
@Composable
fun InventoryScreenBody(snackbarBottomPadding: Dp = 0.dp, viewModel: InventoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.actionResult.collect { result ->
            val message = when (result) {
                is InventoryActionResult.MysteryChestOpened ->
                    context.getString(R.string.inventory_mystery_chest_opened, result.coinsAwarded)
                is InventoryActionResult.XpBoostApplied ->
                    context.getString(R.string.inventory_xp_boost_applied, result.xpGranted)
                InventoryActionResult.Failed -> context.getString(R.string.inventory_action_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MemoryArchitectColors.bgBase.copy(alpha = 0f),
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = snackbarBottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.inventory.quantities.values.all { it <= 0 }) {
                    Text(
                        text = stringResource(R.string.inventory_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).staggeredReveal(0),
                    )
                }
                InventoryItemKind.entries.forEachIndexed { index, kind ->
                    InventoryRow(
                        kind = kind,
                        quantity = uiState.inventory.quantityOf(kind),
                        modifier = Modifier.staggeredReveal(index),
                        onOpenMysteryChest = viewModel::openMysteryChest,
                        isOpeningChest = uiState.isOpeningChest,
                        onApplyXpBoost = viewModel::applyXpBoost,
                        isApplyingXpBoost = uiState.isApplyingXpBoost,
                    )
                }
            }
        }

        // Centered overlay rather than Scaffold's docked-to-bottom snackbarHost slot: a coin/XP
        // reward toast needs to land where the player is looking, not at the screen edge.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
        )
    }
}
