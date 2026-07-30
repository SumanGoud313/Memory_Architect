package com.suman.memoryarchitect.ui.screens.shop

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.core.common.toIcon
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import com.suman.memoryarchitect.domain.progression.MysteryChestAdRules
import com.suman.memoryarchitect.domain.progression.ShopCatalog
import com.suman.memoryarchitect.domain.repository.SpinSource
import com.suman.memoryarchitect.feature.shop.LuckySpinUiState
import com.suman.memoryarchitect.feature.shop.LuckySpinViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.AnimatedCounter
import com.suman.memoryarchitect.ui.components.ConfettiBurst
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Cosmetic-and-coins gacha spin - reached via its own icon on
 * [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen]. One free spin per day, one
 * more via a rewarded ad, and any number more via owned Lucky Spin Tickets - see
 * [LuckySpinViewModel]'s doc. The reward table is mostly coins (150/250, a rarer 500 "jackpot"),
 * with a genuine [ShopCatalog] cosmetic as the minority outcome - guaranteed on this player's very
 * first-ever spin. [onGoToCollections] navigates to the Shop/Collections hub so a won cosmetic can
 * actually be equipped from here. */
@Composable
fun LuckySpinScreen(
    onBack: () -> Unit,
    onGoToCollections: () -> Unit,
    onGoToInventory: () -> Unit,
    viewModel: LuckySpinViewModel = hiltViewModel(),
) {
    val particles = rememberParticleFieldState()
    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.lucky_spin_title),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            LuckySpinScreenBody(onGoToCollections = onGoToCollections, onGoToInventory = onGoToInventory, viewModel = viewModel)
        }
    }
}

@Composable
fun LuckySpinScreenBody(
    onGoToCollections: () -> Unit = {},
    onGoToInventory: () -> Unit = {},
    viewModel: LuckySpinViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardedAdState by viewModel.rewardedAdState.collectAsStateWithLifecycle()
    val mysteryChestRewardedAdState by viewModel.mysteryChestRewardedAdState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val feedback = rememberFeedback()
    val celebration = rememberParticleFieldState(ambientCount = 0)
    var celebrationWidth by remember { mutableStateOf(0f) }
    var revealed by remember {
        val content = uiState as? LuckySpinUiState.Content
        mutableStateOf(content != null && content.lastResult != null && !content.isSpinning)
    }

    LaunchedEffect(uiState) {
        val content = uiState as? LuckySpinUiState.Content ?: return@LaunchedEffect
        if (content.isSpinning) {
            revealed = false
            feedback.onLuckySpinStarted()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
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

                    if (state.isFirstSpinEver && !state.isSpinning && state.lastResult == null) {
                        Text(
                            text = stringResource(R.string.lucky_spin_first_spin_badge),
                            style = MaterialTheme.typography.labelLarge,
                            color = MemoryArchitectColors.accentSage,
                            modifier = Modifier.padding(top = 8.dp).staggeredReveal(1),
                        )
                    }

                    LuckySpinWheel(
                        isSpinning = state.isSpinning,
                        targetReward = state.lastResult?.reward,
                        modifier = Modifier.fillMaxWidth(0.7f).padding(top = 20.dp).staggeredReveal(2),
                        onSettled = {
                            revealed = true
                            feedback.onLuckySpinRevealed()
                            celebration.celebrationRain(width = celebrationWidth.takeIf { it > 0f } ?: 1000f)
                        },
                    )

                    if (revealed && state.lastResult != null) {
                        SpinRewardCard(
                            reward = state.lastResult.reward,
                            wasDuplicate = state.lastResult.wasDuplicate,
                            coinsRefunded = state.lastResult.coinsRefunded,
                            wasFirstSpin = state.wasFirstSpin,
                            onGoToCollections = onGoToCollections,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        )
                    }

                    if (state.errorReason != null) {
                        Text(
                            text = state.errorReason.toDisplayMessage(context),
                            color = MemoryArchitectColors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    if (rewardedAdState is RewardedAdUiState.Failed || mysteryChestRewardedAdState is RewardedAdUiState.Failed) {
                        Text(
                            text = stringResource(R.string.lucky_spin_ad_unavailable),
                            color = MemoryArchitectColors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    SpinActionButton(
                        state = state,
                        isAdLoading = rewardedAdState is RewardedAdUiState.Loading,
                        onSpinFree = { viewModel.spin(SpinSource.FREE) },
                        onWatchAd = { activity?.let(viewModel::watchRewardedAd) },
                        onSpinTicket = { viewModel.spin(SpinSource.TICKET) },
                        modifier = Modifier.padding(top = 28.dp).staggeredReveal(3),
                    )
                }
            }
        }

        ConfettiBurst(
            state = celebration,
            modifier = Modifier.fillMaxSize().onGloballyPositioned { celebrationWidth = it.size.width.toFloat() },
        )

        // Hidden during the spin animation/celebration itself (a genuine "concentration/reward
        // moment" this app's own ad-placement rules already treat like a gameplay screen) - only
        // shown once the wheel is idle, waiting for the next spin. See AdaptiveBannerAd's own doc
        // for why this renders nothing at all for a Remove Ads purchaser regardless.
        val content = uiState as? LuckySpinUiState.Content
        if (content != null && !content.isSpinning) {
            AdaptiveBannerAd(placement = "lucky_spin", modifier = Modifier.align(Alignment.BottomCenter))
            MysteryChestAdTile(
                state = content,
                isAdLoading = mysteryChestRewardedAdState is RewardedAdUiState.Loading,
                onWatchAd = { activity?.let(viewModel::watchRewardedAdForMysteryChest) },
                onAvailableClick = { viewModel.onMysteryChestAvailableClicked(); onGoToInventory() },
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp),
            )
        }
    }
}

/** Top-right corner callout - clear of both the centered wheel/button column below it and the
 * [ScreenHeader] above it (a sibling in the outer `Column`, not this `Box`, so this `Box`'s own
 * [Alignment.TopEnd] already starts below the header with no extra offset needed) - a small,
 * self-contained ad-gated grant entirely separate from the spin wheel (see
 * [com.suman.memoryarchitect.domain.model.MysteryChestAdState]'s doc for why it's tracked as its
 * own daily allowance). The `X/[MysteryChestAdRules.maxClaimsPerDay]` claimed-today count is
 * always shown, not just once exhausted - every player should be able to see the daily limit up
 * front, not discover it only after hitting it. Watch-ad-only by design: unlike
 * [SpinActionButton], there is deliberately no free/ticket alternative offered here at all, even
 * once [LuckySpinUiState.Content.mysteryChestClaimsRemaining] hits 0 for the day - the tile just
 * goes quiet (no button) until the next day resets it. Once a claim succeeds,
 * [LuckySpinUiState.Content.mysteryChestJustClaimed] flips this to a "you got one" callout with an
 * "Avail Now" button instead, so the player has an explicit next step (open it from the Rewards/
 * Inventory screen) rather than the chest silently landing in their balance with no callout at
 * all. */
@Composable
private fun MysteryChestAdTile(
    state: LuckySpinUiState.Content,
    isAdLoading: Boolean,
    onWatchAd: () -> Unit,
    onAvailableClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val claimsUsedToday = MysteryChestAdRules.Default.maxClaimsPerDay - state.mysteryChestClaimsRemaining
    GlassCard(modifier = modifier.widthIn(max = 160.dp), tint = MemoryArchitectColors.accentGold.copy(alpha = 0.14f)) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = InventoryItemKind.MYSTERY_CHEST.toIcon(),
                contentDescription = null,
                tint = MemoryArchitectColors.accentGold,
                modifier = Modifier.size(18.dp).padding(bottom = 2.dp),
            )
            Text(
                text = stringResource(R.string.mystery_chest_box_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MemoryArchitectColors.accentGold,
                textAlign = TextAlign.Center,
            )
            if (state.mysteryChestJustClaimed) {
                Text(
                    text = stringResource(R.string.mystery_chest_won_message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MemoryArchitectColors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
                OutlineButton(
                    text = stringResource(R.string.mystery_chest_available_action),
                    onClick = onAvailableClick,
                    horizontalPadding = 12.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.mystery_chest_watch_ad_description),
                    style = MaterialTheme.typography.labelSmall,
                    color = MemoryArchitectColors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = context.getString(R.string.missions_progress_format, claimsUsedToday, MysteryChestAdRules.Default.maxClaimsPerDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
                if (state.mysteryChestClaimsRemaining > 0) {
                    OutlineButton(
                        text = if (isAdLoading) "…" else stringResource(R.string.mystery_chest_watch_ad_action),
                        onClick = onWatchAd,
                        horizontalPadding = 12.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpinActionButton(
    state: LuckySpinUiState.Content,
    isAdLoading: Boolean,
    onSpinFree: () -> Unit,
    onWatchAd: () -> Unit,
    onSpinTicket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        state.isSpinning -> "…"
        state.canSpinFree -> stringResource(R.string.lucky_spin_free_action)
        state.canSpinAd -> stringResource(R.string.lucky_spin_watch_ad_action)
        state.ticketCount > 0 -> stringResource(R.string.lucky_spin_use_ticket_action, state.ticketCount)
        else -> stringResource(R.string.lucky_spin_come_back_tomorrow)
    }
    PrimaryButton(
        text = label,
        onClick = {
            when {
                state.canSpinFree -> onSpinFree()
                state.canSpinAd -> onWatchAd()
                state.ticketCount > 0 -> onSpinTicket()
            }
        },
        enabled = !state.isSpinning && !isAdLoading && state.canSpin,
        modifier = modifier,
    )
}

@Composable
private fun SpinRewardCard(
    reward: SpinRewardKind,
    wasDuplicate: Boolean,
    coinsRefunded: Long,
    wasFirstSpin: Boolean,
    onGoToCollections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    GlassCard(modifier = modifier, tint = MemoryArchitectColors.accentGold.copy(alpha = 0.14f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (reward) {
                is SpinRewardKind.Coins -> {
                    AnimatedCounter(
                        target = reward.amount.toInt(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MemoryArchitectColors.accentGold,
                        formatter = { context.getString(R.string.lucky_spin_coins_won, it) },
                    )
                }
                is SpinRewardKind.Cosmetic -> {
                    CosmeticGlyph(id = reward.id, category = ShopCatalog.requireDefinition(reward.id).category, isOwned = true, sizeDp = 64.dp)
                    Text(
                        text = reward.id.toDisplayName(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MemoryArchitectColors.textPrimary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (wasFirstSpin) {
                        Text(
                            text = stringResource(R.string.lucky_spin_first_spin_badge),
                            style = MaterialTheme.typography.labelLarge,
                            color = MemoryArchitectColors.accentSage,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (wasDuplicate) {
                        // Already owned - nothing new to go equip, so the refund message replaces
                        // the "go to Collections" nudge entirely rather than showing both.
                        Text(
                            text = context.getString(R.string.lucky_spin_duplicate_refund, coinsRefunded),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MemoryArchitectColors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.lucky_spin_go_to_collections),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MemoryArchitectColors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        OutlineButton(
                            text = stringResource(R.string.lucky_spin_go_to_shop_action),
                            onClick = onGoToCollections,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Compose's [LocalContext] is often a [ContextWrapper] around the hosting Activity rather than
 * the Activity itself, so a plain `as? Activity` cast can fail even inside a normal Activity-
 * hosted screen - this unwraps until it finds one, or gives up (rather than crash) if there
 * somehow isn't one. Same small private helper [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScreen]/
 * [com.suman.memoryarchitect.ui.ConnectivityGate] each already keep their own copy of, rather than
 * a shared utility - this file follows that same existing convention. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
