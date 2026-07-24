package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.progression.ShopCatalog
import com.suman.memoryarchitect.feature.shop.LuckySpinUiState
import com.suman.memoryarchitect.feature.shop.LuckySpinViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Cosmetic-only gacha spin - the reward pool is [ShopCatalog.definitions] itself (never gameplay
 * advantages, per the Points Economy plan's fairness requirement). A roll on an already-owned item
 * refunds half its coin price instead of a no-op - always feels like a win. */
@Composable
fun LuckySpinScreen(onBack: () -> Unit, viewModel: LuckySpinViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()
    val context = LocalContext.current

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            ScreenHeader(title = stringResource(R.string.lucky_spin_title), onBack = onBack, modifier = Modifier.staggeredReveal(0))

            when (val state = uiState) {
                is LuckySpinUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
                is LuckySpinUiState.Content -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PillBadge(
                        text = context.getString(R.string.profile_coins, state.coins),
                        icon = Icons.Filled.MonetizationOn,
                        contentColor = MemoryArchitectColors.accentGold,
                        modifier = Modifier.staggeredReveal(1),
                    )

                    Text(
                        text = context.getString(R.string.lucky_spin_cost, state.spinCostCoins),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 12.dp).staggeredReveal(1),
                    )

                    val result = state.lastResult
                    if (result != null) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            tint = MemoryArchitectColors.accentGold.copy(alpha = 0.14f),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CosmeticGlyph(id = result.awardedId, category = ShopCatalog.requireDefinition(result.awardedId).category, isOwned = true, sizeDp = 64.dp)
                                Text(
                                    text = result.awardedId.toDisplayName(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MemoryArchitectColors.textPrimary,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                                Text(
                                    text = if (result.wasDuplicate) {
                                        context.getString(R.string.lucky_spin_duplicate_refund, result.coinsRefunded)
                                    } else {
                                        stringResource(R.string.lucky_spin_new_item)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MemoryArchitectColors.textSecondary,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }

                    if (state.errorReason != null) {
                        Text(
                            text = state.errorReason.toDisplayMessage(context),
                            color = MemoryArchitectColors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }

                    PrimaryButton(
                        text = if (state.isSpinning) "…" else stringResource(R.string.lucky_spin_action),
                        onClick = viewModel::spin,
                        enabled = !state.isSpinning && state.coins >= state.spinCostCoins,
                        modifier = Modifier.padding(top = 28.dp).staggeredReveal(2),
                    )
                }
            }
        }
    }
}
