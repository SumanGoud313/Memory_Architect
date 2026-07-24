package com.suman.memoryarchitect.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.core.common.toSecondsLabel
import com.suman.memoryarchitect.domain.model.AvatarCatalog
import com.suman.memoryarchitect.domain.model.LeaderboardEntry
import com.suman.memoryarchitect.domain.model.LeaderboardType
import com.suman.memoryarchitect.feature.leaderboard.LeaderboardUiState
import com.suman.memoryarchitect.feature.leaderboard.LeaderboardViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

@Composable
fun LeaderboardScreen(onBack: () -> Unit, viewModel: LeaderboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()
    val context = LocalContext.current

    val selectedTab = when (val state = uiState) {
        is LeaderboardUiState.Content -> state.selectedTab
        is LeaderboardUiState.Error -> state.selectedTab
        LeaderboardUiState.Loading -> null
    }

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            ScreenHeader(
                title = stringResource(R.string.leaderboard_title),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().staggeredReveal(0),
            )

            LeaderboardTabRow(
                selected = selectedTab,
                onSelect = viewModel::selectTab,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp).staggeredReveal(1),
            )

            when (val state = uiState) {
                LeaderboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
                is LeaderboardUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error.toDisplayMessage(context),
                            color = MemoryArchitectColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlineButton(
                            text = stringResource(R.string.action_retry),
                            onClick = viewModel::retry,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
                is LeaderboardUiState.Content -> LeaderboardList(state.result.entries, state.result.currentPlayerRank)
            }
        }
    }
}

@Composable
private fun LeaderboardTabRow(selected: LeaderboardType?, onSelect: (LeaderboardType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LeaderboardTabChip(
            text = stringResource(R.string.leaderboard_tab_global),
            isSelected = selected == LeaderboardType.GLOBAL,
            onClick = { onSelect(LeaderboardType.GLOBAL) },
            modifier = Modifier.weight(1f),
        )
        LeaderboardTabChip(
            text = stringResource(R.string.leaderboard_tab_daily),
            isSelected = selected == LeaderboardType.DAILY,
            onClick = { onSelect(LeaderboardType.DAILY) },
            modifier = Modifier.weight(1f),
        )
        LeaderboardTabChip(
            text = stringResource(R.string.leaderboard_tab_weekly),
            isSelected = selected == LeaderboardType.WEEKLY,
            onClick = { onSelect(LeaderboardType.WEEKLY) },
            modifier = Modifier.weight(1f),
        )
        LeaderboardTabChip(
            text = stringResource(R.string.leaderboard_tab_monthly),
            isSelected = selected == LeaderboardType.MONTHLY,
            onClick = { onSelect(LeaderboardType.MONTHLY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LeaderboardTabChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
private fun LeaderboardList(entries: List<LeaderboardEntry>, currentPlayerRank: Int?) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.leaderboard_empty),
                color = MemoryArchitectColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // The player's own rank when they placed outside this page - entries never contains
        // their row in that case (isCurrentPlayer is always false here), so this is the only
        // place they'd otherwise see themselves reflected at all.
        val isVisibleInPage = entries.any { it.isCurrentPlayer }
        if (currentPlayerRank != null && !isVisibleInPage) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                tint = MemoryArchitectColors.accentGold.copy(alpha = 0.14f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.leaderboard_your_rank),
                        color = MemoryArchitectColors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "#$currentPlayerRank",
                        color = MemoryArchitectColors.accentGold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(entries, key = { it.uid }) { entry ->
                LeaderboardRow(entry)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        tint = if (entry.isCurrentPlayer) MemoryArchitectColors.accentGold.copy(alpha = 0.14f) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankMedal(entry.rank)
            AvatarGlyph(entry.avatarId, entry.avatarUrl, modifier = Modifier.padding(start = 10.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        color = MemoryArchitectColors.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (entry.verified) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.leaderboard_verified),
                            tint = MemoryArchitectColors.accentTerracotta,
                            modifier = Modifier.padding(start = 6.dp).size(16.dp),
                        )
                    }
                    if (entry.isCurrentPlayer) {
                        Text(
                            text = stringResource(R.string.leaderboard_you),
                            color = MemoryArchitectColors.accentGold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                val countryPrefix = entry.country?.let { "$it · " } ?: ""
                Text(
                    text = "$countryPrefix${entry.league.displayName} · ${(entry.accuracy * 100).toInt()}% · ${entry.completionTimeMs.toSecondsLabel()}",
                    color = MemoryArchitectColors.textTertiary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = "${entry.primaryValue}",
                color = MemoryArchitectColors.accentGold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun AvatarGlyph(avatarId: String, avatarUrl: String?, modifier: Modifier = Modifier) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(32.dp).clip(CircleShape),
        )
        return
    }
    Box(
        modifier = modifier.size(32.dp).background(MemoryArchitectColors.glassFill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = AvatarCatalog.glyphFor(avatarId), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RankMedal(rank: Int) {
    val medalColor = when (rank) {
        1 -> Color(0xFFF0C875)
        2 -> Color(0xFFC7CDD6)
        3 -> Color(0xFFCD7F32)
        else -> null
    }
    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        if (medalColor != null) {
            Box(modifier = Modifier.size(36.dp).background(medalColor.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = medalColor, modifier = Modifier.size(20.dp))
            }
        } else {
            Text(text = "#$rank", color = MemoryArchitectColors.textSecondary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
