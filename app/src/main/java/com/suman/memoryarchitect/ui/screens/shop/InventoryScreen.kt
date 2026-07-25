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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.InventoryItemKind
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
    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.inventory_header),
                    onBack = onBack,
                    modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
                )
                InventoryScreenBody()
            }
        }
    }
}

/** Pure content body, no header/background of its own - reusable if a future phase wants Inventory
 * embedded elsewhere (e.g. a future Shop tab redesign) without duplicating this list. */
@Composable
fun InventoryScreenBody(viewModel: InventoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
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
            )
        }
    }
}
