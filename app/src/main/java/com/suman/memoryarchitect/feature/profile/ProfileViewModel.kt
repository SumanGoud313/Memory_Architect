package com.suman.memoryarchitect.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logDailyRewardClaimed
import com.suman.memoryarchitect.core.analytics.logInventoryItemGranted
import com.suman.memoryarchitect.core.analytics.setHighestCampaignLevelProperty
import com.suman.memoryarchitect.core.analytics.setLifetimeCoinsProperty
import com.suman.memoryarchitect.core.analytics.setLifetimeLevelsCompletedProperty
import com.suman.memoryarchitect.core.analytics.setLifetimeStarsProperty
import com.suman.memoryarchitect.core.analytics.setLifetimeXpProperty
import com.suman.memoryarchitect.core.feedback.FeedbackManager
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.progression.DailyRewardCatalog
import com.suman.memoryarchitect.domain.progression.MemoryRankCatalog
import com.suman.memoryarchitect.domain.progression.RewardCatalog
import com.suman.memoryarchitect.domain.progression.XpCurve
import com.suman.memoryarchitect.domain.usecase.ClaimDailyRewardUseCase
import com.suman.memoryarchitect.domain.usecase.GetDailyRewardStatusUseCase
import com.suman.memoryarchitect.domain.usecase.GetEquippedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetLevelCampaignProgressUseCase
import com.suman.memoryarchitect.domain.usecase.GetOwnedCosmeticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import com.suman.memoryarchitect.domain.usecase.GetStatisticsUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedAchievementsUseCase
import com.suman.memoryarchitect.domain.usecase.GetUnlockedRewardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfile: GetPlayerProfileUseCase,
    private val getStatistics: GetStatisticsUseCase,
    private val getUnlockedAchievements: GetUnlockedAchievementsUseCase,
    private val getUnlockedRewards: GetUnlockedRewardsUseCase,
    private val getDailyRewardStatus: GetDailyRewardStatusUseCase,
    private val claimDailyRewardUseCase: ClaimDailyRewardUseCase,
    private val getCampaignProgress: GetLevelCampaignProgressUseCase,
    private val getOwnedCosmetics: GetOwnedCosmeticsUseCase,
    private val getEquippedCosmetics: GetEquippedCosmeticsUseCase,
    private val analytics: AnalyticsLogger,
    private val feedback: FeedbackManager,
) : ViewModel() {

    private val xpCurve = XpCurve()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** One-shot celebration events (unlock burst + coin/XP count-up) — a [SharedFlow] rather than
     * part of [uiState] so a claim never re-fires its animation on recomposition or rotation. */
    private val _claimEvents = MutableSharedFlow<DailyRewardClaimResult>(extraBufferCapacity = 1)
    val claimEvents: SharedFlow<DailyRewardClaimResult> = _claimEvents.asSharedFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            coroutineScope {
                // getProfile/getDailyRewardStatus each make their own network request - awaited
                // one after another (as this used to), a single unreachable server means paying
                // that request's timeout twice in a row before Profile shows anything at all, not
                // once. Launched together instead, the total wait is bounded by whichever one is
                // slowest, not their sum - the "best-effort, should never block the rest of
                // Profile" comment this replaces was already the intent, just not what the
                // sequential awaits actually did.
                val statisticsDeferred = async { getStatistics() }
                val unlockedIdsDeferred = async { getUnlockedAchievements() }
                val unlockedRewardIdsDeferred = async { getUnlockedRewards() }
                val dailyRewardStatusDeferred = async { getDailyRewardStatus().orNull() }
                val ownedCosmeticsDeferred = async { getOwnedCosmetics() }
                val equippedCosmeticsDeferred = async { getEquippedCosmetics() }
                val profileDeferred = async { getProfile() }

                val statistics = statisticsDeferred.await()
                val unlockedIds = unlockedIdsDeferred.await()
                val unlockedRewardIds = unlockedRewardIdsDeferred.await()
                val dailyRewardStatus = dailyRewardStatusDeferred.await()
                val ownedCosmeticIds = ownedCosmeticsDeferred.await()
                val equippedCosmetics = equippedCosmeticsDeferred.await()

                _uiState.value = when (val outcome = profileDeferred.await()) {
                    is Outcome.Success -> contentFor(outcome.data, statistics, unlockedIds, unlockedRewardIds, dailyRewardStatus, ownedCosmeticIds, equippedCosmetics)
                    is Outcome.Error -> ProfileUiState.Error(outcome.error)
                }
                syncUserProperties(statistics)
            }
        }
    }

    /** Refreshes the handful of Firebase user properties this app maintains - see
     * AnalyticsEvents.kt's setXxxProperty functions. Profile is a natural, low-frequency sync
     * point (whenever the player checks their own stats) rather than syncing on every gameplay
     * event, which would be excessive event/property-write volume for numbers that only meaningfully
     * change a few times per session at most. */
    private suspend fun syncUserProperties(statistics: PlayerStatistics) {
        val campaignProgress = getCampaignProgress()
        analytics.setHighestCampaignLevelProperty(campaignProgress.maxUnlockedLevel)
        analytics.setLifetimeStarsProperty(campaignProgress.bestStars.values.sum())
        analytics.setLifetimeLevelsCompletedProperty(statistics.gamesPlayed)
        val profile = (_uiState.value as? ProfileUiState.Content)?.profile
        if (profile != null) {
            analytics.setLifetimeXpProperty(profile.xp)
            analytics.setLifetimeCoinsProperty(profile.coins)
        }
    }

    /** No-op if a claim is already in flight or today's reward was already claimed — the UI only
     * ever shows an enabled claim affordance when [DailyRewardStatus.canClaimToday] is true, but
     * this guards the same invariant here so a double-tap can never double-claim. */
    fun claimDailyReward() {
        val current = _uiState.value as? ProfileUiState.Content ?: return
        if (current.isClaimingDailyReward || current.dailyRewardStatus?.canClaimToday != true) return
        _uiState.value = current.copy(isClaimingDailyReward = true, dailyRewardError = null)
        viewModelScope.launch {
            when (val outcome = claimDailyRewardUseCase()) {
                is Outcome.Success -> {
                    val result = outcome.data
                    val refreshedStatus = getDailyRewardStatus().orNull()
                    val latest = _uiState.value as? ProfileUiState.Content ?: return@launch
                    val (xpIntoLevel, xpForNextLevel) = xpCurve.xpProgressWithinLevel(result.profile.xp)
                    _uiState.value = latest.copy(
                        level = xpCurve.levelForXp(result.profile.xp),
                        xpIntoLevel = xpIntoLevel,
                        xpForNextLevel = xpForNextLevel,
                        profile = result.profile,
                        dailyRewardStatus = refreshedStatus ?: latest.dailyRewardStatus?.copy(cycleDay = result.cycleDay, canClaimToday = false),
                        isClaimingDailyReward = false,
                    )
                    _claimEvents.emit(result)
                    feedback.onDailyRewardClaimed()
                    val entry = DailyRewardCatalog.entryForDay(result.cycleDay)
                    analytics.logDailyRewardClaimed(result.cycleDay, entry.kind.name, result.coinsAwarded, result.xpAwarded, result.shieldAwarded)
                    entry.inventoryGrants.forEach { (kind, quantity) ->
                        analytics.logInventoryItemGranted(kind.name, quantity, source = "daily_reward")
                    }
                }
                is Outcome.Error -> {
                    _uiState.value = (_uiState.value as? ProfileUiState.Content)
                        ?.copy(isClaimingDailyReward = false, dailyRewardError = outcome.error)
                        ?: _uiState.value
                }
            }
        }
    }

    private fun contentFor(
        profile: PlayerProfile,
        statistics: PlayerStatistics,
        unlockedIds: Set<AchievementId>,
        unlockedRewardIds: Set<RewardId>,
        dailyRewardStatus: DailyRewardStatus?,
        ownedCosmeticIds: Set<CosmeticId>,
        equippedCosmetics: Map<CosmeticCategory, CosmeticId>,
    ): ProfileUiState.Content {
        val level = xpCurve.levelForXp(profile.xp)
        val (xpIntoLevel, xpForNextLevel) = xpCurve.xpProgressWithinLevel(profile.xp)
        return ProfileUiState.Content(
            level = level,
            xpIntoLevel = xpIntoLevel,
            xpForNextLevel = xpForNextLevel,
            profile = profile,
            statistics = statistics,
            unlockedAchievementIds = unlockedIds,
            unlockedRewardIds = unlockedRewardIds,
            equippedTitleId = RewardCatalog.equippedTitle(unlockedRewardIds),
            equippedPaletteId = RewardCatalog.equippedPalette(unlockedRewardIds),
            dailyRewardStatus = dailyRewardStatus,
            memoryRank = MemoryRankCatalog.rankFor(level),
            ownedCosmeticIds = ownedCosmeticIds,
            equippedCosmetics = equippedCosmetics,
        )
    }

    private fun <T> Outcome<T>.orNull(): T? = (this as? Outcome.Success)?.data
}
