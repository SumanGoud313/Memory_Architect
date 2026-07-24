package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.PremiumProduct
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** One card per [PremiumProduct] in the Premium Shop tab - a visually distinct sibling of
 * [ShopItemRow], never sharing layout with it, so a coin item and a real-money bundle can never be
 * mistaken for each other at a glance (the redesign's "never mixing Coin/Premium products"
 * requirement). Deliberately gold-washed and larger than a coin row: this is where the "I don't
 * need this... but I absolutely want it" feeling has to live. Tapping the card (outside the Buy
 * button) opens [PremiumProductDetailDialog] for the full live preview. */
@Composable
fun PremiumProductCard(
    product: PremiumProduct,
    formattedPrice: String?,
    isOwned: Boolean,
    isLoading: Boolean,
    onBuy: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpenDetail),
        tint = MemoryArchitectColors.accentGold.copy(alpha = if (product.isBestValue) 0.16f else 0.08f),
    ) {
        Column(modifier = Modifier.padding(18.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(product.titleRes), style = MaterialTheme.typography.titleLarge, color = MemoryArchitectColors.textPrimary)
                if (product.isBestValue) {
                    PillBadge(text = stringResource(R.string.shop_best_value), contentColor = MemoryArchitectColors.accentGold)
                }
            }
            Text(
                text = stringResource(product.taglineRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (product.grantedCosmeticIds.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.grantedCosmeticIds.take(4).forEach { id ->
                        val definition = AllCosmeticsCatalog.requireDefinition(id)
                        CosmeticGlyph(id = id, category = definition.category, isOwned = true, sizeDp = 40.dp)
                    }
                }
                Text(
                    text = "${stringResource(R.string.shop_premium_includes)} · ${product.grantedCosmeticIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formattedPrice ?: "…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MemoryArchitectColors.accentGold,
                )
                if (isOwned) {
                    Text(text = stringResource(R.string.shop_owned_product), color = MemoryArchitectColors.accentGold, style = MaterialTheme.typography.labelLarge)
                } else {
                    PrimaryButton(
                        text = if (isLoading) "…" else stringResource(R.string.shop_premium_buy, formattedPrice ?: "…"),
                        onClick = onBuy,
                        enabled = !isLoading && formattedPrice != null,
                    )
                }
            }
        }
    }
}
