package com.suman.memoryarchitect.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayMessage
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.core.common.toPlayTimeLabel
import com.suman.memoryarchitect.core.common.toSecondsLabel
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.progression.RankStanding
import com.suman.memoryarchitect.feature.statistics.StatisticsUiState
import com.suman.memoryarchitect.feature.statistics.StatisticsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.LevelProgressBar
import com.suman.memoryarchitect.ui.components.RankHero
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.StatTile
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Player-facing statistics dashboard - General/Performance/Activity/Challenges/Collection, plus
 * a rank hero with progress toward the next tier. Read-only (no actions beyond back navigation);
 * see [StatisticsViewModel] for how each number is sourced. */
@Composable
fun StatisticsScreen(onBack: () -> Unit, viewModel: StatisticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.statistics_title),
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            when (val state = uiState) {
                is StatisticsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentTerracotta)
                }
                is StatisticsUiState.Content -> StatisticsContent(state)
            }
        }
    }
}

@Composable
private fun StatisticsContent(state: StatisticsUiState.Content) {
    val context = LocalContext.current
    Column(
        // navigationBarsPadding() before the existing flat padding - this content has no banner ad
        // of its own reserving bottom clearance the way most other screens do, so without it the
        // last stat card could scroll to a resting position right against a 2/3-button nav bar.
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RankHero(rank = state.rankStanding.current, modifier = Modifier.padding(top = 8.dp).staggeredReveal(0))
        RankProgress(standing = state.rankStanding, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).staggeredReveal(0))

        if (state.insight != null) {
            InsightBanner(
                text = state.insight.toDisplayMessage(context),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).staggeredReveal(1),
            )
        }

        StatisticsSection(
            title = stringResource(R.string.statistics_section_general),
            index = 2,
            items = listOf(
                StatItem(Icons.Filled.Timeline, stringResource(R.string.statistics_current_level), "${state.currentLevel}"),
                StatItem(Icons.Filled.Villa, stringResource(R.string.statistics_highest_level), "${state.highestCampaignLevel}"),
                StatItem(Icons.Filled.AutoAwesome, stringResource(R.string.statistics_total_xp), "${state.totalXp}"),
                StatItem(Icons.Filled.MonetizationOn, stringResource(R.string.statistics_total_score), "${state.totalScore}"),
                StatItem(Icons.Filled.Star, stringResource(R.string.statistics_total_stars), "${state.totalStars}"),
                StatItem(
                    Icons.Filled.Public,
                    stringResource(R.string.statistics_global_rank),
                    state.globalRank?.let { "#$it" } ?: stringResource(R.string.statistics_not_ranked),
                ),
            ),
        )

        StatisticsSection(
            title = stringResource(R.string.statistics_section_performance),
            index = 3,
            items = performanceItems(state.statistics),
        )

        StatisticsSection(
            title = stringResource(R.string.statistics_section_activity),
            index = 4,
            items = listOf(
                StatItem(Icons.Filled.Schedule, stringResource(R.string.statistics_total_play_time), state.totalPlayTimeMs.toPlayTimeLabel()),
                StatItem(Icons.AutoMirrored.Filled.Login, stringResource(R.string.statistics_total_sessions), "${state.totalSessions}"),
                StatItem(Icons.Filled.CalendarMonth, stringResource(R.string.statistics_days_played), "${state.statistics.daysPlayed}"),
                StatItem(Icons.Filled.LocalFireDepartment, stringResource(R.string.statistics_current_login_streak), "${state.loginStreak}"),
                StatItem(Icons.AutoMirrored.Filled.TrendingUp, stringResource(R.string.statistics_longest_login_streak), "${state.longestLoginStreak}"),
            ),
        )

        StatisticsSection(
            title = stringResource(R.string.statistics_section_challenges),
            index = 5,
            items = listOf(
                StatItem(Icons.Filled.CalendarMonth, stringResource(R.string.statistics_daily_challenges_completed), "${state.statistics.dailyChallengesWon}"),
                StatItem(Icons.Filled.EventRepeat, stringResource(R.string.statistics_weekly_challenges_completed), "${state.statistics.weeklyChallengesWon}"),
                StatItem(
                    Icons.Filled.Leaderboard,
                    stringResource(R.string.statistics_daily_best_rank),
                    state.statistics.dailyBestRank?.let { "#$it" } ?: stringResource(R.string.statistics_not_ranked),
                ),
                StatItem(
                    Icons.Filled.CalendarViewWeek,
                    stringResource(R.string.statistics_weekly_best_rank),
                    state.statistics.weeklyBestRank?.let { "#$it" } ?: stringResource(R.string.statistics_not_ranked),
                ),
            ),
        )

        StatisticsSection(
            title = stringResource(R.string.statistics_section_collection),
            index = 6,
            items = listOf(
                StatItem(Icons.Filled.Psychology, stringResource(R.string.statistics_objects_memorized), "${state.statistics.objectsMemorized}"),
                StatItem(Icons.Filled.Villa, stringResource(R.string.statistics_rooms_completed), "${state.statistics.gamesPlayed}"),
                StatItem(Icons.Filled.Explore, stringResource(R.string.statistics_themes_unlocked), "${state.unlockedThemeCount}/${state.totalThemeCount}"),
                StatItem(Icons.Filled.EmojiEvents, stringResource(R.string.statistics_achievements_completed), "${state.unlockedAchievementCount}/${state.totalAchievementCount}"),
            ),
        )
    }
}

@Composable
private fun performanceItems(statistics: PlayerStatistics): List<StatItem> = listOf(
    StatItem(Icons.AutoMirrored.Filled.TrendingUp, stringResource(R.string.statistics_average_accuracy), "${(statistics.averageAccuracy * 100).toInt()}%"),
    StatItem(Icons.Filled.Grade, stringResource(R.string.statistics_best_accuracy), "${(statistics.bestAccuracy * 100).toInt()}%"),
    StatItem(Icons.Filled.Schedule, stringResource(R.string.statistics_average_completion_time), statistics.averageCompletionTimeMs.toSecondsLabel()),
    StatItem(Icons.Filled.Bolt, stringResource(R.string.statistics_fastest_completion), statistics.fastestCompletionMs?.toSecondsLabel() ?: "—"),
    StatItem(Icons.Filled.WorkspacePremium, stringResource(R.string.statistics_perfect_games), "${statistics.perfectGames}"),
    StatItem(Icons.Filled.AutoAwesome, stringResource(R.string.statistics_highest_combo), "${statistics.highestCombo}"),
)

private data class StatItem(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun StatisticsSection(title: String, index: Int, items: List<StatItem>) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MemoryArchitectColors.textPrimary,
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp).staggeredReveal(index),
    )
    Column(modifier = Modifier.fillMaxWidth().staggeredReveal(index), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { item ->
                    StatTile(icon = item.icon, label = item.label, value = item.value, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RankProgress(standing: RankStanding, modifier: Modifier = Modifier) {
    val next = standing.next ?: return
    val context = LocalContext.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LevelProgressBar(fraction = standing.progressToNext, modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.statistics_progress_to_rank, (standing.progressToNext * 100).toInt(), next.toDisplayName(context)),
            style = MaterialTheme.typography.labelMedium,
            color = MemoryArchitectColors.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun InsightBanner(text: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, tint = MemoryArchitectColors.accentGold.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MemoryArchitectColors.accentGold)
            Text(
                text = text,
                color = MemoryArchitectColors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
