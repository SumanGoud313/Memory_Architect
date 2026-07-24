package com.suman.memoryarchitect.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.domain.model.LeaderboardType
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.GetDailyLeaderboardUseCase
import com.suman.memoryarchitect.domain.usecase.GetGlobalLeaderboardUseCase
import com.suman.memoryarchitect.domain.usecase.GetMonthlyLeaderboardUseCase
import com.suman.memoryarchitect.domain.usecase.GetWeeklyLeaderboardUseCase
import com.suman.memoryarchitect.domain.usecase.RecordLeaderboardRankUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val getGlobalLeaderboard: GetGlobalLeaderboardUseCase,
    private val getDailyLeaderboard: GetDailyLeaderboardUseCase,
    private val getWeeklyLeaderboard: GetWeeklyLeaderboardUseCase,
    private val getMonthlyLeaderboard: GetMonthlyLeaderboardUseCase,
    private val recordLeaderboardRank: RecordLeaderboardRankUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    // Remembered across tab switches purely so a Daily-only or Weekly-only fetch doesn't have to
    // guess the other board's rank when reporting both to recordLeaderboardRank - passing null for
    // whichever wasn't just fetched is always safe (see ProgressionRepositoryImpl.betterRank),
    // this just avoids needlessly re-fetching a tab the player already visited this session.
    private var lastDailyRank: Int? = null
    private var lastWeeklyRank: Int? = null

    init {
        load(LeaderboardType.GLOBAL)
    }

    fun selectTab(tab: LeaderboardType) {
        if ((_uiState.value as? LeaderboardUiState.Content)?.selectedTab == tab) return
        load(tab)
    }

    fun retry() {
        val tab = when (val state = _uiState.value) {
            is LeaderboardUiState.Content -> state.selectedTab
            is LeaderboardUiState.Error -> state.selectedTab
            LeaderboardUiState.Loading -> LeaderboardType.GLOBAL
        }
        load(tab)
    }

    private fun load(tab: LeaderboardType) {
        _uiState.value = LeaderboardUiState.Loading
        viewModelScope.launch {
            val outcome = when (tab) {
                LeaderboardType.GLOBAL -> getGlobalLeaderboard()
                LeaderboardType.DAILY -> getDailyLeaderboard()
                LeaderboardType.WEEKLY -> getWeeklyLeaderboard()
                LeaderboardType.MONTHLY -> getMonthlyLeaderboard()
            }
            when (outcome) {
                is Outcome.Success -> {
                    _uiState.value = LeaderboardUiState.Content(tab, outcome.data)
                    onLeaderboardLoaded(tab, outcome.data.currentPlayerRank)
                }
                is Outcome.Error -> _uiState.value = LeaderboardUiState.Error(tab, outcome.error)
            }
        }
    }

    /** Opportunistically caches a fresh Daily/Weekly rank as this player's new personal-best rank
     * (if it is one) and evaluates the two rank-gated achievements - see
     * [com.suman.memoryarchitect.domain.repository.ProgressionRepository.recordLeaderboardRank].
     * A no-op for [LeaderboardType.GLOBAL]/[LeaderboardType.MONTHLY] (there's no "best rank"
     * achievement gated on either - the Global board always shows current standing rather than a
     * personal best, and no Monthly-rank achievement exists in [com.suman.memoryarchitect.domain.achievements.AchievementCatalog]
     * to feed). */
    private fun onLeaderboardLoaded(tab: LeaderboardType, rank: Int?) {
        when (tab) {
            LeaderboardType.DAILY -> lastDailyRank = rank
            LeaderboardType.WEEKLY -> lastWeeklyRank = rank
            LeaderboardType.GLOBAL, LeaderboardType.MONTHLY -> return
        }
        viewModelScope.launch {
            val todayEpochDay = LocalDate.now(clock.withZone(ZoneOffset.UTC)).toEpochDay()
            recordLeaderboardRank(lastDailyRank, lastWeeklyRank, todayEpochDay)
        }
    }
}
