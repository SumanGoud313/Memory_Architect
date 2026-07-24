package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import com.suman.memoryarchitect.feature.shop.CollectionsTab
import com.suman.memoryarchitect.feature.shop.CollectionsUiState
import com.suman.memoryarchitect.feature.shop.CollectionsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/** Cosmetic gallery + loadout manager - reached via Profile's "Collections" button. View/equip
 * only, never a storefront (buying happens in [ShopScreen]) - kept cleanly separate.
 *
 * Only *owned* cosmetics render here - the cosmetic audit's core Collections finding was that this
 * screen used to also show locked items (dimmed, priced), which made it read as a second, weaker
 * Shop instead of a trophy case of things the player actually has. [onOpenShop] backs the empty
 * state's "Browse Shop" call to action for a player who owns nothing yet.
 *
 * Three tabs - All / Favorites / Recently Used - all reading the same owned/equipped state, never
 * a second data source: Favorites and Recent both filter the exact same catalog All already
 * renders (see [buildCollectionsSections] and the Recent tab's own flat-list branch below), so a
 * newly purchased Coin *or* Premium cosmetic (see the Shop redesign) shows up in all three the
 * instant it's owned, with zero extra wiring. */
@Composable
fun CollectionsScreen(onBack: () -> Unit, onOpenShop: () -> Unit = {}, viewModel: CollectionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()
    var previewDefinition by remember { mutableStateOf<CosmeticDefinition?>(null) }
    val showcaseHeader = stringResource(R.string.profile_showcase_header)

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.collections_title),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            when (val state = uiState) {
                is CollectionsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
                is CollectionsUiState.Content -> {
                    val ownedCount = state.ownedIds.size
                    val totalCount = AllCosmeticsCatalog.definitions.size
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(modifier = Modifier.staggeredReveal(0)) {
                            Text(
                                text = stringResource(R.string.collections_completion, ownedCount, totalCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MemoryArchitectColors.textSecondary,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MemoryArchitectColors.glassFill),
                            ) {
                                val fraction = if (totalCount > 0) ownedCount.toFloat() / totalCount.toFloat() else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.horizontalGradient(listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold))),
                                )
                            }
                        }

                        if (ownedCount == 0) {
                            CollectionsEmptyState(onOpenShop = onOpenShop, modifier = Modifier.staggeredReveal(1))
                        } else {
                            CollectionsTabRow(
                                selected = state.selectedTab,
                                onSelect = viewModel::selectTab,
                                modifier = Modifier.fillMaxWidth().staggeredReveal(1),
                            )

                            when (state.selectedTab) {
                                CollectionsTab.ALL -> CollectionsGrid(
                                    ownedFilterIds = state.ownedIds,
                                    state = state,
                                    showcaseHeader = showcaseHeader,
                                    onPreview = { previewDefinition = it },
                                    viewModel = viewModel,
                                )
                                CollectionsTab.FAVORITES -> if (state.favoriteIds.isEmpty()) {
                                    CollectionsInfoCard(
                                        title = stringResource(R.string.collections_empty_title),
                                        body = stringResource(R.string.collections_favorites_empty),
                                        modifier = Modifier.staggeredReveal(2),
                                    )
                                } else {
                                    CollectionsGrid(
                                        ownedFilterIds = state.favoriteIds.intersect(state.ownedIds),
                                        state = state,
                                        showcaseHeader = showcaseHeader,
                                        onPreview = { previewDefinition = it },
                                        viewModel = viewModel,
                                    )
                                }
                                CollectionsTab.RECENT -> if (state.recentlyUsedIds.isEmpty()) {
                                    CollectionsInfoCard(
                                        title = stringResource(R.string.collections_empty_title),
                                        body = stringResource(R.string.collections_recent_empty),
                                        modifier = Modifier.staggeredReveal(2),
                                    )
                                } else {
                                    // Recency order matters here (most-recently-equipped first, per
                                    // OwnedCosmeticDao.getRecentlyUsed) - a category grouping would
                                    // destroy that order, so this is the one flat, ungrouped list.
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.recentlyUsedIds.mapNotNull { AllCosmeticsCatalog.definitionOrNull(it) }.forEachIndexed { index, definition ->
                                            CollectionsItemRow(
                                                definition = definition,
                                                isEquipped = state.equipped[definition.category] == definition.id,
                                                isBusy = state.equippingId != null,
                                                isFavorited = definition.id in state.favoriteIds,
                                                onToggleFavorite = { viewModel.toggleFavorite(definition.id) },
                                                onPreview = { previewDefinition = definition },
                                                onEquip = { viewModel.equip(definition.category, definition.id) },
                                                onUnequip = { viewModel.unequip(definition.category) },
                                                modifier = Modifier.staggeredReveal(index + 2),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val preview = previewDefinition
        val contentState = uiState as? CollectionsUiState.Content
        if (preview != null && contentState != null) {
            val isOwned = preview.id in contentState.ownedIds
            val isEquipped = contentState.equipped[preview.category] == preview.id
            CosmeticPreviewDialog(
                definition = preview,
                isOwned = isOwned,
                isEquipped = isEquipped,
                onEquip = { viewModel.equip(preview.category, preview.id) },
                onUnequip = { viewModel.unequip(preview.category) },
                onDismiss = { previewDefinition = null },
            )
        }
    }
}

private class CollectionsSection(val header: String, val items: List<CosmeticDefinition>)

/** The category-grouped gallery body shared by the All and Favorites tabs - [ownedFilterIds] is
 * the one thing that differs between them ([CollectionsUiState.Content.ownedIds] for All,
 * `favoriteIds ∩ ownedIds` for Favorites), so both tabs render off the exact same catalog and
 * equipped-first sort with zero duplicated layout code. */
@Composable
private fun CollectionsGrid(
    ownedFilterIds: Set<CosmeticId>,
    state: CollectionsUiState.Content,
    showcaseHeader: String,
    onPreview: (CosmeticDefinition) -> Unit,
    viewModel: CollectionsViewModel,
) {
    val byCategory = AllCosmeticsCatalog.definitions.groupBy { it.category }
    val sections = buildCollectionsSections(byCategory, ownedFilterIds, state.equipped, showcaseHeader)
    sections.forEachIndexed { sectionIndex, section ->
        Column(modifier = Modifier.staggeredReveal(sectionIndex + 2), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${section.header} (${section.items.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MemoryArchitectColors.textPrimary,
            )
            section.items.forEach { definition ->
                CollectionsItemRow(
                    definition = definition,
                    isEquipped = state.equipped[definition.category] == definition.id,
                    isBusy = state.equippingId != null,
                    isFavorited = definition.id in state.favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavorite(definition.id) },
                    onPreview = { onPreview(definition) },
                    onEquip = { viewModel.equip(definition.category, definition.id) },
                    onUnequip = { viewModel.unequip(definition.category) },
                )
            }
        }
    }
}

/** Mirrors [ShopScreen]'s section-building (same [CosmeticCategory.STICKER_PACK] +
 * [CosmeticCategory.TROPHY_RELIC] + [CosmeticCategory.PROFILE_BADGE] "Showcase" merge,
 * presentation-only), plus filters every category down to [ownedIds] only and sorts the currently
 * equipped item to the front of its section so "what do I have on right now" reads at a glance.
 * [ownedIds] is a filter, not literally "owned" - see [CollectionsGrid]'s doc for why Favorites
 * passes a narrower set through this same parameter. */
private fun buildCollectionsSections(
    byCategory: Map<CosmeticCategory, List<CosmeticDefinition>>,
    ownedIds: Set<CosmeticId>,
    equipped: Map<CosmeticCategory, CosmeticId>,
    showcaseHeader: String,
): List<CollectionsSection> {
    fun ownedSorted(items: List<CosmeticDefinition>): List<CosmeticDefinition> {
        val owned = items.filter { it.id in ownedIds }
        return owned.sortedByDescending { equipped[it.category] == it.id }
    }
    val showcaseCategories = setOf(CosmeticCategory.STICKER_PACK, CosmeticCategory.TROPHY_RELIC, CosmeticCategory.PROFILE_BADGE)
    return buildList {
        CosmeticCategory.entries.forEach { category ->
            if (category in showcaseCategories && category != CosmeticCategory.STICKER_PACK) return@forEach
            if (category == CosmeticCategory.STICKER_PACK) {
                val combined = ownedSorted(showcaseCategories.flatMap { byCategory[it].orEmpty() })
                if (combined.isNotEmpty()) add(CollectionsSection(showcaseHeader, combined))
            } else {
                val items = ownedSorted(byCategory[category].orEmpty())
                if (items.isNotEmpty()) add(CollectionsSection(category.toDisplayName(), items))
            }
        }
    }
}

/** One owned-cosmetic row - shared by the All/Favorites category-grouped grid and the Recent tab's
 * flat list, so equip/unequip/preview/favorite behave identically everywhere a cosmetic appears. */
@Composable
private fun CollectionsItemRow(
    definition: CosmeticDefinition,
    isEquipped: Boolean,
    isBusy: Boolean,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onPreview: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canEquip = !isEquipped && !isBusy
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    canEquip -> Modifier.clickable { onEquip() }
                    isEquipped && !isBusy -> Modifier.clickable { onUnequip() }
                    else -> Modifier
                },
            ),
        tint = if (isEquipped) MemoryArchitectColors.accentGold.copy(alpha = 0.14f) else null,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // A nested clickable - tapping the glyph or the eye icon opens Preview instead of
            // triggering the row's own equip/unequip, unchanged from before.
            Box {
                CosmeticGlyph(
                    id = definition.id,
                    category = definition.category,
                    isOwned = true,
                    modifier = Modifier.clickable { onPreview() },
                )
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = stringResource(R.string.cosmetic_preview_action),
                    tint = MemoryArchitectColors.textPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(MemoryArchitectColors.glassFill, CircleShape)
                        .clickable { onPreview() }
                        .padding(3.dp),
                )
            }
            Text(
                text = definition.id.toDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(start = 14.dp).weight(1f),
            )
            Icon(
                imageVector = if (isFavorited) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(if (isFavorited) R.string.collections_unfavorite else R.string.collections_favorite),
                tint = if (isFavorited) MemoryArchitectColors.accentGold else MemoryArchitectColors.textTertiary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(20.dp)
                    .clickable { onToggleFavorite() },
            )
            Text(
                text = stringResource(if (isEquipped) R.string.collections_equipped else R.string.collections_equip),
                color = if (isEquipped) MemoryArchitectColors.accentGold else MemoryArchitectColors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** All / Favorites / Recently Used - same lightweight chip-row shape [ShopScreen]'s own Coin/
 * Premium tab row uses (see that file's doc for why this app builds a small per-screen chip row
 * rather than a shared `TabRow` primitive). */
@Composable
private fun CollectionsTabRow(selected: CollectionsTab, onSelect: (CollectionsTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CollectionsTabChip(
            text = stringResource(R.string.collections_tab_all),
            isSelected = selected == CollectionsTab.ALL,
            onClick = { onSelect(CollectionsTab.ALL) },
            modifier = Modifier.weight(1f),
        )
        CollectionsTabChip(
            text = stringResource(R.string.collections_tab_favorites),
            isSelected = selected == CollectionsTab.FAVORITES,
            onClick = { onSelect(CollectionsTab.FAVORITES) },
            modifier = Modifier.weight(1f),
        )
        CollectionsTabChip(
            text = stringResource(R.string.collections_tab_recent),
            isSelected = selected == CollectionsTab.RECENT,
            onClick = { onSelect(CollectionsTab.RECENT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CollectionsTabChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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

@Composable
private fun CollectionsEmptyState(onOpenShop: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.collections_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MemoryArchitectColors.textPrimary,
            )
            Text(
                text = stringResource(R.string.collections_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            PrimaryButton(
                text = stringResource(R.string.collections_empty_cta),
                onClick = onOpenShop,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** The Favorites/Recent tabs' empty state - unlike [CollectionsEmptyState] (no cosmetics owned at
 * all, "go buy something"), this is "you own things, just haven't favorited/equipped one yet" - a
 * different message, no Shop call to action. */
@Composable
private fun CollectionsInfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MemoryArchitectColors.textPrimary)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
