package com.suman.memoryarchitect.ui.screens.shop

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.core.billing.toDisplayMessage
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.model.BillingEntitlementKind
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.BillingCatalogProduct
import com.suman.memoryarchitect.domain.progression.PremiumShopCatalog
import com.suman.memoryarchitect.feature.shop.ShopTab
import com.suman.memoryarchitect.feature.shop.ShopUiState
import com.suman.memoryarchitect.feature.shop.ShopViewModel
import com.suman.memoryarchitect.ui.components.ConfettiBurst
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/** The Shop tab body - reached via the Cosmetics Hub's 🪙 Coin Shop / 💎 Premium Shop tabs (Shop
 * is the hub's default tab - see `CosmeticsHubScreen.kt`). Two completely separate storefronts
 * under one screen (the redesign's core requirement): the 🪙 Coin Shop (unchanged below - coins
 * only ever buy cosmetics, never score/stars/leaderboard rank/hints/redos/timer, preserving the
 * existing skill-based leaderboard and gameplay untouched by construction) and the 💎 Premium Shop
 * (real money only, via Google Play Billing, for [PremiumShopCatalog.products]'s cosmetic bundles -
 * `remove_ads_lifetime` is deliberately never rendered here, see the 2 `.filter` call sites below;
 * it already has its own entry point on [com.suman.memoryarchitect.ui.screens.modeselect.ModeSelectScreen],
 * duplicating it here would be redundant). The two tabs never share a card layout or a buy action,
 * so a coin item and a real-money bundle can never be mistaken for one another.
 *
 * No `AmbientBackground`/`ScreenHeader` of its own - the Cosmetics Hub screen owns both, shared
 * across all 3 of its tabs; this composable keeps its own [hiltViewModel] so switching tabs and
 * back never refetches. */
@Composable
fun ShopScreenBody(viewModel: ShopViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var previewDefinition by remember { mutableStateOf<CosmeticDefinition?>(null) }
    var previewProduct by remember { mutableStateOf<BillingCatalogProduct?>(null) }
    // Sticker Pack + Trophy & Relic + Profile Badge render together under one heading (see
    // ShopSection below) - the cosmetic audit found these three categories functionally redundant
    // with each other (all are showcase-only glyphs with no other function); grouping them stops
    // presenting that as separate, confusingly-named categories without touching the underlying
    // enum/catalog/owned data at all.
    val showcaseHeader = stringResource(R.string.profile_showcase_header)
    val particles = rememberParticleFieldState(ambientCount = 0)

    fun launchPremiumPurchase(product: BillingCatalogProduct) {
        val activity = context as? Activity ?: return
        viewModel.buyProduct(activity, product.billingProductId)
    }

    // The purchase confirm button doesn't wait for the real async result before dismissing (it
    // closes its dialog optimistically), so this is the only visible moment a successful real-money
    // purchase gets - same "reward moment = confetti" language every other grant in this app already
    // uses (rewarded ads, level unlocks, gameplay object placement).
    val content = uiState as? ShopUiState.Content
    LaunchedEffect(content?.purchaseState) {
        if (content?.purchaseState is PurchaseUiState.Success) {
            particles.confettiBurst(Offset(400f, 300f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiBurst(state = particles, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).staggeredReveal(0),
                horizontalArrangement = Arrangement.End,
            ) {
                val coins = (uiState as? ShopUiState.Content)?.coins
                if (coins != null) {
                    PillBadge(
                        text = context.getString(R.string.profile_coins, coins),
                        icon = Icons.Filled.MonetizationOn,
                        contentColor = MemoryArchitectColors.accentGold,
                    )
                }
            }

            when (val state = uiState) {
                is ShopUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
                is ShopUiState.Content -> {
                    // Debug-build testing convenience only - stripped from release builds by this
                    // BuildConfig.DEBUG check, same gate SettingsScreen already uses for its
                    // AnalyticsDashboard entry point. See DebugTestGrantor's doc. Horizontally
                    // scrollable since it now carries one button per Premium Shop cosmetic bundle
                    // too, not just the original 3 coin actions.
                    if (BuildConfig.DEBUG) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlineButton(text = "DEBUG: +5000 coins", onClick = viewModel::debugGrantCoins, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +1000 XP", onClick = viewModel::debugGrantXp, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Unlock all", onClick = viewModel::debugUnlockAll, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Lock all", onClick = viewModel::debugLockAll, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Unlock all 100 levels", onClick = viewModel::debugUnlockAllLevels, horizontalPadding = 10.dp)
                            PremiumShopCatalog.cosmeticCollectionProductIds.forEach { productId ->
                                OutlineButton(
                                    text = "DEBUG: Grant ${PremiumShopCatalog.requireProduct(productId).let { stringResource(it.titleRes) }}",
                                    onClick = { viewModel.debugGrantPremiumProduct(productId) },
                                    horizontalPadding = 10.dp,
                                )
                            }
                            OutlineButton(text = "DEBUG: +5 Hint Tokens", onClick = { viewModel.debugGrantInventoryItem(InventoryItemKind.HINT_TOKEN) }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +5 Redo Tokens", onClick = { viewModel.debugGrantInventoryItem(InventoryItemKind.REDO_TOKEN) }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +5 Rewatch Tickets", onClick = { viewModel.debugGrantInventoryItem(InventoryItemKind.REWATCH_TICKET) }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +5 Lucky Spin Tickets", onClick = { viewModel.debugGrantInventoryItem(InventoryItemKind.LUCKY_SPIN_TICKET) }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +5 Mystery Chests", onClick = { viewModel.debugGrantInventoryItem(InventoryItemKind.MYSTERY_CHEST) }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +500 Journey Points", onClick = viewModel::debugGrantJourneyPoints, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: +1 Streak Shield", onClick = viewModel::debugGrantStreakShield, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Reset Daily Reward", onClick = viewModel::debugResetDailyReward, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Reset Returning Player", onClick = viewModel::debugResetReturningPlayer, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Trigger notification", onClick = viewModel::debugTriggerNotification, horizontalPadding = 10.dp)
                            // DebugLiveEventOverride now bypasses each template's own date window
                            // entirely (see its own doc), so any of the 9 templates previews
                            // correctly regardless of today's date - no longer limited to whichever
                            // one's default window happens to cover "now".
                            OutlineButton(text = "DEBUG: Trigger HALLOWEEN event", onClick = { viewModel.debugTriggerSeasonalEvent("HALLOWEEN") }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Trigger SUMMER event", onClick = { viewModel.debugTriggerSeasonalEvent("SUMMER") }, horizontalPadding = 10.dp)
                            OutlineButton(text = "DEBUG: Clear event override", onClick = viewModel::debugClearSeasonalEventOverride, horizontalPadding = 10.dp)
                        }
                    }
                    if (state.errorReason != null) {
                        Text(
                            text = state.errorReason.toDisplayMessage(context),
                            color = MemoryArchitectColors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }
                    if (state.debugMessage != null) {
                        Text(
                            text = state.debugMessage,
                            color = MemoryArchitectColors.accentTerracotta,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }
                    val purchaseFailure = state.purchaseState as? PurchaseUiState.Failed
                    if (purchaseFailure != null) {
                        Text(
                            text = purchaseFailure.reason.toDisplayMessage(context),
                            color = MemoryArchitectColors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }

                    ShopTabRow(
                        selected = state.selectedTab,
                        onSelect = viewModel::selectTab,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).staggeredReveal(2),
                    )

                    when (state.selectedTab) {
                        ShopTab.COIN -> Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            val byCategory = state.catalog.groupBy { it.category }
                            val sections = buildShopSections(byCategory, showcaseHeader)
                            sections.forEachIndexed { sectionIndex, section ->
                                Column(modifier = Modifier.staggeredReveal(sectionIndex), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = section.header,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MemoryArchitectColors.textPrimary,
                                    )
                                    CosmeticRarity.entries.forEach { rarity ->
                                        val rarityItems = section.items.filter { it.rarity == rarity }
                                        if (rarityItems.isEmpty()) return@forEach
                                        Text(
                                            text = rarity.name.lowercase().replaceFirstChar(Char::uppercase),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MemoryArchitectColors.textTertiary,
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                        rarityItems.forEach { definition ->
                                            ShopItemRow(
                                                definition = definition,
                                                isOwned = definition.id in state.ownedIds,
                                                isPurchasing = state.purchasingId == definition.id,
                                                canAfford = state.coins >= definition.priceCoins,
                                                onBuy = { viewModel.purchase(definition.id) },
                                                onPreview = { previewDefinition = definition },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        ShopTab.PREMIUM -> Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            PremiumShopCatalog.products
                                .filter { it.entitlement == BillingEntitlementKind.COSMETIC_COLLECTION }
                                .forEachIndexed { index, product ->
                                val billingState = state.billingProductStates[product.billingProductId]
                                val isLoading = (state.purchaseState as? PurchaseUiState.Loading)?.productId == product.billingProductId
                                PremiumProductCard(
                                    product = product,
                                    formattedPrice = billingState?.formattedPrice,
                                    priceLoadFailed = state.billingProductsLoadFailed,
                                    isOwned = billingState?.owned == true,
                                    isLoading = isLoading,
                                    onBuy = { launchPremiumPurchase(product) },
                                    onOpenDetail = {
                                        previewProduct = product
                                        viewModel.onProductViewed(product.billingProductId)
                                    },
                                    onRetryPriceLoad = viewModel::retryBillingPriceLoad,
                                    modifier = Modifier.staggeredReveal(index),
                                )
                            }
                            OutlineButton(
                                text = stringResource(R.string.remove_ads_restore_purchase),
                                onClick = viewModel::restorePurchases,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
                            )
                        }
                    }
                }
            }
        }

        // Bottom-anchored over both the Coin and Premium sub-tabs alike - see AdaptiveBannerAd's
        // own doc for why this renders nothing at all for a Remove Ads purchaser.
        AdaptiveBannerAd(placement = "shop", modifier = Modifier.align(Alignment.BottomCenter))

        val preview = previewDefinition
        if (preview != null) {
            val isOwned = (uiState as? ShopUiState.Content)?.ownedIds?.contains(preview.id) == true
            CosmeticPreviewDialog(
                definition = preview,
                isOwned = isOwned,
                // Shop is buy-focused - equip/unequip live in Collections (see the plan's scope
                // decision), so an owned item's preview here just shows it, no equip action.
                isEquipped = false,
                onBuy = { viewModel.purchase(preview.id) },
                onDismiss = { previewDefinition = null },
            )
        }

        val product = previewProduct
        if (product != null) {
            val content = uiState as? ShopUiState.Content
            val billingState = content?.billingProductStates?.get(product.billingProductId)
            val isLoading = (content?.purchaseState as? PurchaseUiState.Loading)?.productId == product.billingProductId
            PremiumProductDetailDialog(
                product = product,
                formattedPrice = billingState?.formattedPrice,
                priceLoadFailed = content?.billingProductsLoadFailed == true,
                isOwned = billingState?.owned == true,
                isLoading = isLoading,
                onBuy = { launchPremiumPurchase(product) },
                onDismiss = { previewProduct = null },
                onRetryPriceLoad = viewModel::retryBillingPriceLoad,
            )
        }
    }
}


/** 🪙 Coin Shop / 💎 Premium Shop - the redesign's "two completely separate sections" made visible
 * as two tabs, same lightweight chip-row shape [com.suman.memoryarchitect.ui.screens.leaderboard.LeaderboardScreen]'s
 * own tab row already uses (this app has no shared `TabRow` primitive - each screen with tabs
 * builds its own small chip row, so this follows that existing convention rather than introducing
 * a new shared component for a two-item case). */
@Composable
private fun ShopTabRow(selected: ShopTab, onSelect: (ShopTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShopTabChip(
            text = stringResource(R.string.shop_tab_coin),
            isSelected = selected == ShopTab.COIN,
            onClick = { onSelect(ShopTab.COIN) },
            modifier = Modifier.weight(1f),
        )
        ShopTabChip(
            text = stringResource(R.string.shop_tab_premium),
            isSelected = selected == ShopTab.PREMIUM,
            onClick = { onSelect(ShopTab.PREMIUM) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShopTabChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(MemoryArchitectRadii.chip)
    Box(
        modifier = modifier
            .background(
                if (isSelected) MemoryArchitectColors.accentGold.copy(alpha = 0.18f) else MemoryArchitectColors.glassFill,
                shape,
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (isSelected) MemoryArchitectColors.accentGold else MemoryArchitectColors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private class ShopSection(val header: String, val items: List<CosmeticDefinition>)

/** Every [CosmeticCategory] gets its own section, in declared enum order, except
 * [CosmeticCategory.STICKER_PACK], [CosmeticCategory.TROPHY_RELIC], and [CosmeticCategory.PROFILE_BADGE]
 * which combine into one [showcaseHeader]-titled section (see the doc on the [showcaseHeader] call site). */
private fun buildShopSections(byCategory: Map<CosmeticCategory, List<CosmeticDefinition>>, showcaseHeader: String): List<ShopSection> {
    val showcaseCategories = setOf(CosmeticCategory.STICKER_PACK, CosmeticCategory.TROPHY_RELIC, CosmeticCategory.PROFILE_BADGE)
    return buildList {
        CosmeticCategory.entries.forEach { category ->
            if (category in showcaseCategories && category != CosmeticCategory.STICKER_PACK) return@forEach
            if (category == CosmeticCategory.STICKER_PACK) {
                val combined = showcaseCategories.flatMap { byCategory[it].orEmpty() }
                if (combined.isNotEmpty()) add(ShopSection(showcaseHeader, combined))
            } else {
                val items = byCategory[category].orEmpty()
                if (items.isNotEmpty()) add(ShopSection(category.toDisplayName(), items))
            }
        }
    }
}

@Composable
private fun ShopItemRow(
    definition: CosmeticDefinition,
    isOwned: Boolean,
    isPurchasing: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onPreview: () -> Unit,
) {
    val context = LocalContext.current
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = if (isOwned) MemoryArchitectColors.accentGold.copy(alpha = 0.1f) else null) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Tapping the glyph itself still opens Preview (unchanged), plus a small explicit eye
            // affordance in the corner - the audit flagged glyph-tap alone as an undiscoverable
            // gesture with no visual cue that it's tappable.
            Box {
                CosmeticGlyph(
                    id = definition.id,
                    category = definition.category,
                    isOwned = true,
                    modifier = Modifier.clickable(onClick = onPreview),
                )
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = stringResource(R.string.cosmetic_preview_action),
                    tint = MemoryArchitectColors.textPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(MemoryArchitectColors.glassFill, CircleShape)
                        .clickable(onClick = onPreview)
                        .padding(3.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(text = definition.id.toDisplayName(), style = MaterialTheme.typography.bodyLarge, color = MemoryArchitectColors.textPrimary)
                Text(
                    text = definition.rarity.name.lowercase().replaceFirstChar(Char::uppercase),
                    style = MaterialTheme.typography.bodySmall,
                    color = MemoryArchitectColors.textTertiary,
                )
            }
            if (isOwned) {
                Text(
                    text = stringResource(R.string.shop_owned),
                    color = MemoryArchitectColors.accentGold,
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                // Always tappable, even when unaffordable - purchase() below still runs and
                // surfaces a real "not enough coins" message via errorReason. A `canAfford`-gated
                // `enabled = false` button never fires onClick at all, which reads indistinguishably
                // from "the button is broken" rather than "you can't afford this yet."
                PrimaryButton(
                    text = if (isPurchasing) "…" else context.getString(R.string.shop_buy_for_coins, definition.priceCoins),
                    onClick = onBuy,
                    enabled = !isPurchasing,
                )
            }
        }
    }
}
