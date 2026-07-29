package com.suman.memoryarchitect.ui.screens.removeads

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.billing.PurchaseUiState
import com.suman.memoryarchitect.core.billing.toDisplayMessage
import com.suman.memoryarchitect.feature.removeads.RemoveAdsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.BenefitRow
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** The "Buy Now" / "Restore Purchase" screen for the lifetime `remove_ads_lifetime` one-time
 * purchase - see [com.suman.memoryarchitect.core.billing.BillingManager] for the actual Play
 * Billing logic behind everything shown here. Reached from Settings. */
@Composable
fun RemoveAdsScreen(onBack: () -> Unit, viewModel: RemoveAdsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val hasRemovedAds by viewModel.hasRemovedAds.collectAsStateWithLifecycle()
    val formattedPrice by viewModel.formattedPrice.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val priceLoadFailed by viewModel.priceLoadFailed.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.remove_ads_title),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (hasRemovedAds) {
                    PurchasedCard(modifier = Modifier.staggeredReveal(1))
                } else {
                    OfferCard(
                        formattedPrice = formattedPrice,
                        priceLoadFailed = priceLoadFailed,
                        purchaseState = purchaseState,
                        onBuyNow = { (context as? android.app.Activity)?.let(viewModel::buyNow) },
                        onRestore = viewModel::restorePurchases,
                        onRetryPriceLoad = viewModel::retryPriceLoad,
                        modifier = Modifier.staggeredReveal(1),
                    )
                    StatusMessage(purchaseState = purchaseState, modifier = Modifier.staggeredReveal(2))
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    formattedPrice: String?,
    priceLoadFailed: Boolean,
    purchaseState: PurchaseUiState,
    onBuyNow: () -> Unit,
    onRestore: () -> Unit,
    onRetryPriceLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = purchaseState is PurchaseUiState.Loading
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(
                text = stringResource(R.string.remove_ads_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = MemoryArchitectColors.textPrimary,
            )
            Text(
                text = stringResource(R.string.remove_ads_subheadline),
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )

            Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BenefitRow(stringResource(R.string.remove_ads_benefit_no_banner))
                BenefitRow(stringResource(R.string.remove_ads_benefit_no_interstitial))
                BenefitRow(stringResource(R.string.remove_ads_benefit_no_app_open))
                BenefitRow(stringResource(R.string.remove_ads_benefit_keep_rewarded))
            }

            if (formattedPrice != null) {
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MemoryArchitectColors.accentGold,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else if (priceLoadFailed) {
                Row(modifier = Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.remove_ads_price_load_failed),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MemoryArchitectColors.textTertiary,
                    )
                    OutlineButton(text = stringResource(R.string.action_retry), onClick = onRetryPriceLoad)
                }
            } else {
                Text(
                    text = stringResource(R.string.remove_ads_price_loading),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MemoryArchitectColors.accentGold,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            PrimaryButton(
                text = stringResource(R.string.remove_ads_buy_now),
                onClick = onBuyNow,
                enabled = !isLoading && formattedPrice != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            OutlineButton(
                text = stringResource(R.string.remove_ads_restore_purchase),
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            if (isLoading) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentGold, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PurchasedCard(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), tint = MemoryArchitectColors.accentGold.copy(alpha = 0.14f)) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(56.dp).background(MemoryArchitectColors.accentGold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = MemoryArchitectColors.bgBase, modifier = Modifier.size(32.dp))
            }
            Text(
                text = stringResource(R.string.remove_ads_purchased_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.remove_ads_purchased_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusMessage(purchaseState: PurchaseUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    when (purchaseState) {
        is PurchaseUiState.Pending -> InlineStatusCard(
            icon = Icons.Filled.HourglassTop,
            title = stringResource(R.string.remove_ads_pending_title),
            subtitle = stringResource(R.string.remove_ads_pending_subtitle),
            modifier = modifier,
        )
        is PurchaseUiState.Failed -> InlineStatusCard(
            icon = Icons.Filled.ErrorOutline,
            title = purchaseState.reason.toDisplayMessage(context),
            subtitle = null,
            tint = MemoryArchitectColors.danger,
            modifier = modifier,
        )
        PurchaseUiState.RestoreNotFound -> InlineStatusCard(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.remove_ads_restore_none_found),
            subtitle = null,
            modifier = modifier,
        )
        else -> Unit
    }
}

@Composable
private fun InlineStatusCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    tint: Color = MemoryArchitectColors.accentGold,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MemoryArchitectColors.textPrimary)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MemoryArchitectColors.textSecondary)
                }
            }
        }
    }
}
