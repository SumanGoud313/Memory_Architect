package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.model.BillingCatalogProduct
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import com.suman.memoryarchitect.ui.components.BenefitRow
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The Premium Shop's sibling of [CosmeticPreviewDialog] - a multi-item live preview + Buy for one
 * [BillingCatalogProduct] bundle, never a modification of [CosmeticPreviewDialog] itself (that dialog stays
 * single-item, Coin-tab only). Reuses [PreviewVisual] (promoted to `internal` for exactly this) so
 * every granted cosmetic gets the same animated per-category preview a Coin Shop item already gets -
 * a border shimmers, a timer sweeps, a victory/confetti effect loops, a background theme washes its
 * gradient - browsable in one horizontally-scrolling row so an 8-item collection never overflows the
 * dialog's fixed width. */
@Composable
fun PremiumProductDetailDialog(
    product: BillingCatalogProduct,
    formattedPrice: String?,
    priceLoadFailed: Boolean,
    isOwned: Boolean,
    isLoading: Boolean,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
    onRetryPriceLoad: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(product.titleRes)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(product.taglineRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                if (product.grantedCosmeticIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        product.grantedCosmeticIds.forEach { id ->
                            val definition = remember(id) { AllCosmeticsCatalog.requireDefinition(id) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                                PreviewVisual(definition = definition)
                                Text(
                                    text = definition.id.toDisplayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MemoryArchitectColors.textTertiary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        product.grantedCosmeticIds.forEach { id ->
                            BenefitRow(text = AllCosmeticsCatalog.requireDefinition(id).id.toDisplayName())
                        }
                    }
                }

                when {
                    isOwned -> Text(
                        text = stringResource(R.string.shop_owned_product),
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    formattedPrice != null -> Text(
                        text = formattedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    priceLoadFailed -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
                        Text(
                            text = stringResource(R.string.shop_premium_price_load_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MemoryArchitectColors.textTertiary,
                        )
                        TextButton(onClick = onRetryPriceLoad) {
                            Text(text = stringResource(R.string.action_retry), color = MemoryArchitectColors.accentGold)
                        }
                    }
                    else -> Text(
                        text = stringResource(R.string.shop_premium_price_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MemoryArchitectColors.textTertiary,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!isOwned && formattedPrice != null) {
                TextButton(onClick = { onBuy(); onDismiss() }, enabled = !isLoading) {
                    Text(
                        text = if (isLoading) "…" else stringResource(R.string.shop_premium_buy, formattedPrice),
                        color = MemoryArchitectColors.accentGold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
