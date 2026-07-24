package com.suman.memoryarchitect.ui.screens.nointernet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.connectivity.ui.NoInternetViewModel
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.warmGlowOverlay
import com.suman.memoryarchitect.ui.components.warmGradientBackground
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Compose replacement for NoInternetFragment — presentation only. [com.suman.memoryarchitect.ui.ConnectivityGate]
 * owns showing/hiding this as an overlay and intercepting the back button while it's visible.
 */
@Composable
fun NoInternetScreen(viewModel: NoInternetViewModel = hiltViewModel()) {
    Box(modifier = Modifier.warmGradientBackground().warmGlowOverlay().fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.padding(32.dp)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_internet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MemoryArchitectColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.no_internet_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.textSecondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                )
                PrimaryButton(text = stringResource(R.string.action_retry), onClick = viewModel::retry)
            }
        }
    }
}
