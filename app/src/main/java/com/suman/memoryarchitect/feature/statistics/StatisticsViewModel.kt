package com.suman.memoryarchitect.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.achievements.AchievementCatalog
import com.suman.memoryarchitect.domain.model.LeaderboardResult
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.RewardKind
import com.suman.memoryarchitect.domain.motivation.MotivationEngine
import com.suman.memoryarchitect.domain.progression.PlayerRankEngine
import com.suman.memoryarchitect.domain.progression.RewardCatalog
import com.suman.memoryarchitect.domain.progression.XpCurve
import com.suman.memoryarchitect.domain.usecase.GetGlobalLeaderboardUseCase
import com.suman.memoryarchitect.domain.usecase.GetLevelCampaignProgressUseCase
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import com.suman.memoryarchitect.domain.usecase.GetStatisticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedAchievementsUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedRewardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getProfile: GetPlayerProfileUseCase,
    private val getStatistics: GetStatisticsUseCase,
    private val getUnlockedAchievements: GetUnlockedAchievementsUseCase,
    private val getUnlockedRewards: GetUnlockedRewardsUseCase,
    private val getCampaignProgress: GetLevelCampaignProgressUseCase,
    private val getGlobalLeaderboard: GetGlobalLeaderboardUseCase,
    private val preferences: UserPreferencesDataStore,
) : ViewModel() {

    private val xpCurve = XpCurve()

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = StatisticsUiState.Loading
        viewModelScope.launch {
            coroutineScope {
                // Global rank is genuinely optional (a leaderboard fetch failing must never block
                // the player's own, purely-local numbers from showing) - launched alongside
                // everything else, awaited last, its failure only costs the one field.
                val profileDeferred = async { getProfile() }
                val statisticsDeferred = async { getStatistics() }
                val unlockedAchievementsDeferred = async { getUnlockedAchievements() }
                val unlockedRewardsDeferred = async { getUnlockedRewards() }
                val campaignProgressDeferred = async { getCampaignProgress() }
                val globalLeaderboardDeferred = async { getGlobalLeaderboard() }
                val sessionCountDeferred = async { preferences.sessionCount.first() }
                val playTimeDeferred = async { preferences.lifetimePlayTimeMs.first() }

                val statistics = statisticsDeferred.await()
                val unlockedAchievements = unlockedAchievementsDeferred.await()
                val unlockedRewards = unlockedRewardsDeferred.await()
                val campaignProgress = campaignProgressDeferred.await()
                val globalRank = (globalLeaderboardDeferred.await() as? Outcome.Success<LeaderboardResult>)?.data?.currentPlayerRank
                val sessionCount = sessionCountDeferred.await()
                val lifetimePlayTimeMs = playTimeDeferred.await()

                val profile = when (val outcome = profileDeferred.await()) {
                    is Outcome.Success -> outcome.data
                    is Outcome.Error -> null
                } ?: return@coroutineScope

                val level = xpCurve.levelForXp(profile.xp)
                val unlockedThemeCount = RewardCatalog.timeline.count { it.kind == RewardKind.ROOM_THEME && it.id in unlockedRewards }
                val totalThemeCount = RewardCatalog.timeline.count { it.kind == RewardKind.ROOM_THEME }

                _uiState.value = StatisticsUiState.Content(
                    currentLevel = level,
                    highestCampaignLevel = campaignProgress.maxUnlockedLevel,
                    totalXp = profile.xp,
                    totalScore = statistics.totalScore,
                    totalStars = campaignProgress.bestStars.values.sum(),
                    globalRank = globalRank,
                    rankStanding = PlayerRankEngine.standingFor(level, statistics.averageAccuracy),
                    statistics = statistics,
                    loginStreak = profile.currentStreak,
                    longestLoginStreak = profile.longestStreak,
                    totalSessions = sessionCount,
                    totalPlayTimeMs = lifetimePlayTimeMs,
                    unlockedThemeCount = unlockedThemeCount,
                    totalThemeCount = totalThemeCount,
                    unlockedAchievementCount = unlockedAchievements.size,
                    totalAchievementCount = AchievementCatalog.definitions.size,
                    insight = MotivationEngine.levelsToNextRankInsight(level, statistics.averageAccuracy),
                )
            }
        }
    }
}
