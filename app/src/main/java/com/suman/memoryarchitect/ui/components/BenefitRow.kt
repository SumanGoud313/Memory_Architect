package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** A feature bullet with a checkmark - promoted out of `RemoveAdsScreen.kt` (its original, private
 * home) so [com.suman.memoryarchitect.ui.screens.shop.PremiumProductDetailDialog] can list a
 * Premium Shop bundle's included cosmetics the same way Remove Ads already lists its benefits,
 * without duplicating this composable. */
@Composable
fun BenefitRow(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MemoryArchitectColors.accentGold,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
